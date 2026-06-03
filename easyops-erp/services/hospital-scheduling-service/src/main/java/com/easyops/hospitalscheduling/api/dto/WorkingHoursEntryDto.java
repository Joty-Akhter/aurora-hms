package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Single entry for batch set working hours (dayOfWeek 0=Sunday .. 6=Saturday).
 */
public class WorkingHoursEntryDto {

    @NotNull
    @Min(0)
    @Max(6)
    private Integer dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    @Min(1)
    @Max(24 * 60)
    private Integer slotDurationMinutes;

    @Min(1)
    @Max(2000)
    private Integer slotsPerInterval;

    @Min(1)
    @Max(2000)
    private Integer maxSlotsPerSegment;

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
}
