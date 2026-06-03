package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;

/**
 * One row for GET /reports/usage-by-domain: aggregate by source_system.
 */
public class UsageByDomainItem {

    private String sourceSystem;
    private BigDecimal totalAmount;
    private long transactionCount;

    public UsageByDomainItem() {
    }

    public UsageByDomainItem(String sourceSystem, BigDecimal totalAmount, long transactionCount) {
        this.sourceSystem = sourceSystem;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.transactionCount = transactionCount;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }
}
