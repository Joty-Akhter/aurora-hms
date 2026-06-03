package com.easyops.hr.controller;

import com.easyops.hr.dto.LoanRecoveryLineDto;
import com.easyops.hr.dto.LoanRepaymentAnomalyDto;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanPayrollRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Phase 4: payroll recovery preview and idempotent confirmation (RP-04, PI-01–PI-04).
 * Correlation: {@code X-Correlation-Id} is set on every response (see {@link com.easyops.hr.integration.IntegrationCorrelationFilter}, INT-42).
 */
@RestController
@RequestMapping("/api/hr/loans/payroll-recoveries")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanPayrollRecoveryController {

    private final LoanPayrollRecoveryService loanPayrollRecoveryService;
    private final LoanRbacService loanRbac;

    /**
     * RP-05: list recent payroll reversal events for monitoring (idempotent posting still enforced at confirm).
     */
    @GetMapping("/anomalies")
    public ResponseEntity<List<LoanRepaymentAnomalyDto>> listAnomalies(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requirePayrollRecoveriesRead(actor, organizationId);
        return ResponseEntity.ok(loanPayrollRecoveryService.listPayrollRecoveryAnomalies(organizationId, since));
    }

    /** RP-05: payslip loan lines vs loan postings; missing postings on finalized runs. */
    @GetMapping("/cross-check")
    public ResponseEntity<List<LoanRepaymentAnomalyDto>> crossCheck(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID payrollRunId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requirePayrollRecoveriesRead(actor, organizationId);
        return ResponseEntity.ok(loanPayrollRecoveryService.listPayrollCrossCheck(organizationId, payrollRunId));
    }

    /**
     * Recovery due per active loan for the period; pass payrollRunId to preview idempotency (zero / flagged if already posted).
     */
    @GetMapping
    public ResponseEntity<List<LoanRecoveryLineDto>> getRecoveriesForPayroll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) UUID payrollRunId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requirePayrollRecoveriesRead(actor, organizationId);
        return ResponseEntity.ok(loanPayrollRecoveryService.getRecoveriesForPayroll(
                organizationId, periodStart, periodEnd, payrollRunId));
    }

    /**
     * Post PAYROLL repayments for the run (idempotent per loan+run).
     * Also runs automatically when a payroll run is processed or approved via {@link com.easyops.hr.service.PayrollService}.
     */
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmPayrollDeductions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID payrollRunId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        loanPayrollRecoveryService.confirmPayrollDeductions(payrollRunId, organizationId, actor);
        return ResponseEntity.noContent().build();
    }
}
