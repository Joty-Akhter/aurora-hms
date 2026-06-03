package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CreateRosterBlockRequest {

    @NotNull
    private OffsetDateTime startTime;

    @NotNull
    private OffsetDateTime endTime;

    @NotNull
    @Pattern(regexp = "AVAILABLE|UNAVAILABLE|SUBSTITUTE", message = "type must be AVAILABLE, UNAVAILABLE, or SUBSTITUTE")
    private String type;

    private UUID substituteResourceId;

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UUID getSubstituteResourceId() { return substituteResourceId; }
    public void setSubstituteResourceId(UUID substituteResourceId) { this.substituteResourceId = substituteResourceId; }
}
