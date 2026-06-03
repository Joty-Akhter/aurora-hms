package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class IssueCardRequest {

    @NotNull(message = "cardProductId is required")
    private UUID cardProductId;

    @NotBlank(message = "ownerType is required")
    private String ownerType;

    @NotBlank(message = "ownerReferenceId is required")
    private String ownerReferenceId;

    private UUID corporateId;

    private UUID limitProfileId;

    private String cardNumber; // optional; generated if not provided

    private String physicalSerial;

    public UUID getCardProductId() {
        return cardProductId;
    }

    public void setCardProductId(UUID cardProductId) {
        this.cardProductId = cardProductId;
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

    public UUID getCorporateId() {
        return corporateId;
    }

    public void setCorporateId(UUID corporateId) {
        this.corporateId = corporateId;
    }

    public UUID getLimitProfileId() {
        return limitProfileId;
    }

    public void setLimitProfileId(UUID limitProfileId) {
        this.limitProfileId = limitProfileId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPhysicalSerial() {
        return physicalSerial;
    }

    public void setPhysicalSerial(String physicalSerial) {
        this.physicalSerial = physicalSerial;
    }
}
