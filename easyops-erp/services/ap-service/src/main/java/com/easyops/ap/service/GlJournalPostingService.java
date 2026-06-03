package com.easyops.ap.service;

import com.easyops.ap.client.BankGlAccountResolver;
import com.easyops.ap.client.JournalEntryPoster;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.entity.APBillLine;
import com.easyops.ap.entity.APPayment;
import com.easyops.ap.gl.GlJournalLineSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlJournalPostingService {

    static final String AP_CONTROL = "2010";
    static final String BANK_DEFAULT = "1030";
    static final String EXPENSE_DEFAULT = "5010";
    static final String SOURCE_MODULE = "AP";

    private final JournalEntryPoster journalEntryPoster;
    private final BankGlAccountResolver bankGlAccountResolver;

    public UUID postBillJournal(APBill bill, UUID actorUserId) {
        requireLines(bill.getLines(), "bill");

        List<Map<String, Object>> lines = new ArrayList<>();
        for (APBillLine line : bill.getLines()) {
            BigDecimal net = lineNetBeforeTax(line);
            if (net.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (line.getAccountId() != null) {
                lines.add(GlJournalLineSupport.debitByAccountId(line.getAccountId(), net, line.getDescription()));
            } else {
                lines.add(GlJournalLineSupport.debitByCode(EXPENSE_DEFAULT, net, line.getDescription()));
            }
        }
        if (bill.getTaxAmount() != null && bill.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(GlJournalLineSupport.debitByCode(EXPENSE_DEFAULT, bill.getTaxAmount(),
                    "Purchase tax on bill " + bill.getBillNumber()));
        }
        lines.add(GlJournalLineSupport.creditByCode(AP_CONTROL, bill.getTotalAmount(),
                "AP Bill " + bill.getBillNumber()));

        return post(bill.getOrganizationId(), bill.getBillDate(), "AP_BILL",
                "AP-" + bill.getBillNumber(),
                "AP Bill " + bill.getBillNumber(),
                bill.getId(), lines, actorUserId);
    }

    public UUID postPaymentJournal(APPayment payment, UUID actorUserId) {
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(GlJournalLineSupport.debitByCode(AP_CONTROL, payment.getAmount(),
                "AP Payment " + payment.getPaymentNumber()));
        lines.add(creditBankLine(payment, actorUserId));

        return post(payment.getOrganizationId(), payment.getPaymentDate(), "AP_PAYMENT",
                "AP-PAY-" + payment.getPaymentNumber(),
                "AP Payment " + payment.getPaymentNumber(),
                payment.getId(), lines, actorUserId);
    }

    private Map<String, Object> creditBankLine(APPayment payment, UUID actorUserId) {
        if (payment.getBankAccountId() != null) {
            UUID glAccountId = bankGlAccountResolver.resolveGlAccountId(payment.getBankAccountId(), actorUserId);
            if (glAccountId != null) {
                return GlJournalLineSupport.creditByAccountId(glAccountId, payment.getAmount(),
                        "Bank payment " + payment.getPaymentNumber());
            }
        }
        return GlJournalLineSupport.creditByCode(BANK_DEFAULT, payment.getAmount(),
                "Bank payment " + payment.getPaymentNumber());
    }

    private UUID post(UUID organizationId, LocalDate entryDate, String journalType,
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

    static BigDecimal lineNetBeforeTax(APBillLine line) {
        BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ONE;
        BigDecimal price = line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal discount = line.getDiscountPercent() != null ? line.getDiscountPercent() : BigDecimal.ZERO;

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
