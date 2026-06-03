package com.easyops.ar.controller;

import com.easyops.ar.dto.AgingReportResponse;
import com.easyops.ar.security.AccountingRbacService;
import com.easyops.ar.security.RbacRequestHeaders;
import com.easyops.ar.service.AgingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ar/aging")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AR Aging Reports", description = "Aging reports for Accounts Receivable")
public class AgingReportController {

    private final AgingReportService agingReportService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Generate AR aging report")
    public ResponseEntity<List<AgingReportResponse>> getAgingReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/aging - organizationId: {}, asOfDate: {}", organizationId, asOfDate);
        return ResponseEntity.ok(agingReportService.generateAgingReport(organizationId, asOfDate));
    }
}
