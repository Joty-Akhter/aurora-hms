package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.report.CardReportService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Phase 5.1 – Reporting endpoints: liabilities, usage by domain, corporate exposure.
 */
@RestController
@RequestMapping("/api/hospital-card-management/reports")
@RequiredArgsConstructor
public class CardReportController {

    private final CardReportService cardReportService;
    private final HospitalCardRbacService hospitalCardRbac;

    @GetMapping("/liabilities")
    public List<LiabilityReportItem> getLiabilities(
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "cardProductId", required = false) UUID cardProductId,
            @RequestParam(value = "ownerType", required = false) String ownerType,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime asOfDt = parseOffsetDateTime(asOf);
        return cardReportService.getLiabilities(asOfDt, cardProductId, ownerType);
    }

    @GetMapping("/usage-by-domain")
    public List<UsageByDomainItem> getUsageByDomain(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "sourceSystem", required = false) String sourceSystem,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime fromDt = parseOffsetDateTime(from);
        OffsetDateTime toDt = parseOffsetDateTime(to);
        return cardReportService.getUsageByDomain(fromDt, toDt, sourceSystem);
    }

    @GetMapping("/corporate-exposure")
    public CorporateExposureResponse getCorporateExposure(
            @RequestParam(value = "corporateId") UUID corporateId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        OffsetDateTime asOfDt = parseOffsetDateTime(asOf);
        return cardReportService.getCorporateExposure(corporateId, asOfDt);
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
