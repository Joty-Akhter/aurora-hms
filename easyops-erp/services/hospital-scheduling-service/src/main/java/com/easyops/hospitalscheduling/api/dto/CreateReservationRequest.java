package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CreateReservationRequest {

    @NotNull
    private UUID resourceId;

    @NotNull
    private OffsetDateTime slotStart;

    @NotNull
    private OffsetDateTime slotEnd;

    @Size(max = 30)
    private String status;

    @Size(max = 30)
    private String referenceType;

    @Size(max = 255)
    private String referenceId;

    private UUID patientId;

    @Size(max = 255)
    private String idempotencyKey;

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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
