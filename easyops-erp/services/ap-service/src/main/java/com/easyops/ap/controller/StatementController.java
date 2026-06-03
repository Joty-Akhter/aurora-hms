package com.easyops.ap.controller;

import com.easyops.ap.dto.VendorStatementResponse;
import com.easyops.ap.security.AccountingRbacService;
import com.easyops.ap.security.RbacRequestHeaders;
import com.easyops.ap.service.StatementService;
import com.easyops.ap.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/ap/statements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AP Statements", description = "Vendor statement generation for Accounts Payable")
public class StatementController {

    private final StatementService statementService;
    private final VendorService vendorService;
    private final AccountingRbacService accountingRbac;

    @GetMapping("/vendor/{vendorId}")
    @Operation(summary = "Generate vendor statement for a date range")
    public ResponseEntity<VendorStatementResponse> getVendorStatement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID vendorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = vendorService.getVendorById(vendorId).getOrganizationId();
        accountingRbac.requireAccountingView(actor, orgId);
        log.info("GET /api/ap/statements/vendor/{} - startDate: {}, endDate: {}", vendorId, startDate, endDate);
        return ResponseEntity.ok(statementService.generateVendorStatement(vendorId, startDate, endDate));
    }

    @PostMapping("/vendor/{vendorId}/email")
    @Operation(summary = "Email vendor statement")
    public ResponseEntity<String> emailVendorStatement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID vendorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = vendorService.getVendorById(vendorId).getOrganizationId();
        accountingRbac.requireAccountingManage(actor, orgId);
        log.info("POST /api/ap/statements/vendor/{}/email - startDate: {}, endDate: {}", vendorId, startDate, endDate);
        statementService.emailStatement(vendorId, startDate, endDate);
        return ResponseEntity.ok("Statement emailed successfully");
    }
}
