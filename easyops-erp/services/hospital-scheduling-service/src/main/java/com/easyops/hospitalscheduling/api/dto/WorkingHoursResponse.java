package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public class WorkingHoursResponse {

    private UUID id;
    private UUID resourceId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer slotDurationMinutes;
    private Integer slotsPerInterval;
    private Integer maxSlotsPerSegment;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public Integer getSlotsPerInterval() { return slotsPerInterval; }
    public void setSlotsPerInterval(Integer slotsPerInterval) { this.slotsPerInterval = slotsPerInterval; }
    public Integer getMaxSlotsPerSegment() { return maxSlotsPerSegment; }
    public void setMaxSlotsPerSegment(Integer maxSlotsPerSegment) { this.maxSlotsPerSegment = maxSlotsPerSegment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
