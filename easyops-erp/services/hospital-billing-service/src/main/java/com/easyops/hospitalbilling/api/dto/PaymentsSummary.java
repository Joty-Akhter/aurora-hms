package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentsSummary {

    private BigDecimal totalPaid;
    private OffsetDateTime lastPaymentAt;

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public OffsetDateTime getLastPaymentAt() {
        return lastPaymentAt;
    }

    public void setLastPaymentAt(OffsetDateTime lastPaymentAt) {
        this.lastPaymentAt = lastPaymentAt;
    }
}

