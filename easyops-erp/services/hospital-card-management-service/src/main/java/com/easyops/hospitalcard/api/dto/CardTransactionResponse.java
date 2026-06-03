package com.easyops.hospitalcard.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CardTransactionResponse {

    private UUID id;
    private UUID cardAccountId;
    private String transactionType;
    private String sourceSystem;
    private String externalReferenceId;
    private UUID authorizationId;
    private java.math.BigDecimal amount;
    private String currency;
    private Integer mealCountDelta;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime postedAt;
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

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
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
