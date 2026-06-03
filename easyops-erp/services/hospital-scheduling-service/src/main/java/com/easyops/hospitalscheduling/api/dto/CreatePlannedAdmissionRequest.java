package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class CreatePlannedAdmissionRequest {

    @NotNull
    private UUID patientId;

    @NotNull
    private LocalDate preferredDate;

    @Size(max = 100)
    private String preferredWardOrBedClass;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getPreferredWardOrBedClass() { return preferredWardOrBedClass; }
    public void setPreferredWardOrBedClass(String preferredWardOrBedClass) { this.preferredWardOrBedClass = preferredWardOrBedClass; }
}
