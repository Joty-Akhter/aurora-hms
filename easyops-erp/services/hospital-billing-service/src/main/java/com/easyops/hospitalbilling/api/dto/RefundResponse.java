package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RefundResponse {

    private UUID id;
    private UUID originalPaymentId;
    private UUID invoiceId;
    private BigDecimal amount;
    private String reason;
    private OffsetDateTime processedAt;
    private UUID processedByUserId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOriginalPaymentId() {
        return originalPaymentId;
    }

    public void setOriginalPaymentId(UUID originalPaymentId) {
        this.originalPaymentId = originalPaymentId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
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

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public UUID getProcessedByUserId() {
        return processedByUserId;
    }

    public void setProcessedByUserId(UUID processedByUserId) {
        this.processedByUserId = processedByUserId;
    }
}

