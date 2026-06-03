package com.easyops.hospitalpharmacy.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispenseIdempotencyServiceTest {

    @Test
    void stableHash64_isDeterministic() {
        long a = DispenseIdempotencyService.stableHash64("POST_DISPENSE_LINES:" + "550e8400-e29b-41d4-a716-446655440000" + "\0" + "key-1");
        long b = DispenseIdempotencyService.stableHash64("POST_DISPENSE_LINES:" + "550e8400-e29b-41d4-a716-446655440000" + "\0" + "key-1");
        assertThat(a).isEqualTo(b);
    }
}
