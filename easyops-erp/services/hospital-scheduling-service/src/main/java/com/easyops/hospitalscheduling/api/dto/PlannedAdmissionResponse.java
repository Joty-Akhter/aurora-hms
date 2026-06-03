package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PlannedAdmissionResponse {

    private UUID id;
    private UUID patientId;
    private LocalDate preferredDate;
    private String preferredWardOrBedClass;
    private String status;
    private UUID bedGroupResourceId;
    private UUID reservationId;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getPreferredWardOrBedClass() { return preferredWardOrBedClass; }
    public void setPreferredWardOrBedClass(String preferredWardOrBedClass) { this.preferredWardOrBedClass = preferredWardOrBedClass; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getBedGroupResourceId() { return bedGroupResourceId; }
    public void setBedGroupResourceId(UUID bedGroupResourceId) { this.bedGroupResourceId = bedGroupResourceId; }
    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
