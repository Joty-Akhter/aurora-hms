package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAdjustmentRequest {

    @NotNull(message = "amount is required")
    private BigDecimal amount; // signed: positive = credit, negative = debit

    @NotNull(message = "reason is required")
    private String reason;

    private String idempotencyKey;

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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
