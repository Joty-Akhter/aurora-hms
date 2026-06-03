package com.easyops.hospital.controller;

import com.easyops.hospital.exception.WorkspaceConflictException;
import com.easyops.hospital.service.EpDoctorWorkspaceService;
import com.easyops.hospital.service.EpDoctorWorkspaceService.WorkspaceResult;
import com.easyops.hospital.service.RbacPermissionService;
import com.easyops.hospital.service.WorkspaceRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Server-side Easy Prescription workspace (JSON blob: templates, EP config, drafts map, recent Rx).
 * Gateway path: /api/hospital/easy-prescription/workspace
 *
 * <h3>Concurrency</h3>
 * Optimistic versioning: GET returns {@code X-EP-Workspace-Version}, PUT requires the client to
 * echo it back via {@code X-EP-Client-Version}.  A mismatch → 409 Conflict.
 *
 * <h3>Rate limiting (EP-8)</h3>
 * Max 1 write per 2 seconds per user, enforced by {@link WorkspaceRateLimiter}.
 * Excess writes → 429 Too Many Requests with {@code Retry-After: 2}.
 *
 * <h3>HIPAA audit (EP-10)</h3>
 * Every GET (action='READ') and every PUT (action='UPSERT') are recorded in
 * {@code ehr.ep_doctor_workspace_audit} with userId, organizationId, ip_address,
 * http_status, and timestamp.
 *
 * <p><strong>Both READ and WRITE audit entries must not store the workspace payload.</strong>
 * Only access/mutation metadata is permitted in the audit row:
 * {@code action, userId, organizationId, workspaceId, ip_address, http_status, created_at}.
 * The {@code snapshot_json} column that existed in the original schema was permanently
 * dropped (changeset 051) to enforce this at the storage layer; the
 * {@link com.easyops.hospital.entity.EpDoctorWorkspaceAudit} entity does not map a payload field.
 */
@RestController
@RequestMapping("/api/easy-prescription/workspace")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Easy Prescription Workspace", description = "Sync EP templates, config, and drafts per user/org")
public class EasyPrescriptionWorkspaceController {

    private final EpDoctorWorkspaceService epDoctorWorkspaceService;
    private final RbacPermissionService rbacPermissionService;
    private final WorkspaceRateLimiter workspaceRateLimiter;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Get EP workspace JSON",
               description = "Returns the workspace JSON object. Current version is in X-EP-Workspace-Version response header. Every access is HIPAA-audited (EP-10).")
    public ResponseEntity<JsonNode> getWorkspace(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            HttpServletRequest request) {

        if (organizationId == null) {
            epDoctorWorkspaceService.logReadAudit(userId, null, resolveClientIp(request), HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().build();
        }
        rbacPermissionService.requirePrescriptionView(userId, organizationId);

        WorkspaceResult result = epDoctorWorkspaceService.getWorkspace(userId, organizationId);
        ResponseEntity<JsonNode> response;
        try {
            String raw = (result.dataJson() == null || result.dataJson().isBlank()) ? "{}" : result.dataJson();
            JsonNode node = objectMapper.readTree(raw);
            response = ResponseEntity.ok()
                    .header("X-EP-Workspace-Version", String.valueOf(result.version()))
                    .body(node);
        } catch (Exception e) {
            log.warn("Invalid workspace JSON for user {}", userId, e);
            response = ResponseEntity.ok()
                    .header("X-EP-Workspace-Version", String.valueOf(result.version()))
                    .body(objectMapper.createObjectNode());
        }

        // EP-10 HIPAA: audit every GET — written in a separate transaction so audit
        // failure cannot roll back or delay the data response.
        epDoctorWorkspaceService.logReadAudit(
                userId, organizationId, resolveClientIp(request), response.getStatusCode().value());

        return response;
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Save EP workspace JSON",
               description = "Replaces workspace payload (audited, EP-10). Requires X-EP-Client-Version header matching "
                           + "the current server version; returns 409 on version mismatch, 429 when the per-user "
                           + "2-second rate limit is exceeded.")
    public ResponseEntity<JsonNode> putWorkspace(
            @RequestBody(required = false) JsonNode body,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestHeader(value = "X-EP-Client-Version", defaultValue = "0") int clientVersion,
            HttpServletRequest request) {

        if (organizationId == null) {
            return ResponseEntity.badRequest().build();
        }
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);

        // ── Rate limit check (EP-8: max 1 write per 2 s per user) ───────────────
        if (!workspaceRateLimiter.tryAcquire(userId)) {
            long retryAfterMs = workspaceRateLimiter.retryAfterMs(userId);
            long retryAfterSec = Math.max(1L, (retryAfterMs + 999L) / 1000L); // ceil to whole seconds
            ObjectNode rateLimited = objectMapper.createObjectNode();
            rateLimited.put("rateLimited", true);
            rateLimited.put("retryAfterMs", retryAfterMs);
            rateLimited.put("message", "Workspace writes are limited to one every 2 seconds. Please retry shortly.");
            log.debug("EP workspace rate limit hit for user {} org {} — retry in {}ms", userId, organizationId, retryAfterMs);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfterSec))
                    .body(rateLimited);
        }
        // ────────────────────────────────────────────────────────────────────────

        JsonNode payload = (body == null || body.isNull()) ? objectMapper.createObjectNode() : body;
        try {
            String dataJson = objectMapper.writeValueAsString(payload);
            WorkspaceResult saved = epDoctorWorkspaceService
                    .saveWorkspaceDataJson(userId, organizationId, dataJson, clientVersion);

            ResponseEntity<JsonNode> response = ResponseEntity.ok()
                    .header("X-EP-Workspace-Version", String.valueOf(saved.version()))
                    .body(objectMapper.readTree(saved.dataJson()));

            // EP-10 HIPAA: audit every PUT — separate transaction so a failed audit write
            // cannot roll back the workspace data that was already committed.
            epDoctorWorkspaceService.logWriteAudit(
                    saved.workspaceId(), userId, organizationId,
                    resolveClientIp(request), response.getStatusCode().value());

            return response;

        } catch (WorkspaceConflictException e) {
            log.warn("EP workspace conflict for user {} org {}: client={} server={}",
                    userId, organizationId, clientVersion, e.getServerVersion());
            ObjectNode conflict = objectMapper.createObjectNode();
            conflict.put("conflict", true);
            conflict.put("serverVersion", e.getServerVersion());
            conflict.put("message",
                    "Workspace was modified by another session. Refresh to get the latest version.");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-EP-Workspace-Version", String.valueOf(e.getServerVersion()))
                    .body(conflict);

        } catch (IllegalArgumentException e) {
            // Schema validation (EP-10 M1) and size-limit errors are client mistakes → 400.
            log.warn("EP workspace rejected for user {} org {}: {}", userId, organizationId, e.getMessage());
            ObjectNode bad = objectMapper.createObjectNode();
            bad.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(bad);

        } catch (Exception e) {
            log.error("Failed to save EP workspace for user {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Resolves the real client IP, preferring the {@code X-Forwarded-For} header set by
     * the API Gateway, falling back to the direct TCP remote address.
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; leftmost is the originating client.
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
