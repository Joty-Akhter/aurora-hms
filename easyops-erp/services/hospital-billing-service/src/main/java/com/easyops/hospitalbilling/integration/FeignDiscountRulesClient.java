package com.easyops.hospitalbilling.integration;

import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsResponse;
import com.easyops.hospitalbilling.integration.dto.LineDiscountResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DiscountRulesClient implementation that delegates to hospital-corporate-and-discount-service
 * via Feign. Active only when {@code hospital.billing.discount-service.enabled=true}.
 * When the corporate service is unavailable, set the property to false to use the stub.
 */
@Component
@ConditionalOnProperty(
    prefix = "hospital.billing",
    name = "discount-service.enabled",
    havingValue = "true"
)
public class FeignDiscountRulesClient implements DiscountRulesClient {

    private static final Logger log = LoggerFactory.getLogger(FeignDiscountRulesClient.class);

    private final CorporateDiscountServiceApi corporateDiscountServiceApi;

    public FeignDiscountRulesClient(CorporateDiscountServiceApi corporateDiscountServiceApi) {
        this.corporateDiscountServiceApi = corporateDiscountServiceApi;
    }

    @Override
    public EvaluateDiscountsResponse evaluateDiscounts(EvaluateDiscountsRequest request) {
        try {
            return corporateDiscountServiceApi.evaluateDiscounts(request);
        } catch (Exception e) {
            log.warn("Discount service call failed, returning zero discounts: {}", e.getMessage());
            return buildZeroResponse(request);
        }
    }

    private static EvaluateDiscountsResponse buildZeroResponse(EvaluateDiscountsRequest request) {
        EvaluateDiscountsResponse response = new EvaluateDiscountsResponse();
        if (request != null && request.getItems() != null) {
            List<LineDiscountResult> list = new ArrayList<>();
            for (int i = 0; i < request.getItems().size(); i++) {
                LineDiscountResult r = new LineDiscountResult();
                r.setLineIndex(i);
                r.setDiscountAmount(BigDecimal.ZERO);
                r.setSource("SERVICE_UNAVAILABLE");
                list.add(r);
            }
            response.setLineDiscounts(list);
        }
        return response;
    }
}
