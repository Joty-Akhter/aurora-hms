package com.easyops.ar.service;

import com.easyops.ar.client.BankGlAccountResolver;
import com.easyops.ar.client.JournalEntryPoster;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.ARInvoiceLine;
import com.easyops.ar.entity.ARReceipt;
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
    private static final UUID REVENUE_ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private JournalEntryPoster journalEntryPoster;
    @Mock
    private BankGlAccountResolver bankGlAccountResolver;

    @InjectMocks
    private GlJournalPostingService glJournalPostingService;

    @Test
    void postInvoiceJournal_debitsArAndCreditsRevenueAndTax() {
        when(journalEntryPoster.createAndPostJournal(any(), eq(ACTOR_ID))).thenReturn(JOURNAL_ID);

        ARInvoice invoice = buildInvoice();
        UUID result = glJournalPostingService.postInvoiceJournal(invoice, ACTOR_ID);

        assertThat(result).isEqualTo(JOURNAL_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(journalEntryPoster).createAndPostJournal(captor.capture(), eq(ACTOR_ID));

        Map<String, Object> entry = captor.getValue();
        assertThat(entry.get("journalType")).isEqualTo("AR_INVOICE");
        assertThat(entry.get("sourceModule")).isEqualTo("AR");
        assertThat(entry.get("sourceDocumentId")).isEqualTo(invoice.getId().toString());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) entry.get("lines");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).get("accountCode")).isEqualTo("1110");
        assertThat(amount(lines.get(0).get("debitAmount"))).isEqualByComparingTo("110");
        assertThat(lines.get(1).get("accountId")).isEqualTo(REVENUE_ACCOUNT_ID.toString());
        assertThat(amount(lines.get(1).get("creditAmount"))).isEqualByComparingTo("100");
        assertThat(lines.get(2).get("accountCode")).isEqualTo("2030");
        assertThat(amount(lines.get(2).get("creditAmount"))).isEqualByComparingTo("10");
    }

    @Test
    void postInvoiceJournal_rejectsUnbalancedTotals() {
        ARInvoice invoice = buildInvoice();
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("999"));

        assertThatThrownBy(() -> glJournalPostingService.postInvoiceJournal(invoice, ACTOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced");
    }

    @Test
    void postReceiptJournal_debitsBankAndCreditsAr() {
        when(journalEntryPoster.createAndPostJournal(any(), eq(ACTOR_ID))).thenReturn(JOURNAL_ID);

        ARReceipt receipt = new ARReceipt();
        receipt.setId(UUID.randomUUID());
        receipt.setOrganizationId(ORG_ID);
        receipt.setReceiptNumber("RCT-001");
        receipt.setReceiptDate(LocalDate.of(2026, 5, 20));
        receipt.setAmount(new BigDecimal("50.00"));

        UUID result = glJournalPostingService.postReceiptJournal(receipt, ACTOR_ID);

        assertThat(result).isEqualTo(JOURNAL_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(journalEntryPoster).createAndPostJournal(captor.capture(), eq(ACTOR_ID));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) captor.getValue().get("lines");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).get("accountCode")).isEqualTo("1030");
        assertThat(amount(lines.get(0).get("debitAmount"))).isEqualByComparingTo("50");
        assertThat(lines.get(1).get("accountCode")).isEqualTo("1110");
        assertThat(amount(lines.get(1).get("creditAmount"))).isEqualByComparingTo("50");
    }

    private static ARInvoice buildInvoice() {
        ARInvoiceLine line = new ARInvoiceLine();
        line.setDescription("Service");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setTaxPercent(new BigDecimal("10"));
        line.setLineTotal(new BigDecimal("110"));
        line.setAccountId(REVENUE_ACCOUNT_ID);

        ARInvoice invoice = new ARInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganizationId(ORG_ID);
        invoice.setInvoiceNumber("INV-001");
        invoice.setInvoiceDate(LocalDate.of(2026, 5, 15));
        invoice.setSubtotal(new BigDecimal("100"));
        invoice.setTaxAmount(new BigDecimal("10"));
        invoice.setTotalAmount(new BigDecimal("110"));
        invoice.setLines(List.of(line));
        return invoice;
    }

    private static BigDecimal amount(Object value) {
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }
}
