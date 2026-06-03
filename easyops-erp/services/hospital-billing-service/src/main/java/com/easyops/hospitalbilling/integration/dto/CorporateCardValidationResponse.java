package com.easyops.hospitalbilling.integration.dto;

import java.util.UUID;

public class CorporateCardValidationResponse {
    private boolean valid;
    private String message;
    private UUID corporateCardId;
    private UUID cardId;
    private String cardNumber;
    private String corporateStatus;
    private String registryStatus;
    private UUID corporateClientId;
    private UUID contractId;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public UUID getCorporateCardId() { return corporateCardId; }
    public void setCorporateCardId(UUID corporateCardId) { this.corporateCardId = corporateCardId; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCorporateStatus() { return corporateStatus; }
    public void setCorporateStatus(String corporateStatus) { this.corporateStatus = corporateStatus; }
    public String getRegistryStatus() { return registryStatus; }
    public void setRegistryStatus(String registryStatus) { this.registryStatus = registryStatus; }
    public UUID getCorporateClientId() { return corporateClientId; }
    public void setCorporateClientId(UUID corporateClientId) { this.corporateClientId = corporateClientId; }
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
}
