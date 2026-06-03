package com.easyops.hospitalcard.domain.card;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards", schema = "hospital_card")
public class Card {

    @Id
    private UUID id;

    @Column(name = "card_number", nullable = false, unique = true, length = 100)
    private String cardNumber;

    @Column(name = "physical_serial", length = 100)
    private String physicalSerial;

    @Column(name = "card_product_id", nullable = false)
    private UUID cardProductId;

    @Column(name = "limit_profile_id")
    private UUID limitProfileId;

    @Column(name = "owner_type", nullable = false, length = 30)
    private String ownerType;

    @Column(name = "owner_reference_id", nullable = false, length = 255)
    private String ownerReferenceId;

    @Column(name = "corporate_id")
    private UUID corporateId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "replaced_by_card_id")
    private UUID replacedByCardId;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "status_change_reason", length = 500)
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
