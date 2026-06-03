package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateWaitlistStatusRequest {

    @NotBlank
    @Pattern(regexp = "PROMOTED|CANCELLED|EXPIRED", message = "status must be PROMOTED, CANCELLED, or EXPIRED")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
