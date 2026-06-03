package com.easyops.hospital.util;

/**
 * Normalizes phone numbers for duplicate detection and storage consistency (digits only).
 */
public final class PatientPhoneNormalization {

    /** Minimum digit length for uniqueness checks (avoids trivial collisions). */
    public static final int MIN_DIGITS_FOR_UNIQUENESS = 6;

    private PatientPhoneNormalization() {
    }

    public static String normalizeDigits(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }

    public static String normalizeForStorage(String phone) {
        return normalizeDigits(phone);
    }

    public static boolean isEligibleForUniquenessCheck(String phone) {
        return normalizeDigits(phone).length() >= MIN_DIGITS_FOR_UNIQUENESS;
    }
}
