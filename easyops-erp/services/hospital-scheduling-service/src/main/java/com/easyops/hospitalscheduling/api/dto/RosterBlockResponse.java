package com.easyops.hospitalscheduling.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RosterBlockResponse {

    private UUID id;
    private UUID resourceId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String type;
    private UUID substituteResourceId;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UUID getSubstituteResourceId() { return substituteResourceId; }
    public void setSubstituteResourceId(UUID substituteResourceId) { this.substituteResourceId = substituteResourceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
