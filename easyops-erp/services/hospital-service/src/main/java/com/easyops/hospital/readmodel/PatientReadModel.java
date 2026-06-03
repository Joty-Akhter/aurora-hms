package com.easyops.hospital.readmodel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Minimal read-only projection for patient identity and demographics,
 * to decouple consumers from the JPA entity.
 */
public interface PatientReadModel {

    UUID getPatientId();

    String getMrn();

    String getFullName();

    LocalDate getDateOfBirth();

    String getGender();
}

