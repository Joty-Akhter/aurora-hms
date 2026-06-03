package com.easyops.hospitalcard.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CardResponse {

    private UUID id;
    private String cardNumber;
    private String physicalSerial;
    private UUID cardProductId;
    private UUID limitProfileId;
    private String ownerType;
    private String ownerReferenceId;
    private UUID corporateId;
    private String status;
    private UUID replacedByCardId;
    private OffsetDateTime issuedAt;
    private OffsetDateTime activatedAt;
    private OffsetDateTime blockedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private String statusChangeReason;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getCardProductId() {
        return cardProductId;
    }

    public void setCardProductId(UUID cardProductId) {
        this.cardProductId = cardProductId;
    }

    public UUID getLimitProfileId() {
        return limitProfileId;
    }

    public void setLimitProfileId(UUID limitProfileId) {
        this.limitProfileId = limitProfileId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getReplacedByCardId() {
        return replacedByCardId;
    }

    public void setReplacedByCardId(UUID replacedByCardId) {
        this.replacedByCardId = replacedByCardId;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public OffsetDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(OffsetDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatusChangeReason() {
        return statusChangeReason;
    }

    public void setStatusChangeReason(String statusChangeReason) {
        this.statusChangeReason = statusChangeReason;
    }
}
