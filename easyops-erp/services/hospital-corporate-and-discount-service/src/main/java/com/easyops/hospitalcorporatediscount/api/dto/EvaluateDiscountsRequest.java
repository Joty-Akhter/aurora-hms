package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EvaluateDiscountsRequest {

    private UUID patientId;
    private UUID visitId;
    private UUID corporateClientId;

    @Size(max = 20)
    private String visitType;

    private UUID departmentId;

    @NotNull
    @Valid
    private List<EvaluateDiscountsItemRequest> items;

    private UUID requestedSchemeId;
    private BigDecimal requestedDiscountPercent;
    private BigDecimal requestedDiscountAmount;

    @Size(max = 500)
    private String reason;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public UUID getCorporateClientId() { return corporateClientId; }
    public void setCorporateClientId(UUID corporateClientId) { this.corporateClientId = corporateClientId; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public List<EvaluateDiscountsItemRequest> getItems() { return items; }
    public void setItems(List<EvaluateDiscountsItemRequest> items) { this.items = items; }
    public UUID getRequestedSchemeId() { return requestedSchemeId; }
    public void setRequestedSchemeId(UUID requestedSchemeId) { this.requestedSchemeId = requestedSchemeId; }
    public BigDecimal getRequestedDiscountPercent() { return requestedDiscountPercent; }
    public void setRequestedDiscountPercent(BigDecimal requestedDiscountPercent) { this.requestedDiscountPercent = requestedDiscountPercent; }
    public BigDecimal getRequestedDiscountAmount() { return requestedDiscountAmount; }
    public void setRequestedDiscountAmount(BigDecimal requestedDiscountAmount) { this.requestedDiscountAmount = requestedDiscountAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
