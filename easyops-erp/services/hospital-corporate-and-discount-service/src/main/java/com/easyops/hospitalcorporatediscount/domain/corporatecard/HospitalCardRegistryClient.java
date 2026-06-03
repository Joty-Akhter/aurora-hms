package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class HospitalCardRegistryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HospitalCardRegistryClient(
            RestTemplate restTemplate,
            @Value("${hospital.card-management.base-url:http://hospital-card-management-service}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public RegistryCardResult issue(
            UUID actorUserId,
            UUID organizationId,
            UUID cardProductId,
            UUID corporateClientId,
            String ownerReferenceId,
            String cardNumber) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardProductId", cardProductId);
        payload.put("ownerType", "CORPORATE_BENEFICIARY");
        payload.put("ownerReferenceId", ownerReferenceId);
        payload.put("corporateId", corporateClientId);
        if (cardNumber != null && !cardNumber.isBlank()) {
            payload.put("cardNumber", cardNumber.trim());
        }
        return exchangeCard(actorUserId, organizationId, "/api/hospital-card-management/cards", HttpMethod.POST, payload);
    }

    public RegistryCardResult replace(
            UUID actorUserId,
            UUID organizationId,
            UUID cardId,
            String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", reason);
        return exchangeCard(actorUserId, organizationId, "/api/hospital-card-management/cards/" + cardId + "/replace", HttpMethod.POST, payload);
    }

    public RegistryCardResult updateStatus(
            UUID actorUserId,
            UUID organizationId,
            UUID cardId,
            String status,
            String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("reason", reason);
        return exchangeCard(actorUserId, organizationId, "/api/hospital-card-management/cards/" + cardId + "/status", HttpMethod.PATCH, payload);
    }

    public RegistryCardResult findByCardNumber(UUID actorUserId, UUID organizationId, String cardNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String encodedCardNumber = URLEncoder.encode(cardNumber, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/hospital-card-management/cards/search?cardNumber=" + encodedCardNumber + "&page=0&size=1";
        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to verify card with hospital-card-management-service. Please retry.", ex);
        }
        Map<String, Object> body = response.getBody();
        if (body == null) {
            return null;
        }
        Object content = body.get("content");
        if (!(content instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> cardRow)) {
            return null;
        }
        Object idValue = cardRow.get("id");
        Object cardNumberValue = cardRow.get("cardNumber");
        Object statusValue = cardRow.get("status");
        if (!(idValue instanceof String idString) || !(cardNumberValue instanceof String cardNumberString)) {
            return null;
        }
        String status = statusValue instanceof String ? (String) statusValue : "UNKNOWN";
        return new RegistryCardResult(UUID.fromString(idString), cardNumberString, status);
    }

    private RegistryCardResult exchangeCard(
            UUID actorUserId,
            UUID organizationId,
            String path,
            HttpMethod method,
            Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    baseUrl + path,
                    method,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to sync with hospital-card-management-service. Please retry.", ex);
        }
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Card registry returned empty response");
        }
        Object idValue = body.get("id");
        Object cardNumberValue = body.get("cardNumber");
        Object statusValue = body.get("status");
        if (!(idValue instanceof String idString) || !(cardNumberValue instanceof String cardNumberString)) {
            throw new IllegalStateException("Card registry response missing id/cardNumber");
        }
        String status = statusValue instanceof String ? (String) statusValue : "ISSUED";
        return new RegistryCardResult(UUID.fromString(idString), cardNumberString, status);
    }

    public record RegistryCardResult(UUID cardId, String cardNumber, String status) {}
}
