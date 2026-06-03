package com.easyops.accounting.service;

import com.easyops.accounting.dto.CashFlowResponse;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialReportServiceCashFlowTest {

    @Mock
    private JournalLineRepository journalLineRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private ChartOfAccountsRepository coaRepository;
    @Mock
    private PeriodRepository periodRepository;
    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @InjectMocks
    private FinancialReportService financialReportService;

    private UUID organizationId;
    private UUID periodId;
    private UUID cashAccountId;
    private UUID arAccountId;
    private UUID revenueAccountId;
    private UUID expenseAccountId;
    private UUID entryId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        periodId = UUID.randomUUID();
        cashAccountId = UUID.randomUUID();
        arAccountId = UUID.randomUUID();
        revenueAccountId = UUID.randomUUID();
        expenseAccountId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        Period period = new Period();
        period.setId(periodId);
        period.setOrganizationId(organizationId);
        period.setPeriodName("Jan 2026");
        period.setStartDate(LocalDate.of(2026, 1, 1));
        period.setEndDate(LocalDate.of(2026, 1, 31));
        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));

        ChartOfAccounts cash = account(cashAccountId, "1010", "Cash on Hand", "ASSET", "Current Assets");
        ChartOfAccounts ar = account(arAccountId, "1110", "Trade Debtors", "ASSET", "Current Assets");
        ChartOfAccounts revenue = account(revenueAccountId, "4010", "Sales Revenue", "REVENUE", "Operating Revenue");
        ChartOfAccounts expense = account(expenseAccountId, "6010", "Salaries and Wages", "EXPENSE", "Operating Expenses");

        when(coaRepository.findByOrganizationIdOrderByAccountCode(organizationId))
            .thenReturn(List.of(cash, ar, revenue, expense));
        lenient().when(coaRepository.findById(cashAccountId)).thenReturn(Optional.of(cash));
        lenient().when(coaRepository.findById(arAccountId)).thenReturn(Optional.of(ar));
        lenient().when(coaRepository.findById(revenueAccountId)).thenReturn(Optional.of(revenue));
        lenient().when(coaRepository.findById(expenseAccountId)).thenReturn(Optional.of(expense));
    }

    @Test
    void getCashFlow_computesNetIncomeAndCashBalances() {
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 1, 31);

        JournalLine revenueLine = line(BigDecimal.ZERO, new BigDecimal("10000"));
        JournalLine expenseLine = line(new BigDecimal("3000"), BigDecimal.ZERO);
        JournalLine arLine = line(new BigDecimal("2000"), BigDecimal.ZERO);
        JournalLine cashBeginningLine = line(new BigDecimal("5000"), BigDecimal.ZERO);
        JournalLine cashEndLine = line(new BigDecimal("10000"), BigDecimal.ZERO);

        lenient().when(journalLineRepository.findByAccountIdAndDateRange(any(), any(), any()))
            .thenReturn(List.of());

        when(journalLineRepository.findByAccountIdAndDateRange(eq(revenueAccountId), eq(periodStart), eq(periodEnd)))
            .thenReturn(List.of(revenueLine));
        when(journalLineRepository.findByAccountIdAndDateRange(eq(expenseAccountId), eq(periodStart), eq(periodEnd)))
            .thenReturn(List.of(expenseLine));
        LocalDate dayBeforePeriod = periodStart.minusDays(1);

        when(journalLineRepository.findByAccountIdAndDateRange(eq(arAccountId), any(), eq(dayBeforePeriod)))
            .thenReturn(List.of());
        when(journalLineRepository.findByAccountIdAndDateRange(eq(arAccountId), any(), eq(periodEnd)))
            .thenReturn(List.of(arLine));
        JournalLine cashInPeriodLine = line(new BigDecimal("5000"), BigDecimal.ZERO);
        when(journalLineRepository.findByAccountIdAndDateRange(eq(cashAccountId), any(), any()))
            .thenAnswer(invocation -> {
                LocalDate end = invocation.getArgument(2);
                if (dayBeforePeriod.equals(end)) {
                    return List.of(cashBeginningLine);
                }
                if (periodEnd.equals(end)) {
                    return List.of(cashBeginningLine, cashInPeriodLine);
                }
                return List.of();
            });

        CashFlowResponse response = financialReportService.getCashFlow(organizationId, periodId);

        assertThat(response.getNetIncome()).isEqualByComparingTo(new BigDecimal("7000"));
        assertThat(response.getCashAtBeginning()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(response.getCashAtEnd()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getNetCashFlow()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(response.getOperatingAdjustments())
            .anyMatch(item -> item.getDescription().contains("Trade Debtors")
                && item.getAmount().compareTo(new BigDecimal("-2000")) == 0);
        assertThat(response.getNetCashFromOperations()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(response.getCashAtBeginning().add(response.getNetCashFlow()))
            .isEqualByComparingTo(response.getCashAtEnd());
    }

    @Test
    void getCashFlow_rejectsPeriodFromAnotherOrganization() {
        UUID otherOrg = UUID.randomUUID();
        assertThatThrownBy(() -> financialReportService.getCashFlow(otherOrg, periodId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("does not belong");
    }

    private ChartOfAccounts account(UUID id, String code, String name, String type, String category) {
        ChartOfAccounts account = new ChartOfAccounts();
        account.setId(id);
        account.setOrganizationId(organizationId);
        account.setAccountCode(code);
        account.setAccountName(name);
        account.setAccountType(type);
        account.setAccountCategory(category);
        account.setIsGroup(false);
        account.setOpeningBalance(BigDecimal.ZERO);
        return account;
    }

    private JournalLine line(BigDecimal debit, BigDecimal credit) {
        JournalEntry posted = new JournalEntry();
        posted.setId(entryId);
        posted.setStatus("POSTED");

        JournalLine line = new JournalLine();
        line.setJournalEntry(posted);
        line.setDebitAmount(debit);
        line.setCreditAmount(credit);
        return line;
    }
}
