package com.easyops.hospital.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionDerivedQuantityTest {

    @Test
    void deriveUnits_shorhandTimesDuration() {
        assertThat(PrescriptionDerivedQuantity.deriveUnits("1+0+1", 5))
                .isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(PrescriptionDerivedQuantity.deriveUnits("1+0+0", 30))
                .isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void deriveUnits_prnOrStat_returnsOne() {
        assertThat(PrescriptionDerivedQuantity.deriveUnits("As needed (PRN)", 3))
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(PrescriptionDerivedQuantity.deriveUnits("STAT dose", 1))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void deriveUnits_missingDuration_returnsOne() {
        assertThat(PrescriptionDerivedQuantity.deriveUnits("1+0+1", null))
                .isEqualByComparingTo(BigDecimal.ONE);
    }
}
