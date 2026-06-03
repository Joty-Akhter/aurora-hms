package com.easyops.crm.controller;

import com.easyops.crm.entity.Account;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;
    private final CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String search) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        log.info("GET /api/crm/accounts - organizationId: {}", organizationId);

        List<Account> accounts;

        if (search != null && !search.isEmpty()) {
            accounts = accountService.searchAccounts(organizationId, search);
        } else if (accountType != null) {
            accounts = accountService.getAccountsByType(organizationId, accountType);
        } else {
            accounts = accountService.getAllAccounts(organizationId);
        }

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        log.info("GET /api/crm/accounts/{}", id);
        Account account = accountService.getAccountById(id);
        crmRbac.requireCrmView(actor, account.getOrganizationId());
        return ResponseEntity.ok(account);
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Account account) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, account.getOrganizationId());
        log.info("POST /api/crm/accounts - Creating account");
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Account account) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, accountService.getOrganizationIdForAccount(id));
        log.info("PUT /api/crm/accounts/{}", id);
        Account updated = accountService.updateAccount(id, account);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, accountService.getOrganizationIdForAccount(id));
        log.info("DELETE /api/crm/accounts/{}", id);
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
