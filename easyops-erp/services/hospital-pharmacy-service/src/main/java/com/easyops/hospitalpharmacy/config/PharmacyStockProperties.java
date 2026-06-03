package com.easyops.hospitalpharmacy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "hospital.pharmacy.stock")
public class PharmacyStockProperties {

    /**
     * Absolute quantity delta threshold above which a stock adjustment requires manager approval.
     * Default is effectively unlimited (no approval required unless configured).
     */
    private BigDecimal adjustmentApprovalThreshold = new BigDecimal("999999");

    public BigDecimal getAdjustmentApprovalThreshold() {
        return adjustmentApprovalThreshold;
    }

    public void setAdjustmentApprovalThreshold(BigDecimal adjustmentApprovalThreshold) {
        this.adjustmentApprovalThreshold = adjustmentApprovalThreshold;
    }
}
