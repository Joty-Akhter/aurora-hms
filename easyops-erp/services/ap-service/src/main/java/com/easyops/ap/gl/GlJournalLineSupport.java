package com.easyops.ap.gl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GlJournalLineSupport {

    private GlJournalLineSupport() {
    }

    public static void validateBalanced(List<Map<String, Object>> lines) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            debits = debits.add(amount(line.get("debitAmount")));
            credits = credits.add(amount(line.get("creditAmount")));
        }
        if (debits.compareTo(credits) != 0) {
            throw new IllegalArgumentException(
                    "Journal entry is not balanced. Debits: " + debits + ", Credits: " + credits);
        }
        if (debits.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Journal entry must have a non-zero amount");
        }
    }

    public static BigDecimal amount(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return switch (value) {
            case BigDecimal bd -> bd;
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            default -> new BigDecimal(value.toString());
        };
    }

    public static Map<String, Object> debitByCode(String accountCode, BigDecimal amount, String description) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountCode", accountCode);
        line.put("debitAmount", amount);
        line.put("creditAmount", BigDecimal.ZERO);
        line.put("description", description);
        return line;
    }

    public static Map<String, Object> creditByCode(String accountCode, BigDecimal amount, String description) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountCode", accountCode);
        line.put("debitAmount", BigDecimal.ZERO);
        line.put("creditAmount", amount);
        line.put("description", description);
        return line;
    }

    public static Map<String, Object> debitByAccountId(UUID accountId, BigDecimal amount, String description) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountId", accountId.toString());
        line.put("debitAmount", amount);
        line.put("creditAmount", BigDecimal.ZERO);
        line.put("description", description);
        return line;
    }

    public static Map<String, Object> creditByAccountId(UUID accountId, BigDecimal amount, String description) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountId", accountId.toString());
        line.put("debitAmount", BigDecimal.ZERO);
        line.put("creditAmount", amount);
        line.put("description", description);
        return line;
    }
}
