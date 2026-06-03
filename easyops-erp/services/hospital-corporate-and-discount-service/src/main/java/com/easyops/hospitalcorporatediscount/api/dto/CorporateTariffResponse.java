package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CorporateTariffResponse {

    private UUID id;
    private UUID corporateContractId;
    private String scopeType;
    private String scopeValue;
    private String tariffType;
    private BigDecimal tariffAmount;
    private BigDecimal tariffPercent;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCorporateContractId() { return corporateContractId; }
    public void setCorporateContractId(UUID corporateContractId) { this.corporateContractId = corporateContractId; }
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
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
