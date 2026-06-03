package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SlotTemplateResponse {

    private UUID id;
    private String name;
    private String resourceType;
    private UUID branchId;
    private Integer slotDurationMinutes;
    private Integer slotsPerInterval;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer leadTimeDays;
    private Integer maxAdvanceDays;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public Integer getSlotsPerInterval() { return slotsPerInterval; }
    public void setSlotsPerInterval(Integer slotsPerInterval) { this.slotsPerInterval = slotsPerInterval; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getMaxAdvanceDays() { return maxAdvanceDays; }
    public void setMaxAdvanceDays(Integer maxAdvanceDays) { this.maxAdvanceDays = maxAdvanceDays; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
