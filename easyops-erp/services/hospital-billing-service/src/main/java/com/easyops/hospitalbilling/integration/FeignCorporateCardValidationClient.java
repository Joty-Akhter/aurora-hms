package com.easyops.hospitalbilling.integration;

import com.easyops.hospitalbilling.integration.dto.CorporateCardValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "hospital.billing",
        name = "discount-service.enabled",
        havingValue = "true"
)
public class FeignCorporateCardValidationClient implements CorporateCardValidationClient {
    private static final Logger log = LoggerFactory.getLogger(FeignCorporateCardValidationClient.class);

    private final CorporateDiscountServiceApi corporateDiscountServiceApi;

    public FeignCorporateCardValidationClient(CorporateDiscountServiceApi corporateDiscountServiceApi) {
        this.corporateDiscountServiceApi = corporateDiscountServiceApi;
    }

    @Override
    public CorporateCardValidationResponse validateCard(String cardNumber) {
        try {
            return corporateDiscountServiceApi.validateCorporateCard(cardNumber);
        } catch (Exception ex) {
            log.warn("Corporate card validation failed, treating as invalid: {}", ex.getMessage());
            CorporateCardValidationResponse response = new CorporateCardValidationResponse();
            response.setValid(false);
            response.setMessage("Card validation service unavailable");
            response.setCardNumber(cardNumber);
            return response;
        }
    }
}
