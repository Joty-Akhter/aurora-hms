package com.easyops.inventory.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Posts journal entries to accounting-service integration API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountingJournalClient {

    private final RestTemplate restTemplate;

    @Value("${services.accounting.base-url:http://localhost:8088}")
    private String accountingServiceBaseUrl;

    public Map<String, Object> createAndPostJournal(Map<String, Object> journalEntry, UUID actorUserId) {
        String url = accountingServiceBaseUrl + "/api/accounting/journal-entries";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(journalEntry, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);
            if (result == null) {
                throw new RuntimeException("Accounting service returned empty response");
            }
            return result;
        } catch (RestClientException e) {
            log.error("Failed to post journal entry: {}", e.getMessage());
            throw new RuntimeException("Failed to post journal entry to general ledger: " + e.getMessage(), e);
        }
    }

    public String resolveAccountCode(UUID accountId, UUID actorUserId) {
        if (accountId == null) {
            return null;
        }
        String url = accountingServiceBaseUrl + "/api/accounting/coa/" + accountId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", actorUserId.toString());
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<AccountingAccountRef> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<AccountingAccountRef>() {});
            AccountingAccountRef account = response.getBody();
            if (account != null && account.getAccountCode() != null) {
                return account.getAccountCode();
            }
        } catch (RestClientException e) {
            log.warn("Could not resolve account {} to code: {}", accountId, e.getMessage());
        }
        return null;
    }
}
