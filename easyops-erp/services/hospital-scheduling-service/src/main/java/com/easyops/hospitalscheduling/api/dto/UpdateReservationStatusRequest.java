package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateReservationStatusRequest {

    @NotBlank
    @Size(max = 30)
    private String status;

    @Size(max = 500)
    private String reason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
