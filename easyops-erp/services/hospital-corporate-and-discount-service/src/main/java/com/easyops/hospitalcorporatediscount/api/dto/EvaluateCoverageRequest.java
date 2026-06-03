package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class EvaluateCoverageRequest {

    private UUID patientId;
    private UUID visitId;

    @NotNull
    private UUID corporateContractId;

    @Size(max = 20)
    private String visitType; // OP, IP, ED

    @NotNull
    @Valid
    private List<EvaluateCoverageItemRequest> items;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public UUID getCorporateContractId() { return corporateContractId; }
    public void setCorporateContractId(UUID corporateContractId) { this.corporateContractId = corporateContractId; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public List<EvaluateCoverageItemRequest> getItems() { return items; }
    public void setItems(List<EvaluateCoverageItemRequest> items) { this.items = items; }
}
