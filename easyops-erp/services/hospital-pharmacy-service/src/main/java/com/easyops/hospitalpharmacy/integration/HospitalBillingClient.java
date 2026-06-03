package com.easyops.hospitalpharmacy.integration;

import com.easyops.hospitalpharmacy.integration.dto.CreateChargeBatchPayload;
import com.easyops.hospitalpharmacy.integration.dto.CreateChargePayload;
import com.easyops.hospitalpharmacy.integration.dto.ChargeResponsePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Calls {@code hospital-billing-service} {@code POST /api/hospital-billing/charges}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HospitalBillingClient {

    private static final String CHARGES_URL = "http://hospital-billing-service/api/hospital-billing/charges";

    private final RestTemplate loadBalancedRestTemplate;

    public List<ChargeResponsePayload> postCharges(
            UUID actorUserId,
            UUID organizationId,
            List<CreateChargePayload> charges) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        CreateChargeBatchPayload body = new CreateChargeBatchPayload();
        body.setCharges(charges);
        HttpEntity<CreateChargeBatchPayload> entity = new HttpEntity<>(body, headers);
        try {
            return loadBalancedRestTemplate.exchange(
                    CHARGES_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<ChargeResponsePayload>>() {}
            ).getBody();
        } catch (Exception ex) {
            log.error("Billing charge post failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public CreateChargePayload buildCharge(
            String sourceServiceName,
            UUID patientId,
            UUID visitId,
            String itemCode,
            String itemDescription,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String idempotencyKey) {
        CreateChargePayload c = new CreateChargePayload();
        c.setSourceService(sourceServiceName);
        c.setSourceReferenceId(itemCode + ":" + idempotencyKey);
        c.setPatientId(patientId);
        c.setVisitId(visitId);
        c.setItemCode(itemCode);
        c.setItemDescription(itemDescription);
        c.setQuantity(quantity);
        c.setUnitPrice(unitPrice);
        c.setIdempotencyKey(idempotencyKey);
        return c;
    }
}
