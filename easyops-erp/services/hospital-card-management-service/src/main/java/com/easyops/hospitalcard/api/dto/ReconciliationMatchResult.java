package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;

/**
 * Result of comparing one reconciliation entry with card_transactions.
 */
public class ReconciliationMatchResult {

    public static final String MATCHED = "MATCHED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";

    private String sourceSystem;
    private String externalReferenceId;
    private String status;  // MATCHED | NOT_FOUND | AMOUNT_MISMATCH
    private BigDecimal expectedAmount;  // from request
    private BigDecimal actualAmount;     // from card transaction (null if NOT_FOUND)

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }
}
