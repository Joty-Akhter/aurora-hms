package com.easyops.hospitalcard.api.dto;

public class ReplaceCardRequest {

    private String reason; // LOST, DAMAGED, OTHER

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
