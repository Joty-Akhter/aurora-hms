package com.easyops.hr.controller;

import com.easyops.hr.dto.*;
import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanRepaymentTransaction;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 3: loan accounts, disbursement, manual repayments.
 */
@RestController
@RequestMapping("/api/hr/loans/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmployeeLoanController {

    private final EmployeeLoanService employeeLoanService;
    private final LoanRbacService loanRbac;

    /** AD-03: bulk re-apply holiday rules (active + settlement-pending loans with disbursement date). */
    @PostMapping("/recalculate-holiday-dates/all")
    public ResponseEntity<LoanBulkHolidayRecalcResultDto> recalculateHolidayDueDatesAll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.recalculateInstallmentDueDatesForHolidaysAll(organizationId, actor));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeLoanDto>> list(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) EmployeeLoanStatus status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.listLoans(organizationId, employeeId, status));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<EmployeeLoanDto> get(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.getLoan(organizationId, loanId));
    }

    /** AD-03: re-apply holiday/weekend shift to unpaid installments (after calendar or settings change). */
    @PostMapping("/{loanId}/installments/recalculate-holiday-dates")
    public ResponseEntity<EmployeeLoanDto> recalculateHolidayDueDates(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.recalculateInstallmentDueDatesForHolidays(organizationId, loanId, actor));
    }

    /** ST-04: update legal workflow label (not a full BPM). */
    @PatchMapping("/{loanId}/legal-workflow")
    public ResponseEntity<EmployeeLoanDto> patchLegalWorkflow(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanLegalWorkflowPatchRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.patchLegalWorkflow(organizationId, loanId, body.getLegalWorkflowStatus(), actor));
    }

    @PostMapping("/{loanId}/disburse")
    public ResponseEntity<EmployeeLoanDto> disburse(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanDisbursementRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.disburseEmployeeLoan(organizationId, loanId, body, actor));
    }

    @PostMapping("/{loanId}/repayments")
    public ResponseEntity<LoanManualRepaymentResultDto> repay(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanManualRepaymentRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.recordManualRepayment(organizationId, loanId, body, actor));
    }

    @GetMapping("/{loanId}/repayments")
    public ResponseEntity<List<LoanRepaymentTransaction>> listRepayments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.listRepaymentTransactions(organizationId, loanId));
    }

    /** RP-01: skip an installment with reason (administrative waiver for the period). */
    @PostMapping("/{loanId}/installments/{installmentId}/skip")
    public ResponseEntity<LoanInstallmentDto> skipInstallment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @PathVariable UUID installmentId,
            @Valid @RequestBody LoanInstallmentSkipRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.skipInstallment(organizationId, loanId, installmentId, body, actor));
    }

    /** RP-05: controlled reversal of a payroll posting with full audit trail. */
    @PostMapping("/{loanId}/repayments/{transactionId}/reverse-payroll")
    public ResponseEntity<LoanManualRepaymentResultDto> reversePayrollRepayment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @PathVariable UUID transactionId,
            @Valid @RequestBody LoanPayrollRepaymentReversalRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.reversePayrollRepayment(organizationId, loanId, transactionId, body, actor));
    }
}
