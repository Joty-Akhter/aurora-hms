package com.easyops.accounting.controller;

import com.easyops.accounting.dto.DashboardSummaryResponse;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.security.RbacRequestHeaders;
import com.easyops.accounting.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting Dashboard", description = "Dashboard summary and analytics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AccountingRbacService accountingRbac;

    @GetMapping("/summary")
    @Operation(summary = "Get accounting dashboard summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/accounting/dashboard/summary - organizationId: {}", organizationId);

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(organizationId, actor);
        return ResponseEntity.ok(summary);
    }
}
