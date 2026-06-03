package com.easyops.hospitalbilling.integration;

import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsResponse;

/**
 * Client for evaluating discount rules (e.g. from hospital-corporate-and-discount-service).
 * When the corporate/discount service is not available, a stub implementation returns zero
 * discounts. Wire a Feign-based implementation when the service is deployed.
 *
 * @see com.easyops.hospitalbilling.integration.stub.StubDiscountRulesClient
 * @see requirements/module-hospital/hospital-corporate-and-discount-service-implementation-plan.md
 */
public interface DiscountRulesClient {

    /**
     * Evaluates applicable discounts for the given line items and context (patient, corporate contract).
     *
     * @param request context and line items
     * @return per-line discount amounts and sources; may be empty/zero when service is unavailable
     */
    EvaluateDiscountsResponse evaluateDiscounts(EvaluateDiscountsRequest request);
}
