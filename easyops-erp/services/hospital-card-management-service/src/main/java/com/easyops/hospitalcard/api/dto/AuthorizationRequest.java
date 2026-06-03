package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AuthorizationRequest {

    /** Card number for lookup (or use cardToken). */
    private String cardNumber;

    private String cardToken;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    private String currency;

    /** HOSPITAL, CANTEEN */
    private String usageDomain;

    @NotBlank(message = "sourceSystem is required")
    private String sourceSystem;

    private String externalReferenceId;

    /** For entitlement / non-monetary */
    private Integer mealCount;

    private String idempotencyKey;

    /** For lookup: cardNumber if set, else cardToken. At least one must be set. */
    public String getEffectiveCardNumber() {
        if (cardNumber != null && !cardNumber.isBlank()) {
            return cardNumber;
        }
        return cardToken != null && !cardToken.isBlank() ? cardToken : null;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
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

    public String getUsageDomain() {
        return usageDomain;
    }

    public void setUsageDomain(String usageDomain) {
        this.usageDomain = usageDomain;
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

    public Integer getMealCount() {
        return mealCount;
    }

    public void setMealCount(Integer mealCount) {
        this.mealCount = mealCount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
