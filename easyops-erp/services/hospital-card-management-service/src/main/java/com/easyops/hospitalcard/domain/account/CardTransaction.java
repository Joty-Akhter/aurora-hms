package com.easyops.hospitalcard.domain.account;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_transactions", schema = "hospital_card")
public class CardTransaction {

    @Id
    private UUID id;

    @Column(name = "card_account_id", nullable = false)
    private UUID cardAccountId;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "source_system", length = 50)
    private String sourceSystem;

    @Column(name = "external_reference_id", length = 255)
    private String externalReferenceId;

    @Column(name = "authorization_id")
    private UUID authorizationId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "meal_count_delta")
    private Integer mealCountDelta;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "posted_at")
    private OffsetDateTime postedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCardAccountId() {
        return cardAccountId;
    }

    public void setCardAccountId(UUID cardAccountId) {
        this.cardAccountId = cardAccountId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalReferenceId() {
        return externalReferenceId;
    }

    public void setExternalReferenceId(String externalReferenceId) {
        this.externalReferenceId = externalReferenceId;
    }

    public UUID getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(UUID authorizationId) {
        this.authorizationId = authorizationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getMealCountDelta() {
        return mealCountDelta;
    }

    public void setMealCountDelta(Integer mealCountDelta) {
        this.mealCountDelta = mealCountDelta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(OffsetDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
