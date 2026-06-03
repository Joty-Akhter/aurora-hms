package com.easyops.manufacturing.integration;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingIntegrationServiceTest {

    @Test
    void isBalanced_acceptsBalancedMaterialIssueLines() {
        List<Map<String, Object>> lines = List.of(
                line("1142", "100.00", "0"),
                line("1141", "0", "100.00")
        );
        assertThat(AccountingIntegrationService.isBalanced(lines)).isTrue();
    }

    @Test
    void isBalanced_rejectsUnbalancedWipLines() {
        List<Map<String, Object>> lines = List.of(
                line("1142", "100.00", "0"),
                line("1141", "0", "50.00")
        );
        assertThat(AccountingIntegrationService.isBalanced(lines)).isFalse();
    }

    private static Map<String, Object> line(String code, String debit, String credit) {
        return Map.of(
                "accountCode", code,
                "debitAmount", new BigDecimal(debit),
                "creditAmount", new BigDecimal(credit)
        );
    }
}
