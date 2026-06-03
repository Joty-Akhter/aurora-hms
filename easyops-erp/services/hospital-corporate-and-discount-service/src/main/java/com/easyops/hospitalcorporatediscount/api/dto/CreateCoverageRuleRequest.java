package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateCoverageRuleRequest {

    @NotBlank
    @Size(max = 20)
    private String scopeType;

    @NotBlank
    @Size(max = 100)
    private String scopeValue;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal coveragePercent;

    private BigDecimal maxAmount;

    private BigDecimal coPayPercent;

    private BigDecimal deductibleAmount;

    @Size(max = 100)
    private String applicableVisitTypes;

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
}
