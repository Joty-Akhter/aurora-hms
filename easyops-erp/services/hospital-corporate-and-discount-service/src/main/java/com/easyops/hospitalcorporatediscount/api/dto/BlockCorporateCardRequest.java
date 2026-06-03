package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;

public class BlockCorporateCardRequest {
    @NotBlank
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
