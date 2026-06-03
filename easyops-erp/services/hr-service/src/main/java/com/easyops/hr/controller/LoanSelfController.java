package com.easyops.hr.controller;

import com.easyops.hr.dto.EmployeeLoanDto;
import com.easyops.hr.dto.LoanNotificationEventDto;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanEmployeeSelfService;
import com.easyops.hr.service.LoanNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * RE-02 / RE-03: employee self-service for loans (user linked to employee) and notification feed.
 */
@RestController
@RequestMapping("/api/hr/loans/self")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanSelfController {

    private final LoanEmployeeSelfService loanEmployeeSelfService;
    private final LoanNotificationService loanNotificationService;

    @GetMapping("/my-loans")
    public ResponseEntity<List<EmployeeLoanDto>> myLoans(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return ResponseEntity.ok(loanEmployeeSelfService.listMyLoans(organizationId, actor));
    }

    @GetMapping("/my-loans/{loanId}")
    public ResponseEntity<EmployeeLoanDto> myLoan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID loanId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return ResponseEntity.ok(loanEmployeeSelfService.getMyLoan(organizationId, actor, loanId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<LoanNotificationEventDto>> notifications(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return ResponseEntity.ok(loanNotificationService.listForUser(organizationId, actor, unreadOnly));
    }

    @PatchMapping("/notifications/{eventId}/read")
    public ResponseEntity<Void> markNotificationRead(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID eventId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanNotificationService.markRead(organizationId, actor, eventId);
        return ResponseEntity.noContent().build();
    }
}
