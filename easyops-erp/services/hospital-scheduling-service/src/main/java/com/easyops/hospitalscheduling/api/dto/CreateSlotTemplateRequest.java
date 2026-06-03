package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

public class CreateSlotTemplateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 30)
    private String resourceType;

    private UUID branchId;

    @NotNull
    @Min(1)
    private Integer slotDurationMinutes;

    @NotNull
    @Min(1)
    private Integer slotsPerInterval;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Min(0)
    private Integer leadTimeDays;

    @Min(0)
    private Integer maxAdvanceDays;

    @Size(max = 20)
    private String status;

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
}
