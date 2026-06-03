package com.easyops.hospital.readmodel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Minimal read-only projection for encounters (visits),
 * to make it easier to depend on an abstract interface.
 */
public interface EncounterReadModel {

    UUID getEncounterId();

    UUID getPatientId();

    String getEncounterNumber();

    String getEncounterType();

    String getStatus();

    LocalDate getStartDate();

    LocalDate getEndDate();
}

