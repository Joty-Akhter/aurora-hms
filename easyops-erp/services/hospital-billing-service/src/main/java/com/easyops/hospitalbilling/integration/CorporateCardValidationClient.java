package com.easyops.hospitalbilling.integration;

import com.easyops.hospitalbilling.integration.dto.CorporateCardValidationResponse;

public interface CorporateCardValidationClient {
    CorporateCardValidationResponse validateCard(String cardNumber);
}
