package com.easyops.hospital.service;

import com.easyops.hospital.entity.EpDoctorWorkspace;
import com.easyops.hospital.entity.EpDoctorWorkspaceAudit;
import com.easyops.hospital.exception.WorkspaceConflictException;
import com.easyops.hospital.repository.EpDoctorWorkspaceAuditRepository;
import com.easyops.hospital.repository.EpDoctorWorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpDoctorWorkspaceService {

    private static final int MAX_JSON_CHARS = 2_000_000;

    /**
     * EP-10 schema version enforced on every write.  Clients must include
     * {@code "schemaVersion": <n>} in the workspace JSON root object.
     * The current production schema is version 1; higher values are accepted
     * for forward-compatibility.
     */
    static final int CURRENT_SCHEMA_VERSION = 1;

    private final EpDoctorWorkspaceRepository workspaceRepository;
    private final EpDoctorWorkspaceAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    /**
     * Carries the workspace identity, JSON payload, and current server version back to the
     * controller so it can attach {@code X-EP-Workspace-Version} to every response and
     * pass {@code workspaceId} to {@link #logWriteAudit}.
     */
    public record WorkspaceResult(UUID workspaceId, String dataJson, int version) {}

    /**
     * Returns the stored workspace, injecting {@code "schemaVersion": 0} into any legacy row
     * that pre-dates the EP-10 schema-version requirement.  The injection is applied only to
     * the returned value; the stored row is left untouched so this path stays read-only.
     */
    @Transactional(readOnly = true)
    public WorkspaceResult getWorkspace(UUID userId, UUID organizationId) {
        return workspaceRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .map(w -> {
                    String raw = w.getDataJson() != null ? w.getDataJson() : "{}";
                    return new WorkspaceResult(
                            w.getWorkspaceId(),
                            injectSchemaVersionIfMissing(raw),
                            w.getVersion() != null ? w.getVersion() : 0);
                })
                .orElse(new WorkspaceResult(null, "{}", 0));
    }

    /**
     * Records a HIPAA EP-10 READ audit for a workspace GET.
     * <p>
     * Runs in its own transaction so that an audit failure never rolls back the
     * successful GET response already sent to the client.  {@code snapshotJson}
     * is intentionally omitted — storing the workspace payload in the audit row
     * would persist PHI, which EP-10 explicitly prohibits for READ entries.
     *
     * @param userId         authenticated doctor
     * @param organizationId organisation scope of the request
     * @param ipAddress      client IP resolved from {@code X-Forwarded-For} / RemoteAddr
     * @param httpStatus     HTTP response status code returned to the client
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReadAudit(UUID userId, UUID organizationId, String ipAddress, int httpStatus) {
        try {
            EpDoctorWorkspaceAudit audit = EpDoctorWorkspaceAudit.builder()
                    .userId(userId)
                    .organizationId(organizationId)
                    .action("READ")
                    .ipAddress(ipAddress)
                    .httpStatus(httpStatus)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("EP-10 audit write failed for READ by user {} org {}: {}",
                    userId, organizationId, e.getMessage(), e);
        }
    }

    /**
     * Records a HIPAA EP-10 WRITE audit for a workspace PUT.
     * <p>
     * Runs in its own transaction so that an audit failure cannot roll back
     * workspace data that was already committed.
     *
     * <p><strong>EP-10 payload prohibition (Req-I3):</strong> the workspace JSON that was
     * written must <em>not</em> be stored in the audit row, exactly as for READ audit entries.
     * Only mutation metadata is permitted: {@code action, userId, organizationId, workspaceId,
     * ip_address, http_status, created_at}.  The {@code snapshot_json} database column that
     * previously existed was permanently dropped in changeset 051; this method does not accept
     * a payload parameter and must never be changed to do so.
     *
     * @param workspaceId    persisted workspace row ID (may be {@code null} for new rows created
     *                       in the same request — the write audit is still recorded)
     * @param userId         authenticated doctor
     * @param organizationId organisation scope of the request
     * @param ipAddress      client IP resolved from {@code X-Forwarded-For} / RemoteAddr
     * @param httpStatus     HTTP response status code returned to the client
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWriteAudit(UUID workspaceId, UUID userId, UUID organizationId,
                              String ipAddress, int httpStatus) {
        try {
            EpDoctorWorkspaceAudit audit = EpDoctorWorkspaceAudit.builder()
                    .workspaceId(workspaceId)
                    .userId(userId)
                    .organizationId(organizationId)
                    .action("UPSERT")
                    .ipAddress(ipAddress)
                    .httpStatus(httpStatus)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("EP-10 audit write failed for UPSERT by user {} org {}: {}",
                    userId, organizationId, e.getMessage(), e);
        }
    }

    /**
     * Saves the workspace JSON after validating schema version and optimistic version match.
     *
     * <h3>Schema validation (EP-10 M1)</h3>
     * The payload must be a JSON object containing a {@code schemaVersion} integer key.
     * Missing or non-object payloads are rejected with {@link IllegalArgumentException}
     * which the global exception handler maps to HTTP 400.
     *
     * <h3>Optimistic concurrency</h3>
     * <ul>
     *   <li>New workspace (no existing row): {@code clientVersion} must be {@code 0}.</li>
     *   <li>Existing workspace: {@code clientVersion} must equal {@code row.version}.</li>
     * </ul>
     * Throws {@link WorkspaceConflictException} — mapped to HTTP 409 — when the versions diverge.
     */
    @Transactional
    public WorkspaceResult saveWorkspaceDataJson(
            UUID userId, UUID organizationId, String dataJson, int clientVersion) {

        if (dataJson == null || dataJson.isBlank()) {
            dataJson = "{}";
        }
        if (dataJson.length() > MAX_JSON_CHARS) {
            throw new IllegalArgumentException("Workspace payload exceeds maximum size");
        }

        // EP-10 M1: enforce schemaVersion presence before touching the DB.
        validateSchemaVersion(dataJson);

        EpDoctorWorkspace row = workspaceRepository
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElse(null);

        if (row == null) {
            if (clientVersion != 0) {
                // Client claims an existing version but there is no row — treat as conflict
                // so the client re-reads and discovers the workspace is gone.
                throw new WorkspaceConflictException(0);
            }
            row = EpDoctorWorkspace.builder()
                    .userId(userId)
                    .organizationId(organizationId)
                    .dataJson("{}")
                    .version(0)
                    .build();
        } else {
            int dbVersion = row.getVersion() != null ? row.getVersion() : 0;
            if (clientVersion != dbVersion) {
                log.debug("EP workspace conflict for user {} org {}: client={} server={}",
                        userId, organizationId, clientVersion, dbVersion);
                throw new WorkspaceConflictException(dbVersion);
            }
        }

        row.setDataJson(dataJson);
        int nextVersion = (row.getVersion() != null ? row.getVersion() : 0) + 1;
        row.setVersion(nextVersion);
        row.setUpdatedAt(LocalDateTime.now());
        EpDoctorWorkspace saved = workspaceRepository.save(row);

        log.debug("Saved EP workspace for user {} org {} version {}", userId, organizationId, saved.getVersion());
        return new WorkspaceResult(saved.getWorkspaceId(), saved.getDataJson(), saved.getVersion());
    }

    // ---------------------------------------------------------------------------
    // Schema-version helpers (EP-10 M1)
    // ---------------------------------------------------------------------------

    /**
     * Validates that {@code dataJson} is a JSON object containing a {@code schemaVersion} field.
     *
     * <p>Throws {@link IllegalArgumentException} (→ HTTP 400) when:
     * <ul>
     *   <li>the string is not valid JSON</li>
     *   <li>the root value is not a JSON object</li>
     *   <li>the {@code schemaVersion} key is absent</li>
     *   <li>{@code schemaVersion} is not a JSON integer, or is negative (EP-10 M1)</li>
     * </ul>
     * Values greater than {@link #CURRENT_SCHEMA_VERSION} are accepted with a WARN log
     * (forward-compatible clients).
     */
    private void validateSchemaVersion(String dataJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(dataJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Workspace payload is not valid JSON: " + e.getOriginalMessage());
        }

        if (!root.isObject()) {
            throw new IllegalArgumentException(
                    "Workspace payload must be a JSON object (EP-10); received: "
                            + root.getNodeType());
        }

        if (!root.has("schemaVersion")) {
            throw new IllegalArgumentException(
                    "Workspace payload is missing required field 'schemaVersion' (EP-10). "
                            + "Add \"schemaVersion\": " + CURRENT_SCHEMA_VERSION
                            + " to the root object.");
        }

        JsonNode svNode = root.get("schemaVersion");
        if (!svNode.isNumber() || !svNode.isIntegralNumber()) {
            throw new IllegalArgumentException(
                    "Workspace field 'schemaVersion' must be an integer (EP-10 M1); received: "
                            + svNode.getNodeType());
        }
        int schemaVersion = svNode.intValue();
        if (schemaVersion < 0) {
            throw new IllegalArgumentException(
                    "Workspace field 'schemaVersion' must be non-negative (EP-10 M1).");
        }
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            log.warn("EP workspace schemaVersion {} is ahead of server-supported {} — accepting payload "
                    + "for forward compatibility; upgrade the server before relying on newer fields.",
                    schemaVersion, CURRENT_SCHEMA_VERSION);
        }
    }

    /**
     * Returns the workspace JSON with {@code schemaVersion: 0} injected when the key is absent.
     * Used on read to make legacy rows transparently forward-compatible without a data migration.
     * If parsing fails for any reason the original string is returned unchanged.
     */
    private String injectSchemaVersionIfMissing(String dataJson) {
        try {
            JsonNode root = objectMapper.readTree(dataJson);
            if (root.isObject() && !root.has("schemaVersion")) {
                ((ObjectNode) root).put("schemaVersion", 0);
                log.debug("EP-10: injected schemaVersion=0 into legacy workspace row on read");
                return objectMapper.writeValueAsString(root);
            }
        } catch (JsonProcessingException e) {
            log.warn("EP-10: could not parse stored workspace JSON to inject schemaVersion: {}", e.getMessage());
        }
        return dataJson;
    }
}
