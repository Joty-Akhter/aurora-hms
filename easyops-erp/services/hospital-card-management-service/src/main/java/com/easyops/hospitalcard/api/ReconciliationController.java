package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.report.ReconciliationService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Phase 5.2 – Reconciliation: card-side export and compare with Billing/Canteen.
 */
@RestController
@RequestMapping("/api/hospital-card-management/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final HospitalCardRbacService hospitalCardRbac;

    @GetMapping("/card-vs-billing")
    public List<ReconciliationItem> getCardVsBilling(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "sourceSystem", required = false) String sourceSystem,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime fromDt = parseOffsetDateTime(from);
        OffsetDateTime toDt = parseOffsetDateTime(to);
        return reconciliationService.getCardSideExport(fromDt, toDt, sourceSystem);
    }

    @PostMapping("/compare")
    public List<ReconciliationMatchResult> compare(
            @Valid @RequestBody List<ReconciliationEntryRequest> entries,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return reconciliationService.compare(entries);
    }

    @PostMapping("/mismatches")
    public List<ReconciliationMatchResult> mismatches(
            @Valid @RequestBody List<ReconciliationEntryRequest> entries,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return reconciliationService.mismatchesOnly(entries);
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + value + ". Use ISO-8601 (e.g. 2024-01-01T00:00:00Z)");
        }
    }
}
