package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * One entry from Billing/Canteen for reconciliation compare (POST /reconciliation/compare).
 */
public class ReconciliationEntryRequest {

    @NotNull
    private String sourceSystem;
    @NotNull
    private String externalReferenceId;
    @NotNull
    private BigDecimal amount;

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalReferenceId() {
        return externalReferenceId;
    }

    public void setExternalReferenceId(String externalReferenceId) {
        this.externalReferenceId = externalReferenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
