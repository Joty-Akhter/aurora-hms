package com.easyops.ap.service;

import com.easyops.ap.client.JournalEntryPoster;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.entity.APBillLine;
import com.easyops.ap.entity.APPayment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlJournalPostingServiceTest {

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID JOURNAL_ID = UUID.randomUUID();
    private static final UUID EXPENSE_ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private JournalEntryPoster journalEntryPoster;

    @InjectMocks
    private GlJournalPostingService glJournalPostingService;

    @Test
    void postBillJournal_debitsExpenseNetAndCreditsAp() {
        when(journalEntryPoster.createAndPostJournal(any(), eq(ACTOR_ID))).thenReturn(JOURNAL_ID);

        APBill bill = buildBill();
        UUID result = glJournalPostingService.postBillJournal(bill, ACTOR_ID);

        assertThat(result).isEqualTo(JOURNAL_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(journalEntryPoster).createAndPostJournal(captor.capture(), eq(ACTOR_ID));

        Map<String, Object> entry = captor.getValue();
        assertThat(entry.get("sourceModule")).isEqualTo("AP");
        assertThat(entry.get("sourceDocumentId")).isEqualTo(bill.getId().toString());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) entry.get("lines");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).get("accountId")).isEqualTo(EXPENSE_ACCOUNT_ID.toString());
        assertThat(amount(lines.get(0).get("debitAmount"))).isEqualByComparingTo("200");
        assertThat(lines.get(1).get("accountCode")).isEqualTo("5010");
        assertThat(amount(lines.get(1).get("debitAmount"))).isEqualByComparingTo("20");
        assertThat(lines.get(2).get("accountCode")).isEqualTo("2010");
        assertThat(amount(lines.get(2).get("creditAmount"))).isEqualByComparingTo("220");
    }

    @Test
    void postBillJournal_rejectsUnbalancedTotals() {
        APBill bill = buildBill();
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setTotalAmount(new BigDecimal("999"));

        assertThatThrownBy(() -> glJournalPostingService.postBillJournal(bill, ACTOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced");
    }

    @Test
    void postPaymentJournal_debitsApAndCreditsBank() {
        when(journalEntryPoster.createAndPostJournal(any(), eq(ACTOR_ID))).thenReturn(JOURNAL_ID);

        APPayment payment = new APPayment();
        payment.setId(UUID.randomUUID());
        payment.setOrganizationId(ORG_ID);
        payment.setPaymentNumber("PAY-001");
        payment.setPaymentDate(LocalDate.of(2026, 5, 20));
        payment.setAmount(new BigDecimal("250.00"));

        UUID result = glJournalPostingService.postPaymentJournal(payment, ACTOR_ID);

        assertThat(result).isEqualTo(JOURNAL_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(journalEntryPoster).createAndPostJournal(captor.capture(), eq(ACTOR_ID));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) captor.getValue().get("lines");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).get("accountCode")).isEqualTo("2010");
        assertThat(amount(lines.get(0).get("debitAmount"))).isEqualByComparingTo("250");
        assertThat(lines.get(1).get("accountCode")).isEqualTo("1030");
        assertThat(amount(lines.get(1).get("creditAmount"))).isEqualByComparingTo("250");
    }

    private static APBill buildBill() {
        APBillLine line = new APBillLine();
        line.setDescription("Supplies");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("200"));
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setTaxPercent(new BigDecimal("10"));
        line.setLineTotal(new BigDecimal("220"));
        line.setAccountId(EXPENSE_ACCOUNT_ID);

        APBill bill = new APBill();
        bill.setId(UUID.randomUUID());
        bill.setOrganizationId(ORG_ID);
        bill.setBillNumber("BILL-001");
        bill.setBillDate(LocalDate.of(2026, 5, 15));
        bill.setSubtotal(new BigDecimal("200"));
        bill.setTaxAmount(new BigDecimal("20"));
        bill.setTotalAmount(new BigDecimal("220"));
        bill.setLines(List.of(line));
        return bill;
    }

    private static BigDecimal amount(Object value) {
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }
}
