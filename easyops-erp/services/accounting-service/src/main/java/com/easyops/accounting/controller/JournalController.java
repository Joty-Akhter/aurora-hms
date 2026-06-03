package com.easyops.accounting.controller;

import com.easyops.accounting.dto.JournalEntryRequest;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.security.RbacRequestHeaders;
import com.easyops.accounting.service.JournalPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Journal Entries", description = "Journal entry posting and management")
public class JournalController {

    private final JournalPostingService journalService;
    private final AccountingRbacService accountingRbac;

    @PostMapping
    @Operation(summary = "Create journal entry (draft)")
    public ResponseEntity<JournalEntry> createJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody JournalEntryRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(journalService.createJournalEntry(request, actor));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get journal entries for organization")
    public ResponseEntity<Page<JournalEntry>> getJournalEntries(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            Pageable pageable) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(journalService.getJournalEntries(organizationId, pageable));
    }

    @GetMapping("/{journalEntryId}")
    @Operation(summary = "Get journal entry by ID")
    public ResponseEntity<JournalEntry> getJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID journalEntryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        JournalEntry entry = journalService.getJournalEntry(journalEntryId);
        accountingRbac.requireAccountingView(actor, entry.getOrganizationId());
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/{journalEntryId}/lines")
    @Operation(summary = "Get journal entry lines")
    public ResponseEntity<List<JournalLine>> getJournalLines(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID journalEntryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        JournalEntry entry = journalService.getJournalEntry(journalEntryId);
        accountingRbac.requireAccountingView(actor, entry.getOrganizationId());
        return ResponseEntity.ok(journalService.getJournalLines(journalEntryId));
    }

    @PostMapping("/{journalEntryId}/post")
    @Operation(summary = "Post journal entry to GL")
    public ResponseEntity<JournalEntry> postJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID journalEntryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        JournalEntry entry = journalService.getJournalEntry(journalEntryId);
        accountingRbac.requireAccountingManage(actor, entry.getOrganizationId());
        return ResponseEntity.ok(journalService.postJournalEntry(journalEntryId, actor));
    }

    @PostMapping("/{journalEntryId}/reverse")
    @Operation(summary = "Reverse a posted journal entry")
    public ResponseEntity<JournalEntry> reverseJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID journalEntryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        JournalEntry entry = journalService.getJournalEntry(journalEntryId);
        accountingRbac.requireAccountingManage(actor, entry.getOrganizationId());
        return ResponseEntity.ok(journalService.reverseJournalEntry(journalEntryId, actor));
    }
}
