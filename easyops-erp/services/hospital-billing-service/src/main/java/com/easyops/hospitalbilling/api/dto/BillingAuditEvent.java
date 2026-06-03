package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BillingAuditEvent {

    private String type; // PAYMENT, REFUND, ADJUSTMENT, DISCOUNT
    private UUID invoiceId;
    private UUID paymentId;
    private UUID refundId;
    private UUID adjustmentId;
    private UUID chargeLineId;
    private BigDecimal amount;
    private UUID userId;
    private OffsetDateTime occurredAt;
    private String description;
    private String source;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getRefundId() {
        return refundId;
    }

    public void setRefundId(UUID refundId) {
        this.refundId = refundId;
    }

    public UUID getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(UUID adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public UUID getChargeLineId() {
        return chargeLineId;
    }

    public void setChargeLineId(UUID chargeLineId) {
        this.chargeLineId = chargeLineId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

