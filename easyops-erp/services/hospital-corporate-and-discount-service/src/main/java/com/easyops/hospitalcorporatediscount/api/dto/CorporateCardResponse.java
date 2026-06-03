package com.easyops.hospitalcorporatediscount.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CorporateCardResponse {
    private UUID id;
    private UUID corporateClientId;
    private UUID contractId;
    private String holderName;
    private String holderIdentifier;
    private String cardType;
    private UUID cardProductId;
    private UUID cardId;
    private String cardNumber;
    private String status;
    private String action;
    private String title;
    private String html;
    private UUID replacedByCorporateCardId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
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
}
