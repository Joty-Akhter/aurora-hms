package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCardStatusRequest {

    @NotBlank(message = "status is required")
    private String status; // ISSUED, ACTIVE, BLOCKED, SUSPENDED, REVOKED, REPLACED, EXPIRED, CLOSED

    private String reason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
