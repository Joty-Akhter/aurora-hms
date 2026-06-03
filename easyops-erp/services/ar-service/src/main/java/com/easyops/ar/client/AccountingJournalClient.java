package com.easyops.ar.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
public class AccountingJournalClient implements JournalEntryPoster {

    private final RestTemplate restTemplate;

    @Value("${services.accounting.base-url:http://localhost:8088}")
    private String accountingServiceBaseUrl;

    public UUID createAndPostJournal(Map<String, Object> journalEntry, UUID actorUserId) {
        String url = accountingServiceBaseUrl + "/api/accounting/journal-entries";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(journalEntry, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);
            if (result == null || result.get("id") == null) {
                throw new RuntimeException("Accounting service returned no journal id");
            }
            return UUID.fromString(result.get("id").toString());
        } catch (HttpClientErrorException e) {
            log.error("Accounting service rejected journal entry: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to post journal entry to general ledger: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("Failed to post journal entry: {}", e.getMessage());
            throw new RuntimeException("Failed to post journal entry to general ledger: " + e.getMessage(), e);
        }
    }
}
