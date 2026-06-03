package com.easyops.ap.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankAccountClient implements BankGlAccountResolver {

    private final RestTemplate restTemplate;

    @Value("${services.bank.base-url:http://localhost:8092}")
    private String bankServiceBaseUrl;

    public UUID resolveGlAccountId(UUID bankAccountId, UUID actorUserId) {
        String url = bankServiceBaseUrl + "/api/bank/accounts/" + bankAccountId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", actorUserId.toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("glAccountId") == null) {
                return null;
            }
            return UUID.fromString(body.get("glAccountId").toString());
        } catch (RestClientException e) {
            log.warn("Could not resolve GL account for bank account {}: {}", bankAccountId, e.getMessage());
            return null;
        }
    }
}
