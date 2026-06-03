package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;

public class IssueStaffCardRequest {

    @NotBlank(message = "employeeId is required")
    private String employeeId;
    @NotBlank(message = "employmentStatus is required")
    private String employmentStatus;
    private Boolean adminOverride;
    private String overrideReason;
    private String cardNumber;
    private String physicalSerial;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Boolean getAdminOverride() {
        return adminOverride;
    }

    public void setAdminOverride(Boolean adminOverride) {
        this.adminOverride = adminOverride;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getPhysicalSerial() {
        return physicalSerial;
    }

    public void setPhysicalSerial(String physicalSerial) {
        this.physicalSerial = physicalSerial;
    }
}
