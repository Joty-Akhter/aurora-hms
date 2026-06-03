package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UpdatePlannedAdmissionStatusRequest {

    @NotNull
    @Size(max = 30)
    private String status; // RESERVED, CONVERTED, EXPIRED, CANCELLED

    private UUID bedGroupResourceId; // required when status = RESERVED

    private OffsetDateTime expiresAt; // optional when RESERVED (default e.g. 24h from now)

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getBedGroupResourceId() { return bedGroupResourceId; }
    public void setBedGroupResourceId(UUID bedGroupResourceId) { this.bedGroupResourceId = bedGroupResourceId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
