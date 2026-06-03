package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscountDecisionResponse {

    private UUID id;
    private String billContextId;
    private UUID patientId;
    private UUID corporateClientId;
    private UUID discountSchemeId;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private UUID decidedByUserId;
    private UUID approvedByUserId;
    private OffsetDateTime createdAt;
    private OffsetDateTime approvedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBillContextId() { return billContextId; }
    public void setBillContextId(String billContextId) { this.billContextId = billContextId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getCorporateClientId() { return corporateClientId; }
    public void setCorporateClientId(UUID corporateClientId) { this.corporateClientId = corporateClientId; }
    public UUID getDiscountSchemeId() { return discountSchemeId; }
    public void setDiscountSchemeId(UUID discountSchemeId) { this.discountSchemeId = discountSchemeId; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public UUID getDecidedByUserId() { return decidedByUserId; }
    public void setDecidedByUserId(UUID decidedByUserId) { this.decidedByUserId = decidedByUserId; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(UUID approvedByUserId) { this.approvedByUserId = approvedByUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
}
