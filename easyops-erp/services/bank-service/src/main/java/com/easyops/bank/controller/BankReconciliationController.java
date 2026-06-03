package com.easyops.bank.controller;

import com.easyops.bank.dto.ReconciliationRequest;
import com.easyops.bank.entity.BankReconciliation;
import com.easyops.bank.security.AccountingRbacService;
import com.easyops.bank.security.RbacRequestHeaders;
import com.easyops.bank.service.BankAccountService;
import com.easyops.bank.service.BankReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank/reconciliations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bank Reconciliations", description = "Bank reconciliation management")
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;
    private final BankAccountService bankAccountService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get reconciliations by account")
    public ResponseEntity<List<BankReconciliation>> getReconciliationsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID accountId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bankAccountService.getAccountById(accountId).getOrganizationId();
        accountingRbac.requireAccountingView(actor, orgId);
        return ResponseEntity.ok(reconciliationService.getReconciliationsByAccount(accountId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reconciliation by ID")
    public ResponseEntity<BankReconciliation> getReconciliationById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankReconciliation r = reconciliationService.getReconciliationById(id);
        UUID orgId = r.getBankAccount().getOrganizationId();
        accountingRbac.requireAccountingView(actor, orgId);
        return ResponseEntity.ok(r);
    }

    @PostMapping
    @Operation(summary = "Create new reconciliation")
    public ResponseEntity<BankReconciliation> createReconciliation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ReconciliationRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bankAccountService.getAccountById(request.getBankAccountId()).getOrganizationId();
        accountingRbac.requireAccountingManage(actor, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationService.createReconciliation(request));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete reconciliation")
    public ResponseEntity<BankReconciliation> completeReconciliation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankReconciliation r = reconciliationService.getReconciliationById(id);
        UUID orgId = r.getBankAccount().getOrganizationId();
        accountingRbac.requireAccountingManage(actor, orgId);
        return ResponseEntity.ok(reconciliationService.completeReconciliation(id, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reconciliation")
    public ResponseEntity<Void> deleteReconciliation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankReconciliation r = reconciliationService.getReconciliationById(id);
        UUID orgId = r.getBankAccount().getOrganizationId();
        accountingRbac.requireAccountingManage(actor, orgId);
        reconciliationService.deleteReconciliation(id);
        return ResponseEntity.noContent().build();
    }
}
