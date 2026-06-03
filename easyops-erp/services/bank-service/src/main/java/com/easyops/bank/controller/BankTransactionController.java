package com.easyops.bank.controller;

import com.easyops.bank.dto.TransactionRequest;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.entity.BankTransaction;
import com.easyops.bank.security.AccountingRbacService;
import com.easyops.bank.security.RbacRequestHeaders;
import com.easyops.bank.service.BankAccountService;
import com.easyops.bank.service.BankTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bank Transactions", description = "Bank transaction management")
public class BankTransactionController {

    private final BankTransactionService transactionService;
    private final BankAccountService bankAccountService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get transactions by account")
    public ResponseEntity<List<BankTransaction>> getTransactionsByAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount account = bankAccountService.getAccountById(accountId);
        accountingRbac.requireAccountingView(actor, account.getOrganizationId());

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByDateRange(accountId, startDate, endDate));
        }
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    @GetMapping("/unreconciled")
    @Operation(summary = "Get unreconciled transactions")
    public ResponseEntity<List<BankTransaction>> getUnreconciledTransactions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID accountId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount account = bankAccountService.getAccountById(accountId);
        accountingRbac.requireAccountingView(actor, account.getOrganizationId());
        return ResponseEntity.ok(transactionService.getUnreconciledTransactions(accountId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<BankTransaction> getTransactionById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankTransaction tx = transactionService.getTransactionById(id);
        BankAccount account = bankAccountService.getAccountById(tx.getBankAccountId());
        accountingRbac.requireAccountingView(actor, account.getOrganizationId());
        return ResponseEntity.ok(tx);
    }

    @PostMapping
    @Operation(summary = "Create new transaction")
    public ResponseEntity<BankTransaction> createTransaction(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TransactionRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankAccount account = bankAccountService.getAccountById(request.getBankAccountId());
        accountingRbac.requireAccountingManage(actor, account.getOrganizationId());
        request.setCreatedBy(actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(request, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction")
    public ResponseEntity<Void> deleteTransaction(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        BankTransaction tx = transactionService.getTransactionById(id);
        BankAccount account = bankAccountService.getAccountById(tx.getBankAccountId());
        accountingRbac.requireAccountingManage(actor, account.getOrganizationId());
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
