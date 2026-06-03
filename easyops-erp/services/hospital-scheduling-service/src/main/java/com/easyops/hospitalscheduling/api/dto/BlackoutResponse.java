package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BlackoutResponse {

    private UUID id;
    private UUID resourceId;
    private UUID branchId;
    private LocalDate blackoutDate;
    private String reason;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public LocalDate getBlackoutDate() { return blackoutDate; }
    public void setBlackoutDate(LocalDate blackoutDate) { this.blackoutDate = blackoutDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
