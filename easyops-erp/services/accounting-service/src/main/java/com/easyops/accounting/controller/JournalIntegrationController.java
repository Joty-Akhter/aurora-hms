package com.easyops.accounting.controller;

import com.easyops.accounting.dto.JournalEntryRequest;
import com.easyops.accounting.dto.JournalLineRequest;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.security.RbacRequestHeaders;
import com.easyops.accounting.service.ChartOfAccountsService;
import com.easyops.accounting.service.JournalPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Integration controller for external services (e.g. pharma) to post journal entries.
 * Accepts Map-based payload with accountCode instead of accountId for easier integration.
 */
@RestController
@RequestMapping("/api/accounting/journal-entries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Journal Integration", description = "Integration endpoints for external services")
public class JournalIntegrationController {

    private final ChartOfAccountsService chartOfAccountsService;
    private final JournalPostingService journalPostingService;
    private final AccountingRbacService accountingRbac;

    @PostMapping
    @Operation(summary = "Create journal entry (e.g. deposit)")
    public ResponseEntity<Map<String, Object>> createJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        return createJournalEntryInternal(userIdHeader, request);
    }

    @PostMapping("/incentive")
    @Operation(summary = "Create incentive journal entry")
    public ResponseEntity<Map<String, Object>> createIncentiveJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        return createJournalEntryInternal(userIdHeader, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get journal entry by ID (integration read)")
    public ResponseEntity<Map<String, Object>> getJournalEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        JournalEntry journal = journalPostingService.getJournalEntry(id);
        accountingRbac.requireAccountingView(actor, journal.getOrganizationId());
        return ResponseEntity.ok(toIntegrationResponse(journal));
    }

    private ResponseEntity<Map<String, Object>> createJournalEntryInternal(
            String userIdHeader,
            Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Object orgIdObj = request.get("organizationId");
        if (orgIdObj == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        UUID orgId = UUID.fromString(orgIdObj.toString());
        accountingRbac.requireAccountingManage(actor, orgId);

        try {
            JournalEntryRequest journalRequest = mapToJournalEntryRequest(request);
            JournalEntry journal = journalPostingService.createJournalEntry(journalRequest, actor);

            boolean postImmediately = "POSTED".equals(request.get("status"));
            if (postImmediately) {
                journal = journalPostingService.postJournalEntry(journal.getId(), actor);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", journal.getId());
            result.put("journalNumber", journal.getJournalNumber());
            result.put("status", journal.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create journal entry from integration request: {}", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private JournalEntryRequest mapToJournalEntryRequest(Map<String, Object> request) {
        Object orgIdObj = request.get("organizationId");
        if (orgIdObj == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        JournalEntryRequest journalRequest = new JournalEntryRequest();
        journalRequest.setOrganizationId(UUID.fromString(orgIdObj.toString()));
        Object entryDate = request.get("entryDate");
        if (entryDate == null) {
            entryDate = request.get("journalDate");
        }
        if (entryDate == null) {
            throw new IllegalArgumentException("entryDate is required");
        }
        journalRequest.setJournalDate(parseEntryDate(entryDate));
        journalRequest.setJournalType(request.get("journalType") != null ? request.get("journalType").toString() : "MANUAL");
        journalRequest.setReferenceNumber(request.get("referenceId") != null ? request.get("referenceId").toString() : null);
        journalRequest.setDescription(request.get("description") != null ? request.get("description").toString() : "Integration entry");
        if (request.get("sourceModule") != null) {
            journalRequest.setSourceModule(request.get("sourceModule").toString());
        }
        Object sourceDocId = request.get("sourceDocumentId");
        if (sourceDocId != null) {
            journalRequest.setSourceDocumentId(UUID.fromString(sourceDocId.toString()));
        }

        List<Map<String, Object>> linesMap = (List<Map<String, Object>>) request.get("lines");
        if (linesMap == null || linesMap.isEmpty()) {
            throw new IllegalArgumentException("At least one journal line is required");
        }
        List<JournalLineRequest> lines = new ArrayList<>();
        UUID orgId = journalRequest.getOrganizationId();

        for (Map<String, Object> lineMap : linesMap) {
            JournalLineRequest line = new JournalLineRequest();
            Object accountIdObj = lineMap.get("accountId");
            if (accountIdObj != null) {
                line.setAccountId(UUID.fromString(accountIdObj.toString()));
            } else {
                Object codeObj = lineMap.get("accountCode");
                if (codeObj == null) {
                    throw new IllegalArgumentException("accountCode or accountId is required for each journal line");
                }
                ChartOfAccounts account = chartOfAccountsService.getAccountByCode(orgId, codeObj.toString());
                line.setAccountId(account.getId());
            }
            line.setDebitAmount(toBigDecimal(lineMap.get("debitAmount")));
            line.setCreditAmount(toBigDecimal(lineMap.get("creditAmount")));
            line.setDescription(lineMap.get("description") != null ? lineMap.get("description").toString() : null);
            lines.add(line);
        }

        journalRequest.setLines(lines);
        return journalRequest;
    }

    private LocalDate parseEntryDate(Object entryDate) {
        if (entryDate instanceof LocalDate d) {
            return d;
        }
        String text = entryDate.toString();
        if (text.length() >= 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
            return LocalDate.parse(text.substring(0, 10));
        }
        return LocalDate.parse(text);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return switch (value) {
            case BigDecimal bd -> bd;
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            default -> new BigDecimal(value.toString());
        };
    }

    private Map<String, Object> toIntegrationResponse(JournalEntry journal) {
        List<JournalLine> lines = journalPostingService.getJournalLines(journal.getId());
        List<Map<String, Object>> lineMaps = new ArrayList<>();
        for (JournalLine line : lines) {
            ChartOfAccounts account = chartOfAccountsService.getAccountById(line.getAccountId());
            Map<String, Object> lineMap = new HashMap<>();
            lineMap.put("accountCode", account.getAccountCode());
            lineMap.put("accountName", account.getAccountName());
            lineMap.put("debitAmount", line.getDebitAmount());
            lineMap.put("creditAmount", line.getCreditAmount());
            lineMap.put("description", line.getDescription());
            lineMaps.add(lineMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", journal.getId());
        result.put("journalNumber", journal.getJournalNumber());
        result.put("status", journal.getStatus());
        result.put("organizationId", journal.getOrganizationId());
        result.put("entryDate", journal.getJournalDate());
        result.put("journalType", journal.getJournalType());
        result.put("referenceId", journal.getReferenceNumber());
        result.put("description", journal.getDescription());
        result.put("lines", lineMaps);
        return result;
    }
}
