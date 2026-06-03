package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;

/**
 * Single discount line (description, source, amount) for display on estimates or invoices.
 */
public class DiscountLineResponse {

    private String description;
    private String source;
    private BigDecimal amount;

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
}
