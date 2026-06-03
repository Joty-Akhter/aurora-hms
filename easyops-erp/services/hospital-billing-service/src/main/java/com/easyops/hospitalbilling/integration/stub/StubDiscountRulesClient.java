package com.easyops.hospitalbilling.integration.stub;

import com.easyops.hospitalbilling.integration.DiscountRulesClient;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsResponse;
import com.easyops.hospitalbilling.integration.dto.LineDiscountResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation that returns zero discounts. Used when
 * hospital-corporate-and-discount-service is not available or not enabled.
 * To be replaced (or overridden) when the corporate service is deployed and
 * {@code hospital.billing.discount-service.enabled=true}.
 */
@Component
@Primary
@ConditionalOnProperty(
    prefix = "hospital.billing",
    name = "discount-service.enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class StubDiscountRulesClient implements DiscountRulesClient {

    @Override
    public EvaluateDiscountsResponse evaluateDiscounts(EvaluateDiscountsRequest request) {
        EvaluateDiscountsResponse response = new EvaluateDiscountsResponse();
        List<LineDiscountResult> lineDiscounts = new ArrayList<>();
        if (request != null && request.getItems() != null) {
            for (int i = 0; i < request.getItems().size(); i++) {
                LineDiscountResult r = new LineDiscountResult();
                r.setLineIndex(i);
                r.setDiscountAmount(BigDecimal.ZERO);
                r.setSource("STUB");
                lineDiscounts.add(r);
            }
        }
        response.setLineDiscounts(lineDiscounts);
        return response;
    }
}
