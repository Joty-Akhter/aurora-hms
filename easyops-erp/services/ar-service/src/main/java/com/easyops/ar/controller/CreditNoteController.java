package com.easyops.ar.controller;

import com.easyops.ar.dto.CreditNoteRequest;
import com.easyops.ar.entity.ARCreditNote;
import com.easyops.ar.security.AccountingRbacService;
import com.easyops.ar.security.RbacRequestHeaders;
import com.easyops.ar.service.CreditNoteService;
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
@RequestMapping("/api/ar/credit-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AR Credit Notes", description = "Credit note management for Accounts Receivable")
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all credit notes for an organization")
    public ResponseEntity<List<ARCreditNote>> getAllCreditNotes(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/credit-notes - organizationId: {}, status: {}", organizationId, status);
        List<ARCreditNote> creditNotes = status != null
                ? creditNoteService.getCreditNotesByStatus(organizationId, status)
                : creditNoteService.getAllCreditNotes(organizationId);
        return ResponseEntity.ok(creditNotes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get credit note by ID")
    public ResponseEntity<ARCreditNote> getCreditNoteById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARCreditNote creditNote = creditNoteService.getCreditNoteById(id);
        accountingRbac.requireAccountingView(actor, creditNote.getOrganizationId());
        log.info("GET /api/ar/credit-notes/{}", id);
        return ResponseEntity.ok(creditNote);
    }

    @PostMapping
    @Operation(summary = "Create new credit note")
    public ResponseEntity<ARCreditNote> createCreditNote(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody CreditNoteRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        log.info("POST /api/ar/credit-notes - Creating credit note: {}", request.getCreditNoteNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(creditNoteService.createCreditNote(request));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post credit note (change status from DRAFT to POSTED)")
    public ResponseEntity<ARCreditNote> postCreditNote(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARCreditNote existing = creditNoteService.getCreditNoteById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("POST /api/ar/credit-notes/{}/post", id);
        return ResponseEntity.ok(creditNoteService.postCreditNote(id, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete credit note")
    public ResponseEntity<Void> deleteCreditNote(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARCreditNote existing = creditNoteService.getCreditNoteById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/ar/credit-notes/{}", id);
        creditNoteService.deleteCreditNote(id);
        return ResponseEntity.noContent().build();
    }
}
