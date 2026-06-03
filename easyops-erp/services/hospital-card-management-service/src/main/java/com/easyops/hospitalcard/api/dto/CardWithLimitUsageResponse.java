package com.easyops.hospitalcard.api.dto;

import java.util.UUID;

/**
 * Card summary with current period limit usage. Used by GET /limit-profiles/{id}/cards-with-usage.
 */
public class CardWithLimitUsageResponse {

    private UUID cardId;
    private String cardNumber;
    private String ownerType;
    private String ownerReferenceId;
    private String status;
    private LimitUsageSummary limitUsage;

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerReferenceId() {
        return ownerReferenceId;
    }

    public void setOwnerReferenceId(String ownerReferenceId) {
        this.ownerReferenceId = ownerReferenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LimitUsageSummary getLimitUsage() {
        return limitUsage;
    }

    public void setLimitUsage(LimitUsageSummary limitUsage) {
        this.limitUsage = limitUsage;
    }
}
