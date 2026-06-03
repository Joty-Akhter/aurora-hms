package com.easyops.hospital.util;

/**
 * Split a single stored full name into first / middle / last for HL7, DICOM, and legacy APIs
 * that still expect separate components.
 */
public final class PatientNameInterop {

    private PatientNameInterop() {}

    /** Returns [firstName, middleName, lastName]. */
    public static String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", "", ""};
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return new String[] {parts[0], "", parts[0]};
        }
        if (parts.length == 2) {
            return new String[] {parts[0], "", parts[1]};
        }
        String first = parts[0];
        String last = parts[parts.length - 1];
        String middle = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
        return new String[] {first, middle, last};
    }
}
