package com.easyops.hospitalbilling.integration.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to evaluate discounts for a set of line items.
 * Matches the contract expected by hospital-corporate-and-discount-service POST /discounts/evaluate.
 */
public class EvaluateDiscountsRequest {

    private UUID patientId;
    private UUID visitId;
    private UUID corporateClientId;
    private List<LineItemForDiscountRequest> items;

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public UUID getCorporateClientId() {
        return corporateClientId;
    }

    public void setCorporateClientId(UUID corporateClientId) {
        this.corporateClientId = corporateClientId;
    }

    public List<LineItemForDiscountRequest> getItems() {
        return items;
    }

    public void setItems(List<LineItemForDiscountRequest> items) {
        this.items = items;
    }
}
