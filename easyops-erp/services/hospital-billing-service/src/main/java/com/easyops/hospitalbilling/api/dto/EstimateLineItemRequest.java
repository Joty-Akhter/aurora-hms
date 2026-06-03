package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;

/**
 * Single line item for an estimate request.
 */
public class EstimateLineItemRequest {

    private String itemCode;
    private String itemDescription;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
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
}
