package com.easyops.ar.gl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlJournalLineSupportTest {

    @Test
    void validateBalanced_rejectsMismatch() {
        List<Map<String, Object>> lines = List.of(
                GlJournalLineSupport.debitByCode("1110", new BigDecimal("100"), "dr"),
                GlJournalLineSupport.creditByCode("4010", new BigDecimal("90"), "cr")
        );

        assertThatThrownBy(() -> GlJournalLineSupport.validateBalanced(lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced");
    }
}
