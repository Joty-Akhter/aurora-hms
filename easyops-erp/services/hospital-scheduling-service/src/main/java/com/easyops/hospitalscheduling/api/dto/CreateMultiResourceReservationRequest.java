package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request to create one reservation per resource for the same slot and reference.
 * All resources must be free for the slot; otherwise the whole operation fails.
 * Phase 2 appointments use a single resource (doctor); Phase 4 can add e.g. room required
 * and create two reservations (doctor + room) with the same referenceType/referenceId.
 */
public class CreateMultiResourceReservationRequest {

    @NotEmpty(message = "at least one resourceId required")
    private List<UUID> resourceIds;

    @NotNull
    private OffsetDateTime slotStart;

    @NotNull
    private OffsetDateTime slotEnd;

    @Size(max = 30)
    private String referenceType;

    @Size(max = 255)
    private String referenceId;

    private UUID patientId;

    @Size(max = 255)
    private String idempotencyKey;

    public List<UUID> getResourceIds() { return resourceIds; }
    public void setResourceIds(List<UUID> resourceIds) { this.resourceIds = resourceIds; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(OffsetDateTime slotStart) { this.slotStart = slotStart; }
    public OffsetDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(OffsetDateTime slotEnd) { this.slotEnd = slotEnd; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
