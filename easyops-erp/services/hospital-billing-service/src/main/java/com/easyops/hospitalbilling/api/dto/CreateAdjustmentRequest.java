package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;

public class CreateAdjustmentRequest {

    private String type;
    private BigDecimal amount;
    private String reason;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

