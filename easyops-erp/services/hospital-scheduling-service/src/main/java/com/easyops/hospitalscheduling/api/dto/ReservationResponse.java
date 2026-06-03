package com.easyops.hospitalscheduling.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReservationResponse {

    private UUID id;
    private UUID resourceId;
    private OffsetDateTime slotStart;
    private OffsetDateTime slotEnd;
    private String status;
    private String referenceType;
    private String referenceId;
    private UUID patientId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(OffsetDateTime slotStart) { this.slotStart = slotStart; }
    public OffsetDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(OffsetDateTime slotEnd) { this.slotEnd = slotEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
