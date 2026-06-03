package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ReplaceStaffCardRequest {

    @NotBlank(message = "reason is required")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
