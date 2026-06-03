package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;

public class StaffEmploymentStatusSyncRequest {

    @NotBlank(message = "employeeId is required")
    private String employeeId;
    @NotBlank(message = "employmentStatus is required")
    private String employmentStatus;
    private String reason;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
