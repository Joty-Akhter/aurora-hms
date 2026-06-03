package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;

/**
 * Request to add an invoice-level discount line.
 */
public class CreateInvoiceDiscountLineRequest {

    private String description;
    private String source;
    private BigDecimal amount;
    private java.util.UUID createdBy;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public java.util.UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(java.util.UUID createdBy) {
        this.createdBy = createdBy;
    }
}
