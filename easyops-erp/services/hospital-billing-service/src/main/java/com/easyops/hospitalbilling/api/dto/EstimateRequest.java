package com.easyops.hospitalbilling.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request body for computing an estimate (pre-invoice quote with optional discount application).
 */
public class EstimateRequest {

    private List<EstimateLineItemRequest> lineItems;
    private UUID patientId;
    private UUID corporateContractId;
    private UUID corporateClientId;
    private String cardNumber;

    public List<EstimateLineItemRequest> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<EstimateLineItemRequest> lineItems) {
        this.lineItems = lineItems;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getCorporateContractId() {
        return corporateContractId;
    }

    public void setCorporateContractId(UUID corporateContractId) {
        this.corporateContractId = corporateContractId;
    }

    public UUID getCorporateClientId() {
        return corporateClientId;
    }

    public void setCorporateClientId(UUID corporateClientId) {
        this.corporateClientId = corporateClientId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}
