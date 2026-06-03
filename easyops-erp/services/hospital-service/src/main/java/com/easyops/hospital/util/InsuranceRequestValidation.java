package com.easyops.hospital.util;

import com.easyops.hospital.dto.request.InsuranceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Shared validation for insurance create/update (REST and embedded patient payloads).
 */
public final class InsuranceRequestValidation {

    private InsuranceRequestValidation() {
    }

    /**
     * When both dates are present, expiration must not precede effective date.
     */
    public static void assertEffectiveBeforeExpiration(InsuranceRequest request) {
        LocalDate effective = request.getEffectiveDate();
        LocalDate expiration = request.getExpirationDate();
        if (effective != null && expiration != null && expiration.isBefore(effective)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expiration date cannot be before the effective date.");
        }
    }
}
