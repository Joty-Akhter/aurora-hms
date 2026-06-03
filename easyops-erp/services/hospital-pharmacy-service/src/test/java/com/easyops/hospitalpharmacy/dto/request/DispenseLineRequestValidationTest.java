package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link DispenseLineRequest} constraints (e.g. {@code @Size} on stock override) are enforced
 * when the controller uses {@code List<@Valid DispenseLineRequest>} (Phase P1).
 */
class DispenseLineRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void stockOverrideReason_within2000_ok() {
        DispenseLineRequest r = validBase();
        r.setStockOverrideReason("x".repeat(2000));
        Set<ConstraintViolation<DispenseLineRequest>> v = validator.validate(r);
        assertThat(v).isEmpty();
    }

    @Test
    void stockOverrideReason_over2000_invalid() {
        DispenseLineRequest r = validBase();
        r.setStockOverrideReason("x".repeat(2001));
        Set<ConstraintViolation<DispenseLineRequest>> v = validator.validate(r);
        assertThat(v).isNotEmpty();
    }

    private static DispenseLineRequest validBase() {
        DispenseLineRequest r = new DispenseLineRequest();
        r.setDrugId(UUID.randomUUID());
        r.setQuantityDispensed(BigDecimal.ONE);
        return r;
    }
}
