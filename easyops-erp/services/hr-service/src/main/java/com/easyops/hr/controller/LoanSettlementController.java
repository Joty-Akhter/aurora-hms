package com.easyops.hr.controller;

import com.easyops.hr.dto.*;
import com.easyops.hr.integration.PfSettlementClient;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeLoanService;
import com.easyops.hr.service.LoanOrgSettingsProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 5: exit settlement workflow (ST-01–ST-05, BR-09).
 */
@RestController
@RequestMapping("/api/hr/loans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanSettlementController {

    private final EmployeeLoanService employeeLoanService;
    private final PfSettlementClient pfSettlementClient;
    private final LoanOrgSettingsProvider loanOrgSettingsProvider;
    private final LoanRbacService loanRbac;

    @PostMapping("/accounts/{loanId}/settlement/start")
    public ResponseEntity<EmployeeLoanDto> startSettlement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody(required = false) LoanSettlementStartRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        LoanSettlementStartRequest req = body != null ? body : new LoanSettlementStartRequest();
        return ResponseEntity.ok(employeeLoanService.startSettlement(organizationId, loanId, req, actor));
    }

    @PostMapping("/accounts/{loanId}/settlement/allocate")
    public ResponseEntity<LoanManualRepaymentResultDto> allocateSettlement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanSettlementAllocateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.allocateSettlementRepayment(organizationId, loanId, body, actor));
    }

    @PostMapping("/accounts/{loanId}/settlement/shortfall")
    public ResponseEntity<EmployeeLoanDto> recordShortfall(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanSettlementShortfallRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.recordSettlementShortfall(organizationId, loanId, body, actor));
    }

    @PostMapping("/accounts/{loanId}/settlement/close")
    public ResponseEntity<EmployeeLoanDto> closeSettlement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanSettlementCloseRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(employeeLoanService.closeSettlementLoan(organizationId, loanId, body, actor));
    }

    /**
     * Hint for PF amount when integrated; otherwise empty (manual entry).
     */
    @GetMapping("/settlements/pf-hint")
    public ResponseEntity<BigDecimal> getPfSettlementHint(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        Optional<BigDecimal> amt = pfSettlementClient.getAvailableSettlementAmount(organizationId, employeeId);
        return ResponseEntity.ok(amt.orElse(null));
    }

    /** ST-03: configured settlement source order for UI and validation. */
    @GetMapping("/settlements/allocation-priority")
    public ResponseEntity<List<String>> allocationPriority(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanOrgSettingsProvider.getSettings(organizationId).getSettlementAllocationPriority());
    }
}
