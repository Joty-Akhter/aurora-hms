package com.easyops.hr.controller;

import com.easyops.hr.dto.*;
import com.easyops.hr.entity.LoanApplicationStatus;
import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Phase 2 + AL-03 multi-step approval (HR → Finance), clarification, delegation (NF-03 granular RBAC).
 */
@RestController
@RequestMapping("/api/hr/loans/applications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final LoanRbacService loanRbac;
    private final HrRbacService hrRbac;

    @PostMapping
    public ResponseEntity<LoanApplicationDto> create(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @Valid @RequestBody LoanApplicationCreateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansApply(actor, organizationId);
        LoanApplicationDto created = loanApplicationService.create(organizationId, body, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<LoanApplicationDto> update(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId,
            @Valid @RequestBody LoanApplicationUpdateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansApply(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.update(organizationId, applicationId, body, actor));
    }

    @PostMapping("/{applicationId}/submit")
    public ResponseEntity<LoanApplicationDto> submit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansApply(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.submit(organizationId, applicationId, actor));
    }

    /**
     * AL-03: advances workflow — HR approval when SUBMITTED, Finance approval when PENDING_FINANCE_APPROVAL,
     * or single-step when org {@code skipFinanceApproval} is true.
     */
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<LoanApplicationDto> approve(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId,
            @RequestBody(required = false) LoanApplicationDecisionRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        LoanApplicationDto current = loanApplicationService.get(organizationId, applicationId);
        if (current.getStatus() == LoanApplicationStatus.SUBMITTED) {
            if (current.getDelegatedToUserId() != null && !current.getDelegatedToUserId().equals(actor)) {
                hrRbac.requireHrManage(actor, organizationId);
            } else {
                loanRbac.requireHrLoanHrApprove(actor, organizationId);
            }
        } else if (current.getStatus() == LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            loanRbac.requireHrLoanFinanceApprove(actor, organizationId);
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not awaiting approval at this stage");
        }
        return ResponseEntity.ok(loanApplicationService.approveOrAdvance(organizationId, applicationId, actor, body));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<LoanApplicationDto> reject(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId,
            @Valid @RequestBody LoanApplicationRejectRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        LoanApplicationDto current = loanApplicationService.get(organizationId, applicationId);
        if (current.getStatus() == LoanApplicationStatus.SUBMITTED) {
            if (current.getDelegatedToUserId() != null && !current.getDelegatedToUserId().equals(actor)) {
                hrRbac.requireHrManage(actor, organizationId);
            } else {
                loanRbac.requireHrLoanHrApprove(actor, organizationId);
            }
        } else if (current.getStatus() == LoanApplicationStatus.PENDING_FINANCE_APPROVAL) {
            loanRbac.requireHrLoanFinanceApprove(actor, organizationId);
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application cannot be rejected in current status");
        }
        return ResponseEntity.ok(loanApplicationService.reject(organizationId, applicationId, actor, body.getReason()));
    }

    @PostMapping("/{applicationId}/cancel")
    public ResponseEntity<LoanApplicationDto> cancel(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansApply(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.cancel(organizationId, applicationId, actor));
    }

    @PostMapping("/{applicationId}/request-clarification")
    public ResponseEntity<LoanApplicationDto> requestClarification(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId,
            @Valid @RequestBody LoanApplicationClarificationRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoanWorkflowParticipant(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.requestClarification(organizationId, applicationId, actor, body.getMessage()));
    }

    @PostMapping("/{applicationId}/delegate")
    public ResponseEntity<LoanApplicationDto> delegateHr(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId,
            @Valid @RequestBody LoanApplicationDelegateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireHrLoanHrApprove(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.delegateHrApproval(organizationId, applicationId, actor, body.getDelegateToUserId()));
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationDto>> list(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LoanApplicationStatus status,
            @RequestParam(required = false) LoanCategoryType categoryType) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.list(organizationId, employeeId, status, categoryType));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<LoanApplicationDto> get(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.get(organizationId, applicationId));
    }

    @GetMapping("/{applicationId}/actions")
    public ResponseEntity<List<LoanApplicationActionDto>> listActions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID applicationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanApplicationService.listActions(organizationId, applicationId));
    }
}
