package com.easyops.ar.service;

import com.easyops.ar.client.BankGlAccountResolver;
import com.easyops.ar.client.JournalEntryPoster;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.entity.ARInvoiceLine;
import com.easyops.ar.entity.ARCreditNote;
import com.easyops.ar.entity.ARCreditNoteLine;
import com.easyops.ar.entity.ARReceipt;
import com.easyops.ar.gl.GlJournalLineSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds and posts GL journal entries for AR subledger documents.
 */
@Service
@RequiredArgsConstructor
public class GlJournalPostingService {

    static final String AR_CONTROL = "1110";
    static final String TAX_PAYABLE = "2030";
    static final String BANK_DEFAULT = "1030";
    static final String REVENUE_DEFAULT = "4010";
    static final String SOURCE_MODULE = "AR";

    private final JournalEntryPoster journalEntryPoster;
    private final BankGlAccountResolver bankGlAccountResolver;

    public UUID postInvoiceJournal(ARInvoice invoice, UUID actorUserId) {
        requireLines(invoice.getLines(), "invoice");

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(GlJournalLineSupport.debitByCode(AR_CONTROL, invoice.getTotalAmount(),
                "AR Invoice " + invoice.getInvoiceNumber()));
        addRevenueCredits(lines, invoice.getLines());
        addTaxCredit(lines, invoice.getTaxAmount(), "Tax on invoice " + invoice.getInvoiceNumber());

        return post(invoice.getOrganizationId(), invoice.getInvoiceDate(), "AR_INVOICE",
                "AR-" + invoice.getInvoiceNumber(),
                "AR Invoice " + invoice.getInvoiceNumber(),
                invoice.getId(), lines, actorUserId);
    }

    public UUID postReceiptJournal(ARReceipt receipt, UUID actorUserId) {
        if (receipt.getAmount() == null || receipt.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Receipt amount must be greater than zero");
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(debitBankLine(receipt, actorUserId));
        lines.add(GlJournalLineSupport.creditByCode(AR_CONTROL, receipt.getAmount(),
                "AR Receipt " + receipt.getReceiptNumber()));

        return post(receipt.getOrganizationId(), receipt.getReceiptDate(), "AR_RECEIPT",
                "AR-RCT-" + receipt.getReceiptNumber(),
                "AR Receipt " + receipt.getReceiptNumber(),
                receipt.getId(), lines, actorUserId);
    }

    public UUID postCreditNoteJournal(ARCreditNote creditNote, UUID actorUserId) {
        requireLines(creditNote.getLines(), "credit note");

        List<Map<String, Object>> lines = new ArrayList<>();
        addRevenueDebits(lines, creditNote.getLines());
        addTaxDebit(lines, creditNote.getTaxAmount(), "Tax reversal on credit note " + creditNote.getCreditNoteNumber());
        lines.add(GlJournalLineSupport.creditByCode(AR_CONTROL, creditNote.getTotalAmount(),
                "AR Credit Note " + creditNote.getCreditNoteNumber()));

        return post(creditNote.getOrganizationId(), creditNote.getCreditNoteDate(), "AR_CREDIT_NOTE",
                "AR-CN-" + creditNote.getCreditNoteNumber(),
                "AR Credit Note " + creditNote.getCreditNoteNumber(),
                creditNote.getId(), lines, actorUserId);
    }

    private void addRevenueCredits(List<Map<String, Object>> lines, List<ARInvoiceLine> invoiceLines) {
        for (ARInvoiceLine line : invoiceLines) {
            BigDecimal net = lineNetBeforeTax(line);
            if (net.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (line.getAccountId() != null) {
                lines.add(GlJournalLineSupport.creditByAccountId(line.getAccountId(), net, line.getDescription()));
            } else {
                lines.add(GlJournalLineSupport.creditByCode(REVENUE_DEFAULT, net, line.getDescription()));
            }
        }
    }

    private void addRevenueDebits(List<Map<String, Object>> lines, List<ARCreditNoteLine> creditLines) {
        for (ARCreditNoteLine line : creditLines) {
            BigDecimal net = lineNetBeforeTax(line);
            if (net.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (line.getAccountId() != null) {
                lines.add(GlJournalLineSupport.debitByAccountId(line.getAccountId(), net, line.getDescription()));
            } else {
                lines.add(GlJournalLineSupport.debitByCode(REVENUE_DEFAULT, net, line.getDescription()));
            }
        }
    }

    private void addTaxCredit(List<Map<String, Object>> lines, BigDecimal taxAmount, String description) {
        if (taxAmount != null && taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(GlJournalLineSupport.creditByCode(TAX_PAYABLE, taxAmount, description));
        }
    }

    private void addTaxDebit(List<Map<String, Object>> lines, BigDecimal taxAmount, String description) {
        if (taxAmount != null && taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(GlJournalLineSupport.debitByCode(TAX_PAYABLE, taxAmount, description));
        }
    }

    private Map<String, Object> debitBankLine(ARReceipt receipt, UUID actorUserId) {
        if (receipt.getBankAccountId() != null) {
            UUID glAccountId = bankGlAccountResolver.resolveGlAccountId(receipt.getBankAccountId(), actorUserId);
            if (glAccountId != null) {
                return GlJournalLineSupport.debitByAccountId(glAccountId, receipt.getAmount(),
                        "Bank receipt " + receipt.getReceiptNumber());
            }
        }
        return GlJournalLineSupport.debitByCode(BANK_DEFAULT, receipt.getAmount(),
                "Bank receipt " + receipt.getReceiptNumber());
    }

    private UUID post(UUID organizationId, java.time.LocalDate entryDate, String journalType,
                      String referenceId, String description, UUID sourceDocumentId,
                      List<Map<String, Object>> lines, UUID actorUserId) {
        GlJournalLineSupport.validateBalanced(lines);

        Map<String, Object> journalEntry = new HashMap<>();
        journalEntry.put("organizationId", organizationId.toString());
        journalEntry.put("entryDate", entryDate.toString());
        journalEntry.put("journalType", journalType);
        journalEntry.put("referenceId", referenceId);
        journalEntry.put("description", description);
        journalEntry.put("sourceModule", SOURCE_MODULE);
        journalEntry.put("sourceDocumentId", sourceDocumentId.toString());
        journalEntry.put("status", "POSTED");
        journalEntry.put("lines", lines);
        return journalEntryPoster.createAndPostJournal(journalEntry, actorUserId);
    }

    static BigDecimal lineNetBeforeTax(ARInvoiceLine line) {
        return lineNetBeforeTax(line.getQuantity(), line.getUnitPrice(), line.getDiscountPercent());
    }

    static BigDecimal lineNetBeforeTax(ARCreditNoteLine line) {
        return lineNetBeforeTax(line.getQuantity(), line.getUnitPrice(), line.getDiscountPercent());
    }

    private static BigDecimal lineNetBeforeTax(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal discount = discountPercent != null ? discountPercent : BigDecimal.ZERO;

        BigDecimal lineSubtotal = qty.multiply(price).setScale(4, RoundingMode.HALF_UP);
        BigDecimal discountAmount = lineSubtotal
                .multiply(discount)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return lineSubtotal.subtract(discountAmount);
    }

    private static void requireLines(List<?> lines, String documentType) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot post " + documentType + " without line items");
        }
    }
}
