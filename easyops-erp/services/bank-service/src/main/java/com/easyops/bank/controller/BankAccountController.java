package com.easyops.bank.controller;

import com.easyops.bank.dto.BankAccountRequest;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.security.AccountingRbacService;
import com.easyops.bank.security.RbacRequestHeaders;
import com.easyops.bank.service.BankAccountService;
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
@RequestMapping("/api/bank/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bank Accounts", description = "Bank account management")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all bank accounts")
    public ResponseEntity<List<BankAccount>> getAllAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        List<BankAccount> accounts = activeOnly
                ? bankAccountService.getActiveAccounts(organizationId)
                : bankAccountService.getAllAccounts(organizationId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bank account by ID")
    public ResponseEntity<BankAccount> getAccountById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount account = bankAccountService.getAccountById(id);
        accountingRbac.requireAccountingView(actor, account.getOrganizationId());
        return ResponseEntity.ok(account);
    }

    @PostMapping
    @Operation(summary = "Create new bank account")
    public ResponseEntity<BankAccount> createAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody BankAccountRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createAccount(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update bank account")
    public ResponseEntity<BankAccount> updateAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody BankAccountRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount existing = bankAccountService.getAccountById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(bankAccountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bank account")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount existing = bankAccountService.getAccountById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        bankAccountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
