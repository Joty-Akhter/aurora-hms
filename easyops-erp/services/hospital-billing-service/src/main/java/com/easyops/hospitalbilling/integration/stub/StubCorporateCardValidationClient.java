package com.easyops.hospitalbilling.integration.stub;

import com.easyops.hospitalbilling.integration.CorporateCardValidationClient;
import com.easyops.hospitalbilling.integration.dto.CorporateCardValidationResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "hospital.billing",
        name = "discount-service.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class StubCorporateCardValidationClient implements CorporateCardValidationClient {
    @Override
    public CorporateCardValidationResponse validateCard(String cardNumber) {
        CorporateCardValidationResponse response = new CorporateCardValidationResponse();
        response.setValid(true);
        response.setMessage("Validation skipped (discount service disabled)");
        response.setCardNumber(cardNumber);
        return response;
    }
}
