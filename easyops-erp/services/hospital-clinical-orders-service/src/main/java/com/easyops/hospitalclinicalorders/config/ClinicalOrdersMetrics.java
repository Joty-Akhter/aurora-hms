package com.easyops.hospitalclinicalorders.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for clinical orders observability.
 * Exposed via actuator/prometheus: orders_created_total, order_sets_created_total,
 * worklist_status_changes_total, result_available_total.
 */
@Component
public class ClinicalOrdersMetrics {

    public static final int PAGINATION_MAX_PAGE_SIZE = 100;

    private final Counter orderSetsCreatedTotal;
    private final Counter ordersCreatedTotal;
    private final Counter worklistStatusChangesTotal;
    private final Counter resultAvailableTotal;

    public ClinicalOrdersMetrics(MeterRegistry registry) {
        this.orderSetsCreatedTotal = registry.counter("clinical_orders_order_sets_created_total");
        this.ordersCreatedTotal = registry.counter("clinical_orders_orders_created_total");
        this.worklistStatusChangesTotal = registry.counter("clinical_orders_worklist_status_changes_total");
        this.resultAvailableTotal = registry.counter("clinical_orders_result_available_total");
    }

    public void incrementOrderSetsCreated() {
        orderSetsCreatedTotal.increment();
    }

    public void incrementOrdersCreated() {
        ordersCreatedTotal.increment();
    }

    public void incrementWorklistStatusChanges() {
        worklistStatusChangesTotal.increment();
    }

    public void incrementResultAvailable() {
        resultAvailableTotal.increment();
    }
}
