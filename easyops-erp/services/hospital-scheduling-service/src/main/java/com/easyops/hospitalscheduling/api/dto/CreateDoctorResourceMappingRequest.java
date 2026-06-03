package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class CreateDoctorResourceMappingRequest {
    @NotNull private UUID doctorUserId;
    @NotNull private UUID resourceId;
    private UUID branchId;
    private Boolean isPrimary;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public UUID getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(UUID doctorUserId) { this.doctorUserId = doctorUserId; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
