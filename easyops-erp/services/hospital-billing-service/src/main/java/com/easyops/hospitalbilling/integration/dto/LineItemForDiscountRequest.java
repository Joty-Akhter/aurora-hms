package com.easyops.hospitalbilling.integration.dto;

import java.math.BigDecimal;

/**
 * Single line item sent to the corporate/discount service for evaluation.
 */
public class LineItemForDiscountRequest {

    private String serviceCode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private java.util.UUID departmentId;

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public java.util.UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(java.util.UUID departmentId) {
        this.departmentId = departmentId;
    }
}
