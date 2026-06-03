package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CheckConflictsRequest {

    @NotNull
    private UUID resourceId;

    @NotNull
    private OffsetDateTime slotStart;

    @NotNull
    private OffsetDateTime slotEnd;

    private UUID excludeReservationId;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(OffsetDateTime slotStart) { this.slotStart = slotStart; }
    public OffsetDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(OffsetDateTime slotEnd) { this.slotEnd = slotEnd; }
    public UUID getExcludeReservationId() { return excludeReservationId; }
    public void setExcludeReservationId(UUID excludeReservationId) { this.excludeReservationId = excludeReservationId; }
}
