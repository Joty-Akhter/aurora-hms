package com.easyops.accountingperiod;

import com.easyops.accountingperiod.dto.AccountingPeriodResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Resolves accounting periods from accounting-service for AR/AP documents.
 */
public class AccountingPeriodClient implements AccountingPeriodResolver {

    private static final Logger log = LoggerFactory.getLogger(AccountingPeriodClient.class);

    private final RestTemplate restTemplate;
    private final String accountingServiceBaseUrl;

    public AccountingPeriodClient(RestTemplate restTemplate, String accountingServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.accountingServiceBaseUrl = accountingServiceBaseUrl;
    }

    @Override
    public UUID resolvePeriodId(UUID organizationId, LocalDate date, UUID actorUserId) {
        Objects.requireNonNull(organizationId, "organizationId is required");
        Objects.requireNonNull(date, "date is required");
        Objects.requireNonNull(actorUserId, "actorUserId is required");

        String url = accountingServiceBaseUrl
                + "/api/accounting/fiscal-years/organization/"
                + organizationId
                + "/periods/for-date?date="
                + date;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", actorUserId.toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AccountingPeriodResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AccountingPeriodResponse.class);
            AccountingPeriodResponse body = response.getBody();
            if (body == null || body.getId() == null) {
                throw new RuntimeException("Accounting service returned no period for date: " + date);
            }
            return body.getId();
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException(
                    "No accounting period exists for date " + date
                            + ". Set up the fiscal year for this organization first.",
                    e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new RuntimeException(
                    "Not authorized to resolve accounting periods for this organization.", e);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new RuntimeException("Invalid date for accounting period lookup: " + date, e);
        } catch (RestClientException e) {
            log.error("Failed to resolve accounting period for org {} on {}: {}", organizationId, date, e.getMessage());
            throw new RuntimeException(
                    "Unable to resolve accounting period for date " + date
                            + ". Ensure accounting-service is reachable and fiscal years are configured.",
                    e);
        }
    }
}
