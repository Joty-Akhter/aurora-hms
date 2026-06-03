package com.easyops.hospitalcard.domain.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Phase 5.3 – Custom metrics for card operations. Exposed via actuator/prometheus.
 */
@Component
public class CardMetrics {

    private static final String AUTHORIZATIONS = "card_authorizations_total";
    private static final String TRANSACTIONS = "card_transactions_total";
    private static final String BALANCE_CHECKS = "card_balance_checks_total";

    private final MeterRegistry registry;

    public CardMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAuthorization(boolean approved) {
        Counter.builder(AUTHORIZATIONS)
                .description("Total card authorization attempts")
                .tag("outcome", approved ? "approved" : "declined")
                .register(registry)
                .increment();
    }

    public void recordTransaction(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) return;
        Counter.builder(TRANSACTIONS)
                .description("Total card transactions by type")
                .tag("type", transactionType)
                .register(registry)
                .increment();
    }

    public void recordBalanceCheck() {
        Counter.builder(BALANCE_CHECKS)
                .description("Total card balance check requests")
                .register(registry)
                .increment();
    }
}
