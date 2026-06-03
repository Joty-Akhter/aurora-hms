package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.EpAdviceLinesRequest;
import com.easyops.hospital.service.EpAdviceCatalogService;
import com.easyops.hospital.service.RbacPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * EP (Easy Prescription) advice catalog: org-wide lookup rows plus per-user frequency for suggestions.
 */
@RestController
@RequestMapping("/api/easy-prescription/advice")
@RequiredArgsConstructor
@Tag(name = "EP Advice Catalog", description = "Ranked advice suggestions and catalog updates for prescriptions/templates")
public class EpAdviceController {

    private final EpAdviceCatalogService epAdviceCatalogService;
    private final RbacPermissionService rbacPermissionService;

    @GetMapping("/suggestions")
    @Operation(summary = "Ranked advice suggestions",
               description = "Returns advice lines ordered by this user's frequency of use, then remaining catalog entries. Optional substring filter on normalized text.")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "80") int limit) {

        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        return ResponseEntity.ok(epAdviceCatalogService.getSuggestions(userId, query == null ? "" : query, limit));
    }

    @PostMapping("/ensure")
    @Operation(summary = "Ensure advice exists in org catalog",
               description = "Normalizes and deduplicates lines against the ADVICE lookup category; creates missing rows. Does not change usage counters.")
    public ResponseEntity<Void> ensure(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @Valid @RequestBody(required = false) EpAdviceLinesRequest body) {

        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        epAdviceCatalogService.ensureLines(body != null ? body.getLines() : List.of());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/record-usage")
    @Operation(summary = "Record advice usage",
               description = "Ensures each line exists in the catalog and increments this user's usage counters (e.g. after saving a prescription or template).")
    public ResponseEntity<Void> recordUsage(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @Valid @RequestBody(required = false) EpAdviceLinesRequest body) {

        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        epAdviceCatalogService.recordUsage(userId, body != null ? body.getLines() : List.of());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dismiss")
    @Operation(summary = "Dismiss advice from personal suggestions",
               description = "Removes this user's usage counters for the given advice lines so they rank lower or disappear from autocomplete. Org catalog entries may still appear until hidden client-side.")
    public ResponseEntity<Void> dismiss(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @Valid @RequestBody(required = false) EpAdviceLinesRequest body) {

        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        epAdviceCatalogService.dismissSuggestions(userId, body != null ? body.getLines() : List.of());
        return ResponseEntity.noContent().build();
    }
}
