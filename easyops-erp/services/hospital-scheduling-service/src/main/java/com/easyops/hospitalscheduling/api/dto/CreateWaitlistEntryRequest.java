package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class CreateWaitlistEntryRequest {

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID resourceId;

    private LocalDate preferredFromDate;
    private LocalDate preferredToDate;
    private Integer priority;

    @Size(max = 50)
    private String priorityReason;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getPreferredFromDate() { return preferredFromDate; }
    public void setPreferredFromDate(LocalDate preferredFromDate) { this.preferredFromDate = preferredFromDate; }
    public LocalDate getPreferredToDate() { return preferredToDate; }
    public void setPreferredToDate(LocalDate preferredToDate) { this.preferredToDate = preferredToDate; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getPriorityReason() { return priorityReason; }
    public void setPriorityReason(String priorityReason) { this.priorityReason = priorityReason; }
}
