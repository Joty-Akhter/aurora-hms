package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Batch request to set working hours for a resource (replaces existing).
 */
public class SetWorkingHoursRequest {

    @NotEmpty
    @Valid
    private List<WorkingHoursEntryDto> entries;

    public List<WorkingHoursEntryDto> getEntries() { return entries; }
    public void setEntries(List<WorkingHoursEntryDto> entries) { this.entries = entries; }
}
