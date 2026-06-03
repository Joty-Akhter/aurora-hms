package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateCorporateTariffRequest {

    @NotBlank
    @Size(max = 20)
    private String scopeType;

    @NotBlank
    @Size(max = 100)
    private String scopeValue;

    @NotBlank
    @Size(max = 20)
    private String tariffType;

    private BigDecimal tariffAmount;

    private BigDecimal tariffPercent;

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }
    public String getTariffType() { return tariffType; }
    public void setTariffType(String tariffType) { this.tariffType = tariffType; }
    public BigDecimal getTariffAmount() { return tariffAmount; }
    public void setTariffAmount(BigDecimal tariffAmount) { this.tariffAmount = tariffAmount; }
    public BigDecimal getTariffPercent() { return tariffPercent; }
    public void setTariffPercent(BigDecimal tariffPercent) { this.tariffPercent = tariffPercent; }
}
