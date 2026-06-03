package com.easyops.hr.controller;

import com.easyops.hr.dto.LoanApplicationActionDto;
import com.easyops.hr.dto.LoanCombinedAuditDto;
import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.LoanAuditLog;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanApplicationService;
import com.easyops.hr.service.LoanAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Phase 6 (RE-04): loan audit timeline for a loan account.
 */
@RestController
@RequestMapping("/api/hr/loans/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanAuditController {

    private final LoanAuditService loanAuditService;
    private final LoanApplicationService loanApplicationService;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanRbacService loanRbac;

    @GetMapping
    public ResponseEntity<List<LoanAuditLog>> listForLoan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanAuditService.listForLoan(organizationId, loanId));
    }

    /**
     * RE-04: org-scoped audit entries (PI-05 COA mapping changes, AD-03 bulk holiday recalc summary); not tied to a loan id.
     */
    @GetMapping("/org")
    public ResponseEntity<List<LoanAuditLog>> listOrgLoanConfigAudit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanAuditService.listOrgLoanConfigAudit(organizationId));
    }

    /**
     * RE-04: loan account events plus originating application workflow (when loan was created from an application).
     */
    @GetMapping("/combined")
    public ResponseEntity<LoanCombinedAuditDto> combined(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        EmployeeLoan loan = employeeLoanRepository.findByLoanIdAndOrganizationId(loanId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        List<LoanAuditLog> logs = loanAuditService.listForLoan(organizationId, loanId);
        List<LoanApplicationActionDto> actions = List.of();
        if (loan.getLoanApplicationId() != null) {
            actions = loanApplicationService.listActions(organizationId, loan.getLoanApplicationId());
        }
        return ResponseEntity.ok(LoanCombinedAuditDto.builder()
                .loanId(loanId)
                .loanApplicationId(loan.getLoanApplicationId())
                .loanAuditLogs(logs)
                .applicationWorkflowActions(actions)
                .build());
    }
}
