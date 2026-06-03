package com.easyops.hr.service;

import com.easyops.hr.entity.LoanInterestMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanScheduleBuilderTest {

    @Test
    void none_equalPrincipal_sumsToPrincipal() {
        List<BigDecimal> a = LoanScheduleBuilder.monthlyScheduledAmounts(
                new BigDecimal("100.00"), 4, LoanInterestMethod.NONE, null);
        assertEquals(4, a.size());
        assertEquals(new BigDecimal("100.00"), a.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void flat_includesInterestInTotal() {
        List<BigDecimal> a = LoanScheduleBuilder.monthlyScheduledAmounts(
                new BigDecimal("1200.00"),
                12,
                LoanInterestMethod.FLAT,
                new BigDecimal("12"));
        assertEquals(12, a.size());
        BigDecimal sum = a.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // total interest = 1200 * 0.12 * 1 = 144; total = 1344; monthly ~ 112
        assertEquals(new BigDecimal("1344.00"), sum);
    }

    @Test
    void reducing_balance_paymentsPositiveAndExceedPrincipal() {
        List<BigDecimal> a = LoanScheduleBuilder.monthlyScheduledAmounts(
                new BigDecimal("10000.00"),
                12,
                LoanInterestMethod.REDUCING_BALANCE,
                new BigDecimal("12"));
        assertEquals(12, a.size());
        for (BigDecimal p : a) {
            assertTrue(p.compareTo(BigDecimal.ZERO) > 0);
        }
        BigDecimal sum = a.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(sum.compareTo(new BigDecimal("10000.00")) > 0);
    }
}
