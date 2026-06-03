package com.easyops.hospital.exception;

import lombok.Getter;

/**
 * HTTP 422 — semantic / business-rule violations that are not generic 400 validation errors
 * (e.g. FR-P1.4a diagnosis limits, FR-P1.10 Schedule II refill prohibition).
 */
@Getter
public class UnprocessableEntityException extends RuntimeException {

    /** Machine-readable code for API clients (e.g. {@code DIAGNOSIS_LIMIT_EXCEEDED}); may be null. */
    private final String code;

    public UnprocessableEntityException(String message) {
        this(message, null);
    }

    public UnprocessableEntityException(String message, String code) {
        super(message);
        this.code = code;
    }
}
