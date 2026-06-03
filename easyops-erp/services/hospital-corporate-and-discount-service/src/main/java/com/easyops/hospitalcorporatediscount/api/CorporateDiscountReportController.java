package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateUtilizationResponse;
import com.easyops.hospitalcorporatediscount.api.dto.DiscountSummaryResponse;
import com.easyops.hospitalcorporatediscount.domain.report.CorporateDiscountReportService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount/reports")
@RequiredArgsConstructor
public class CorporateDiscountReportController {

    private final CorporateDiscountReportService reportService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    /**
     * Corporate utilization: count of discount decisions per corporate in the date range.
     * If corporateId is provided, returns a single utilization; otherwise returns one entry per corporate.
     */
    @GetMapping("/corporate-utilization")
    public CorporateUtilizationResponse getCorporateUtilization(
            @RequestParam(value = "corporateId", required = false) UUID corporateId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return reportService.getCorporateUtilization(Optional.ofNullable(corporateId), from, to);
    }

    /**
     * Discount summary: total discount amount (and decision count) by scheme in the date range.
     * If schemeId is provided, returns a single scheme summary; otherwise returns one entry per scheme.
     */
    @GetMapping("/discount-summary")
    public DiscountSummaryResponse getDiscountSummary(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "schemeId", required = false) UUID schemeId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return reportService.getDiscountSummary(from, to, Optional.ofNullable(schemeId));
    }
}
