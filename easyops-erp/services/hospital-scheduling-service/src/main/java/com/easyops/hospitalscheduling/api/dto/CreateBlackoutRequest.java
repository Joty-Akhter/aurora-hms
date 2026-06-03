package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class CreateBlackoutRequest {

    private UUID resourceId;
    private UUID branchId;

    @NotNull
    private LocalDate blackoutDate;

    @Size(max = 255)
    private String reason;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public LocalDate getBlackoutDate() { return blackoutDate; }
    public void setBlackoutDate(LocalDate blackoutDate) { this.blackoutDate = blackoutDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
