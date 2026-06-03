package com.easyops.accounting.service;

import com.easyops.accounting.dto.DashboardSummaryResponse;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.repository.JournalEntryRepository;
import com.easyops.accounting.repository.JournalLineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceRecentTransactionsTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalLineRepository journalLineRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void resolveTransactionAmount_usesHeaderTotalWhenPresent() {
        JournalEntry entry = postedEntry(new BigDecimal("1500.0000"));

        assertThat(dashboardService.resolveTransactionAmount(entry, Map.of()))
                .isEqualByComparingTo("1500.0000");
    }

    @Test
    void resolveTransactionAmount_fallsBackToLineSumWhenHeaderIsZero() {
        JournalEntry entry = postedEntry(BigDecimal.ZERO);
        Map<UUID, BigDecimal> lineTotals = Map.of(entry.getId(), new BigDecimal("2500.5000"));

        assertThat(dashboardService.resolveTransactionAmount(entry, lineTotals))
                .isEqualByComparingTo("2500.5000");
    }

    @Test
    void getDashboardSummary_populatesPostedRecentTransactionsWithAmounts() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        JournalEntry entry = postedEntry(new BigDecimal("999.0000"));
        entry.setJournalNumber("JV000042");
        entry.setJournalType("SYSTEM");
        entry.setDescription("Inventory receipt");

        when(journalEntryRepository.findTop10ByOrganizationIdAndStatusOrderByPostedAtDesc(
                organizationId, "POSTED")).thenReturn(List.of(entry));

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(organizationId, actorUserId);

        assertThat(summary.getRecentTransactions()).hasSize(1);
        DashboardSummaryResponse.RecentTransaction txn = summary.getRecentTransactions().getFirst();
        assertThat(txn.getAmount()).isEqualByComparingTo("999.0000");
        assertThat(txn.getReference()).isEqualTo("JV000042");
        assertThat(txn.getType()).isEqualTo("SYSTEM");
        assertThat(txn.getDescription()).isEqualTo("Inventory receipt");
        verify(journalEntryRepository).findTop10ByOrganizationIdAndStatusOrderByPostedAtDesc(
                eq(organizationId), eq("POSTED"));
        verify(journalLineRepository, org.mockito.Mockito.never())
                .sumDebitAmountsGroupedByJournalEntryId(any());
    }

    @Test
    void getDashboardSummary_batchesLineFallbackForZeroHeaderTotals() {
        UUID organizationId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        JournalEntry withHeader = postedEntry(new BigDecimal("100.0000"));
        JournalEntry legacy = postedEntry(BigDecimal.ZERO);
        legacy.setJournalNumber("JV000099");

        when(journalEntryRepository.findTop10ByOrganizationIdAndStatusOrderByPostedAtDesc(
                organizationId, "POSTED")).thenReturn(List.of(withHeader, legacy));
        when(journalLineRepository.sumDebitAmountsGroupedByJournalEntryId(List.of(legacy.getId())))
                .thenReturn(List.<Object[]>of(new Object[]{legacy.getId(), new BigDecimal("42.0000")}));

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(organizationId, actorUserId);

        assertThat(summary.getRecentTransactions()).hasSize(2);
        assertThat(summary.getRecentTransactions().get(0).getAmount()).isEqualByComparingTo("100.0000");
        assertThat(summary.getRecentTransactions().get(1).getAmount()).isEqualByComparingTo("42.0000");
    }

    private JournalEntry postedEntry(BigDecimal totalDebit) {
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setOrganizationId(UUID.randomUUID());
        entry.setJournalDate(LocalDate.of(2026, 1, 15));
        entry.setStatus("POSTED");
        entry.setPostedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        entry.setTotalDebit(totalDebit);
        return entry;
    }
}
