package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateCorporateCardRequest {
    @NotNull
    private UUID corporateClientId;
    private UUID contractId;
    @NotBlank
    private String holderName;
    @NotBlank
    private String holderIdentifier;
    @NotBlank
    private String cardType;
    @NotNull
    private UUID cardProductId;
    private String cardNumber;

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
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
}
