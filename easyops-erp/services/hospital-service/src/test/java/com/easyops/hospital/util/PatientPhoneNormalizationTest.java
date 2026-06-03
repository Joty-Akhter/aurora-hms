package com.easyops.hospital.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatientPhoneNormalizationTest {

    @Test
    void normalizeDigits_stripsFormatting() {
        assertThat(PatientPhoneNormalization.normalizeDigits("+880 1712-345678")).isEqualTo("8801712345678");
        assertThat(PatientPhoneNormalization.normalizeDigits("01712345678")).isEqualTo("01712345678");
    }

    @Test
    void isEligibleForUniquenessCheck_requiresMinimumDigits() {
        assertThat(PatientPhoneNormalization.isEligibleForUniquenessCheck("12345")).isFalse();
        assertThat(PatientPhoneNormalization.isEligibleForUniquenessCheck("123456")).isTrue();
    }
}
