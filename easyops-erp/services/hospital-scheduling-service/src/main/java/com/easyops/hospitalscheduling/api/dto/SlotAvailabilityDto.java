package com.easyops.hospitalscheduling.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SlotAvailabilityDto {

    private OffsetDateTime start;
    private OffsetDateTime end;
    private int availableCount;
    /** When set, this slot is served by the substitute resource (Phase 4 SUBSTITUTE swap). */
    private UUID substituteResourceId;

    public SlotAvailabilityDto() {}

    public SlotAvailabilityDto(OffsetDateTime start, OffsetDateTime end, int availableCount) {
        this.start = start;
        this.end = end;
        this.availableCount = availableCount;
    }

    public OffsetDateTime getStart() { return start; }
    public void setStart(OffsetDateTime start) { this.start = start; }
    public OffsetDateTime getEnd() { return end; }
    public void setEnd(OffsetDateTime end) { this.end = end; }
    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }
    public UUID getSubstituteResourceId() { return substituteResourceId; }
    public void setSubstituteResourceId(UUID substituteResourceId) { this.substituteResourceId = substituteResourceId; }
}
