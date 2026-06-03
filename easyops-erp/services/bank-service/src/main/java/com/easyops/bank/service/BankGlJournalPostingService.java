package com.easyops.bank.service;

import com.easyops.bank.client.AccountingJournalClient;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.entity.BankTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankGlJournalPostingService {

    private static final String SOURCE_MODULE = "BANK";

    private final AccountingJournalClient accountingJournalClient;

    public UUID postTransactionJournal(BankTransaction transaction, BankAccount bankAccount,
                                       UUID offsetGlAccountId, UUID actorUserId) {
        if (bankAccount.getGlAccountId() == null) {
            throw new IllegalArgumentException(
                    "Bank account must be linked to a GL account before posting to the general ledger");
        }
        if (offsetGlAccountId == null) {
            throw new IllegalArgumentException(
                    "Offset GL account is required to post a bank transaction to the general ledger");
        }

        BigDecimal debit = transaction.getDebitAmount() != null ? transaction.getDebitAmount() : BigDecimal.ZERO;
        BigDecimal credit = transaction.getCreditAmount() != null ? transaction.getCreditAmount() : BigDecimal.ZERO;
        if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Transaction must have a debit or credit amount");
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        if (credit.compareTo(BigDecimal.ZERO) > 0) {
            // Deposit: increase bank asset
            lines.add(line(bankAccount.getGlAccountId(), credit, BigDecimal.ZERO, "Bank deposit"));
            lines.add(line(offsetGlAccountId, BigDecimal.ZERO, credit, transaction.getDescription()));
        } else {
            // Withdrawal: decrease bank asset
            lines.add(line(offsetGlAccountId, debit, BigDecimal.ZERO, transaction.getDescription()));
            lines.add(line(bankAccount.getGlAccountId(), BigDecimal.ZERO, debit, "Bank withdrawal"));
        }
        validateBalanced(lines);

        Map<String, Object> journal = new HashMap<>();
        journal.put("organizationId", bankAccount.getOrganizationId().toString());
        journal.put("entryDate", transaction.getTransactionDate().toString());
        journal.put("journalType", "BANK_" + transaction.getTransactionType());
        journal.put("referenceId", "BANK-" + transaction.getId());
        journal.put("description", transaction.getDescription() != null
                ? transaction.getDescription()
                : "Bank " + transaction.getTransactionType());
        journal.put("sourceModule", SOURCE_MODULE);
        journal.put("sourceDocumentId", transaction.getId().toString());
        journal.put("status", "POSTED");
        journal.put("lines", lines);

        return accountingJournalClient.createAndPostJournal(journal, actorUserId);
    }

    private static Map<String, Object> line(UUID accountId, BigDecimal debit, BigDecimal credit, String description) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountId", accountId.toString());
        line.put("debitAmount", debit);
        line.put("creditAmount", credit);
        line.put("description", description);
        return line;
    }

    private static void validateBalanced(List<Map<String, Object>> lines) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            debits = debits.add(toBigDecimal(line.get("debitAmount")));
            credits = credits.add(toBigDecimal(line.get("creditAmount")));
        }
        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException("Bank journal is not balanced");
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
