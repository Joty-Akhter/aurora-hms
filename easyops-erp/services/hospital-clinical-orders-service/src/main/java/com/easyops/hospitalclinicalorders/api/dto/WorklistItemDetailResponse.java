package com.easyops.hospitalclinicalorders.api.dto;

import java.util.UUID;

public class WorklistItemDetailResponse extends WorklistItemResponse {
    private ClinicalOrderResponse order;
    private UUID orderSetId;
    private UUID patientId;
    private UUID visitId;

    public ClinicalOrderResponse getOrder() { return order; }
    public void setOrder(ClinicalOrderResponse order) { this.order = order; }
    public UUID getOrderSetId() { return orderSetId; }
    public void setOrderSetId(UUID orderSetId) { this.orderSetId = orderSetId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
}
