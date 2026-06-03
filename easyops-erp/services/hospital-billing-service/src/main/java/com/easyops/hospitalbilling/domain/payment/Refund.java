package com.easyops.hospitalbilling.domain.payment;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds", schema = "hospital_billing")
public class Refund {

    @Id
    private UUID id;

    @Column(name = "original_payment_id", nullable = false)
    private UUID originalPaymentId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "processed_by_user_id")
    private UUID processedByUserId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

