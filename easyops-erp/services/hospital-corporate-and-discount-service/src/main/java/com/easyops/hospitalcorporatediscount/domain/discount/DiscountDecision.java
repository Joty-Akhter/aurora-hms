package com.easyops.hospitalcorporatediscount.domain.discount;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "discount_decisions", schema = "hospital_corporate_discount")
public class DiscountDecision {

    @Id
    private UUID id;

    @Column(name = "bill_context_id", length = 255)
    private String billContextId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "corporate_client_id")
    private UUID corporateClientId;

    @Column(name = "discount_scheme_id")
    private UUID discountSchemeId;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

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
