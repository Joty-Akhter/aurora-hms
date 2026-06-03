package com.easyops.accounting.controller;

import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.service.ChartOfAccountsService;
import com.easyops.accounting.service.JournalPostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalIntegrationControllerTest {

    private ChartOfAccountsService chartOfAccountsService;
    private JournalPostingService journalPostingService;
    private AccountingRbacService accountingRbac;
    private JournalIntegrationController controller;

    @BeforeEach
    void setUp() {
        chartOfAccountsService = mock(ChartOfAccountsService.class);
        journalPostingService = mock(JournalPostingService.class);
        accountingRbac = mock(AccountingRbacService.class);
        controller = new JournalIntegrationController(
                chartOfAccountsService, journalPostingService, accountingRbac);
    }

    @Test
    void getJournalEntry_returnsIntegrationMapWithAccountCodes() {
        UUID actor = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        JournalEntry journal = new JournalEntry();
        journal.setId(journalId);
        journal.setOrganizationId(orgId);
        journal.setJournalNumber("JV000001");
        journal.setStatus("POSTED");
        journal.setJournalDate(LocalDate.of(2026, 1, 15));
        journal.setJournalType("MANUAL");
        journal.setReferenceNumber("REF-1");
        journal.setDescription("Test entry");

        JournalLine line = new JournalLine();
        line.setAccountId(accountId);
        line.setDebitAmount(new BigDecimal("100"));
        line.setCreditAmount(BigDecimal.ZERO);
        line.setDescription("Line 1");

        ChartOfAccounts account = new ChartOfAccounts();
        account.setAccountCode("CASH");
        account.setAccountName("Cash");

        when(journalPostingService.getJournalEntry(journalId)).thenReturn(journal);
        when(journalPostingService.getJournalLines(journalId)).thenReturn(List.of(line));
        when(chartOfAccountsService.getAccountById(accountId)).thenReturn(account);

        var response = controller.getJournalEntry(actor.toString(), journalId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("id")).isEqualTo(journalId);
        assertThat(body.get("journalNumber")).isEqualTo("JV000001");
        assertThat(body.get("referenceId")).isEqualTo("REF-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).get("accountCode")).isEqualTo("CASH");

        verify(accountingRbac).requireAccountingView(eq(actor), eq(orgId));
    }
}
