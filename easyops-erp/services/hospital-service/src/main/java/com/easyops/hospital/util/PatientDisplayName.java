package com.easyops.hospital.util;

import com.easyops.hospital.entity.Patient;

/** Single {@code full_name} field on {@link Patient}; use this for display and concatenated labels. */
public final class PatientDisplayName {

    private PatientDisplayName() {}

    public static String of(Patient patient) {
        if (patient == null || patient.getFullName() == null) {
            return "";
        }
        return patient.getFullName().trim();
    }
}
