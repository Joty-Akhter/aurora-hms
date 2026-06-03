package com.easyops.hospitalpharmacy.entity;

/**
 * Stored in {@code dispense_orders.external_validation_status} (Phase P3 — WS-E).
 */
public enum ExternalValidationStatus {
    NOT_REQUIRED,
    PENDING,
    VERIFIED,
    FAILED_SOFT;

    public static ExternalValidationStatus fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NOT_REQUIRED;
        }
        String n = raw.trim().toUpperCase().replace('-', '_');
        try {
            return ExternalValidationStatus.valueOf(n);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid externalValidationStatus: " + raw);
        }
    }
}
