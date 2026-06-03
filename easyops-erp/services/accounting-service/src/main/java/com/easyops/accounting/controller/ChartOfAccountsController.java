package com.easyops.accounting.controller;

import com.easyops.accounting.dto.CoARequest;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.security.RbacRequestHeaders;
import com.easyops.accounting.service.ChartOfAccountsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/coa")
@RequiredArgsConstructor
@Tag(name = "Chart of Accounts", description = "Chart of Accounts management")
public class ChartOfAccountsController {

    private final ChartOfAccountsService coaService;
    private final AccountingRbacService accountingRbac;

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<ChartOfAccounts> createAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody CoARequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coaService.createAccount(request, actor));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get all accounts for organization")
    public ResponseEntity<List<ChartOfAccounts>> getOrganizationAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(coaService.getOrganizationAccounts(organizationId));
    }

    @GetMapping("/organization/{organizationId}/active")
    @Operation(summary = "Get active accounts for organization")
    public ResponseEntity<List<ChartOfAccounts>> getActiveAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(coaService.getActiveAccounts(organizationId));
    }

    @GetMapping("/organization/{organizationId}/posting")
    @Operation(summary = "Get posting accounts (non-group accounts)")
    public ResponseEntity<List<ChartOfAccounts>> getPostingAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(coaService.getPostingAccounts(organizationId));
    }

    @GetMapping("/organization/{organizationId}/type/{accountType}")
    @Operation(summary = "Get accounts by type")
    public ResponseEntity<List<ChartOfAccounts>> getAccountsByType(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            @PathVariable String accountType) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(coaService.getAccountsByType(organizationId, accountType));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ChartOfAccounts> getAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID accountId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ChartOfAccounts account = coaService.getAccountById(accountId);
        accountingRbac.requireAccountingView(actor, account.getOrganizationId());
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account")
    public ResponseEntity<ChartOfAccounts> updateAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID accountId,
            @Valid @RequestBody CoARequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ChartOfAccounts existing = coaService.getAccountById(accountId);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(coaService.updateAccount(accountId, request, actor));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<Void> deactivateAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID accountId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, organizationId);
        coaService.deactivateAccount(accountId, organizationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/organization/{organizationId}/load-standard-coa")
    @Operation(summary = "Load standard Chart of Accounts template for organization")
    public ResponseEntity<String> loadStandardCoA(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, organizationId);
        try {
            int accountsCreated = coaService.loadStandardCoA(organizationId, actor);
            return ResponseEntity.ok("Successfully loaded " + accountsCreated + " accounts for organization.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
