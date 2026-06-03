package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class EvaluateCoverageItemRequest {

    @NotBlank
    @Size(max = 100)
    private String serviceCode;

    private UUID serviceGroupId;

    private UUID departmentId;

    @NotNull
    private BigDecimal quantity;

    @NotNull
    private BigDecimal basePrice;

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public UUID getServiceGroupId() { return serviceGroupId; }
    public void setServiceGroupId(UUID serviceGroupId) { this.serviceGroupId = serviceGroupId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
}
