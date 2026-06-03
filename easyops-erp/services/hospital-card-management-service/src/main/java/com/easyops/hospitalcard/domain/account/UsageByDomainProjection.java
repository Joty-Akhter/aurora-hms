package com.easyops.hospitalcard.domain.account;

import java.math.BigDecimal;

/**
 * Projection for usage-by-domain report: source_system, sum(amount), count.
 */
public interface UsageByDomainProjection {

    String getSourceSystem();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
