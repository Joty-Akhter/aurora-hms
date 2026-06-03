package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public class UpdateDoctorResourceMappingRequest {
    private UUID branchId;
    private Boolean isPrimary;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;

    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
