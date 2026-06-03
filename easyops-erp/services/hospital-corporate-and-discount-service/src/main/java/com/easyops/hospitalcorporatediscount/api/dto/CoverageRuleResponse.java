package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CoverageRuleResponse {

    private UUID id;
    private UUID corporateContractId;
    private String scopeType;
    private String scopeValue;
    private BigDecimal coveragePercent;
    private BigDecimal maxAmount;
    private BigDecimal coPayPercent;
    private BigDecimal deductibleAmount;
    private String applicableVisitTypes;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCorporateContractId() { return corporateContractId; }
    public void setCorporateContractId(UUID corporateContractId) { this.corporateContractId = corporateContractId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }
    public BigDecimal getCoveragePercent() { return coveragePercent; }
    public void setCoveragePercent(BigDecimal coveragePercent) { this.coveragePercent = coveragePercent; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public BigDecimal getCoPayPercent() { return coPayPercent; }
    public void setCoPayPercent(BigDecimal coPayPercent) { this.coPayPercent = coPayPercent; }
    public BigDecimal getDeductibleAmount() { return deductibleAmount; }
    public void setDeductibleAmount(BigDecimal deductibleAmount) { this.deductibleAmount = deductibleAmount; }
    public String getApplicableVisitTypes() { return applicableVisitTypes; }
    public void setApplicableVisitTypes(String applicableVisitTypes) { this.applicableVisitTypes = applicableVisitTypes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
