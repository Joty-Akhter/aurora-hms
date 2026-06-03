package com.easyops.accounting.service;

import com.easyops.accounting.config.AccountingValidationProperties;
import com.easyops.accounting.dto.JournalEntryRequest;
import com.easyops.accounting.dto.JournalLineRequest;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.entity.JournalLine;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.repository.JournalEntryRepository;
import com.easyops.accounting.repository.JournalLineRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalPostingServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID PERIOD_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");
    private static final UUID JOURNAL_ID = UUID.fromString("44444444-5555-6666-7777-888888888888");
    private static final UUID REVERSAL_ID = UUID.fromString("55555555-6666-7777-8888-999999999999");

    @Mock
    private JournalEntryRepository journalRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @Mock
    private PeriodService periodService;

    @Mock
    private AccountingValidationProperties validationProperties;

    @InjectMocks
    private JournalPostingService journalPostingService;

    @BeforeEach
    void setUp() {
        when(validationProperties.isAllowBackdated()).thenReturn(false);
    }

    @Test
    void createJournalEntry_rejectsBackdatedDateWhenNotAllowed() {
        JournalEntryRequest request = balancedRequest(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> journalPostingService.createJournalEntry(request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Backdated journal entries are not allowed");
    }

    @Test
    void createJournalEntry_allowsBackdatedDateForSubledgerSource() {
        JournalEntryRequest request = balancedRequest(LocalDate.now().minusDays(5));
        request.setJournalType("AR_INVOICE");
        request.setSourceModule("AR");

        when(periodService.getPeriodForDate(any(), any())).thenReturn(openPeriod());
        when(journalRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry entry = inv.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });

        assertThat(journalPostingService.createJournalEntry(request, USER_ID)).isNotNull();
    }

    @Test
    void reverseJournalEntry_postsReversalThroughStandardPostPath() {
        JournalEntry original = postedJournal();
        JournalLine debitLine = line(original, 1, BigDecimal.TEN, BigDecimal.ZERO);
        JournalLine creditLine = line(original, 2, BigDecimal.ZERO, BigDecimal.TEN);

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(original));
        when(journalRepository.findById(REVERSAL_ID)).thenAnswer(inv -> {
            JournalEntry draft = postedJournal();
            draft.setId(REVERSAL_ID);
            draft.setStatus("DRAFT");
            draft.setJournalNumber("JV000002");
            return Optional.of(draft);
        });
        when(journalLineRepository.findByJournalEntry_IdOrderByLineNumber(JOURNAL_ID))
                .thenReturn(List.of(debitLine, creditLine));
        when(journalRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry entry = inv.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(REVERSAL_ID);
            }
            return entry;
        });
        when(periodService.getPeriodById(PERIOD_ID)).thenReturn(openPeriod());

        JournalEntry result = journalPostingService.reverseJournalEntry(JOURNAL_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo("POSTED");
        assertThat(original.getStatus()).isEqualTo("REVERSED");
        verify(journalRepository, org.mockito.Mockito.atLeast(2)).save(any(JournalEntry.class));
    }

    private static JournalEntry postedJournal() {
        JournalEntry entry = new JournalEntry();
        entry.setId(JOURNAL_ID);
        entry.setOrganizationId(ORG_ID);
        entry.setJournalNumber("JV000001");
        entry.setJournalDate(LocalDate.now());
        entry.setPeriodId(PERIOD_ID);
        entry.setStatus("POSTED");
        entry.setTotalDebit(BigDecimal.TEN);
        entry.setTotalCredit(BigDecimal.TEN);
        return entry;
    }

    private static JournalLine line(JournalEntry entry, int lineNo, BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setLineNumber(lineNo);
        line.setAccountId(UUID.randomUUID());
        line.setDebitAmount(debit);
        line.setCreditAmount(credit);
        return line;
    }

    private static Period openPeriod() {
        Period period = new Period();
        period.setId(PERIOD_ID);
        period.setStatus("OPEN");
        return period;
    }

    private static JournalEntryRequest balancedRequest(LocalDate journalDate) {
        JournalLineRequest debit = new JournalLineRequest();
        debit.setAccountId(UUID.randomUUID());
        debit.setDebitAmount(BigDecimal.TEN);
        debit.setCreditAmount(BigDecimal.ZERO);

        JournalLineRequest credit = new JournalLineRequest();
        credit.setAccountId(UUID.randomUUID());
        credit.setDebitAmount(BigDecimal.ZERO);
        credit.setCreditAmount(BigDecimal.TEN);

        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganizationId(ORG_ID);
        request.setJournalDate(journalDate);
        request.setJournalType("MANUAL");
        request.setLines(List.of(debit, credit));
        return request;
    }
}
