package com.easyops.hospitalbilling.integration;

import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for hospital-corporate-and-discount-service POST /discounts/evaluate.
 * Only active when {@code hospital.billing.discount-service.enabled=true}.
 * When the corporate service is not yet deployed, keep the property false and use
 * {@link com.easyops.hospitalbilling.integration.stub.StubDiscountRulesClient}.
 */
@FeignClient(
    name = "hospital-corporate-and-discount-service",
    path = "/api/hospital-corporate-discount",
    configuration = CorporateDiscountFeignConfig.class
)
public interface CorporateDiscountServiceApi {

    /**
     * Evaluates applicable discount rules for the given request.
     * Contract matches hospital-corporate-and-discount-service implementation plan.
     */
    @PostMapping("/discounts/evaluate")
    EvaluateDiscountsResponse evaluateDiscounts(@RequestBody EvaluateDiscountsRequest request);

    @GetMapping("/corporate-cards/validate")
    com.easyops.hospitalbilling.integration.dto.CorporateCardValidationResponse validateCorporateCard(
            @RequestParam("cardNumber") String cardNumber
    );
}
