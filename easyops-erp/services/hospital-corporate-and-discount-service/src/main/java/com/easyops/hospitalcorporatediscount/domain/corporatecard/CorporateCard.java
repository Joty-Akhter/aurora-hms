package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "corporate_cards", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class CorporateCard {

    @Id
    private UUID id;

    @Column(name = "corporate_client_id", nullable = false)
    private UUID corporateClientId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "holder_name", nullable = false, length = 255)
    private String holderName;

    @Column(name = "holder_identifier", nullable = false, length = 255)
    private String holderIdentifier;

    @Column(name = "card_type", nullable = false, length = 100)
    private String cardType;

    @Column(name = "card_product_id", nullable = false)
    private UUID cardProductId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "card_number", nullable = false, length = 100)
    private String cardNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "replaced_by_corporate_card_id")
    private UUID replacedByCorporateCardId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCorporateClientId() { return corporateClientId; }
    public void setCorporateClientId(UUID corporateClientId) { this.corporateClientId = corporateClientId; }
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getHolderIdentifier() { return holderIdentifier; }
    public void setHolderIdentifier(String holderIdentifier) { this.holderIdentifier = holderIdentifier; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public UUID getCardProductId() { return cardProductId; }
    public void setCardProductId(UUID cardProductId) { this.cardProductId = cardProductId; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getReplacedByCorporateCardId() { return replacedByCorporateCardId; }
    public void setReplacedByCorporateCardId(UUID replacedByCorporateCardId) { this.replacedByCorporateCardId = replacedByCorporateCardId; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
