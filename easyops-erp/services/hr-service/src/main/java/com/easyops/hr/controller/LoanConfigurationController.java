package com.easyops.hr.controller;

import com.easyops.hr.dto.*;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 1: loan org settings and loan categories (AD-01, LC-01–LC-02).
 */
@RestController
@RequestMapping("/api/hr/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanConfigurationController {

    private final LoanConfigurationService loanConfigurationService;
    private final LoanRbacService loanRbac;

    @GetMapping("/settings")
    public ResponseEntity<LoanOrganizationSettingsDto> getSettings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        log.debug("GET /api/hr/loans/settings organizationId={}", organizationId);
        return ResponseEntity.ok(loanConfigurationService.getSettings(organizationId));
    }

    @PatchMapping("/settings")
    public ResponseEntity<LoanOrganizationSettingsDto> patchSettings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @Valid @RequestBody LoanOrganizationSettingsPatchRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        log.info("PATCH /api/hr/loans/settings organizationId={}", organizationId);
        return ResponseEntity.ok(loanConfigurationService.patchSettings(organizationId, body));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<LoanCategoryDto>> listCategories(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanConfigurationService.listCategories(organizationId, includeInactive));
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<LoanCategoryDto> getCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID categoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanConfigurationService.getCategory(organizationId, categoryId));
    }

    @PostMapping("/categories")
    public ResponseEntity<LoanCategoryDto> createCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @Valid @RequestBody LoanCategoryCreateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        LoanCategoryDto created = loanConfigurationService.createCategory(organizationId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<LoanCategoryDto> updateCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody LoanCategoryUpdateRequest body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(loanConfigurationService.updateCategory(organizationId, categoryId, body));
    }

    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID categoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        loanConfigurationService.deactivateCategory(organizationId, categoryId);
    }
}
