package com.easyops.hospitalcorporatediscount.domain.coverage;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "coverage_rules", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class CoverageRule {

    @Id
    private UUID id;

    @Column(name = "corporate_contract_id", nullable = false)
    private UUID corporateContractId;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    @Column(name = "scope_value", nullable = false, length = 100)
    private String scopeValue;

    @Column(name = "coverage_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal coveragePercent;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "co_pay_percent", precision = 5, scale = 2)
    private BigDecimal coPayPercent;

    @Column(name = "deductible_amount", precision = 19, scale = 4)
    private BigDecimal deductibleAmount;

    @Column(name = "applicable_visit_types", length = 100)
    private String applicableVisitTypes;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCorporateContractId() {
        return corporateContractId;
    }

    public void setCorporateContractId(UUID corporateContractId) {
        this.corporateContractId = corporateContractId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeValue() {
        return scopeValue;
    }

    public void setScopeValue(String scopeValue) {
        this.scopeValue = scopeValue;
    }

    public BigDecimal getCoveragePercent() {
        return coveragePercent;
    }

    public void setCoveragePercent(BigDecimal coveragePercent) {
        this.coveragePercent = coveragePercent;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getCoPayPercent() {
        return coPayPercent;
    }

    public void setCoPayPercent(BigDecimal coPayPercent) {
        this.coPayPercent = coPayPercent;
    }

    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }

    public void setDeductibleAmount(BigDecimal deductibleAmount) {
        this.deductibleAmount = deductibleAmount;
    }

    public String getApplicableVisitTypes() {
        return applicableVisitTypes;
    }

    public void setApplicableVisitTypes(String applicableVisitTypes) {
        this.applicableVisitTypes = applicableVisitTypes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
