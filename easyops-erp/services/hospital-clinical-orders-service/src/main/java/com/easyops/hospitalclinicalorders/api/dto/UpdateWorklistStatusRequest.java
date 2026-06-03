package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateWorklistStatusRequest {
    @NotBlank
    private String status; // QUEUED, ASSIGNED, IN_PROGRESS, COMPLETED, ON_HOLD
    private String remarks;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
