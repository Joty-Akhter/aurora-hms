package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.PharmacyDirectoryRequest;
import com.easyops.hospital.dto.response.PharmacyDirectoryResponse;
import com.easyops.hospital.service.PharmacyDirectoryService;
import com.easyops.hospital.service.RbacPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FR-P3.5: Pharmacy Directory REST API.
 *
 * <ul>
 *   <li>GET  /api/pharmacy-directory/search          — prescription picker autocomplete (view)</li>
 *   <li>GET  /api/pharmacy-directory                 — full active list (view)</li>
 *   <li>GET  /api/pharmacy-directory/{id}            — single entry (view)</li>
 *   <li>GET  /api/pharmacy-directory/npi/{npi}       — lookup by NPI (view)</li>
 *   <li>GET  /api/pharmacy-directory/stale           — staleness report (manage)</li>
 *   <li>POST /api/pharmacy-directory                 — create entry (manage)</li>
 *   <li>PUT  /api/pharmacy-directory/{id}            — update entry (manage)</li>
 *   <li>POST /api/pharmacy-directory/{id}/verify     — mark verified (manage)</li>
 *   <li>DELETE /api/pharmacy-directory/{id}          — soft-delete / deactivate (manage)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pharmacy-directory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pharmacy Directory", description = "FR-P3.5 — Retail / e-prescribing pharmacy master data, NPI lookup, and staleness management")
public class PharmacyDirectoryController {

    private final PharmacyDirectoryService pharmacyDirectoryService;
    private final RbacPermissionService rbacPermissionService;

    // ──────────────────────────── Search / Lookup (view permission) ────────────────────────────

    @GetMapping("/search")
    @Operation(
        summary = "Search pharmacy directory",
        description = "Free-text search over name, city, NPI for the prescription pharmacy picker. " +
                      "Pass eprescribingOnly=true to restrict to e-Rx capable pharmacies."
    )
    public ResponseEntity<List<PharmacyDirectoryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "false") boolean eprescribingOnly,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.search(q, state, eprescribingOnly));
    }

    @GetMapping
    @Operation(summary = "List all active pharmacies", description = "Returns all active pharmacy directory entries ordered by name.")
    public ResponseEntity<List<PharmacyDirectoryResponse>> getAll(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pharmacy by ID")
    public ResponseEntity<PharmacyDirectoryResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.getById(id));
    }

    @GetMapping("/npi/{npi}")
    @Operation(summary = "Get pharmacy by NPI", description = "Lookup a specific pharmacy by its 10-digit NPI.")
    public ResponseEntity<PharmacyDirectoryResponse> getByNpi(
            @PathVariable String npi,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.getByNpi(npi));
    }

    // ──────────────────────────── Admin / Manage endpoints ────────────────────────────

    @GetMapping("/stale")
    @Operation(
        summary = "List stale pharmacy entries",
        description = "Returns all active pharmacies whose last_verified_at is null or older than " +
                      PharmacyDirectoryService.STALE_DAYS + " days. Use for admin reverification workflows."
    )
    public ResponseEntity<List<PharmacyDirectoryResponse>> getStale(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionManage(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.getStaleEntries());
    }

    @PostMapping
    @Operation(summary = "Create pharmacy directory entry", description = "Admin: add a new pharmacy to the directory.")
    public ResponseEntity<PharmacyDirectoryResponse> create(
            @Valid @RequestBody PharmacyDirectoryRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionManage(userId, organizationId);
        PharmacyDirectoryResponse response = pharmacyDirectoryService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pharmacy directory entry", description = "Admin: update an existing pharmacy entry.")
    public ResponseEntity<PharmacyDirectoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PharmacyDirectoryRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionManage(userId, organizationId);
        return ResponseEntity.ok(pharmacyDirectoryService.update(id, request, userId));
    }

    @PostMapping("/{id}/verify")
    @Operation(
        summary = "Mark pharmacy as verified",
        description = "Admin: update last_verified_at to now, clearing the stale flag. Optionally supply a verification note."
    )
    public ResponseEntity<PharmacyDirectoryResponse> verify(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionManage(userId, organizationId);
        String notes = body != null ? body.get("verificationNotes") : null;
        return ResponseEntity.ok(pharmacyDirectoryService.markVerified(id, notes, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate pharmacy", description = "Admin: soft-delete (set is_active=false). Does not affect existing prescriptions.")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionManage(userId, organizationId);
        pharmacyDirectoryService.deactivate(id, userId);
        return ResponseEntity.noContent().build();
    }
}
