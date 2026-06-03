package com.easyops.ar.service;

import com.easyops.ar.entity.ARInvoiceLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GlJournalLineMathTest {

    @Test
    void lineNetBeforeTax_excludesTaxFromRevenueCredit() {
        ARInvoiceLine line = new ARInvoiceLine();
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setTaxPercent(new BigDecimal("10"));

        assertThat(GlJournalPostingService.lineNetBeforeTax(line)).isEqualByComparingTo("100");
    }
}
