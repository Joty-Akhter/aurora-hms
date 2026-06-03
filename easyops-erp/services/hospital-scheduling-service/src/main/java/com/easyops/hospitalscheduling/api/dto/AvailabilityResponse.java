package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AvailabilityResponse {

    private UUID resourceId;
    private LocalDate date;
    private List<SlotAvailabilityDto> slots;
    private boolean blackedOut;

    public AvailabilityResponse() {}

    public AvailabilityResponse(UUID resourceId, LocalDate date, List<SlotAvailabilityDto> slots, boolean blackedOut) {
        this.resourceId = resourceId;
        this.date = date;
        this.slots = slots;
        this.blackedOut = blackedOut;
    }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<SlotAvailabilityDto> getSlots() { return slots; }
    public void setSlots(List<SlotAvailabilityDto> slots) { this.slots = slots; }
    public boolean isBlackedOut() { return blackedOut; }
    public void setBlackedOut(boolean blackedOut) { this.blackedOut = blackedOut; }
}
