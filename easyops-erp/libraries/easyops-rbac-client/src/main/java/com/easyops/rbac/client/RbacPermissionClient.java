package com.easyops.rbac.client;

import com.easyops.rbac.client.dto.RbacPermissionDto;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fetches effective permissions from {@code rbac-service} via
 * {@code GET /api/rbac/authorization/users/{userId}/permissions}.
 */
@Slf4j
public class RbacPermissionClient {

    public static final String METER_DENIED = "erp.rbac.denied";

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    private final String rbacBaseUrl;
    private final String serviceName;

    public RbacPermissionClient(
            RestTemplate restTemplate,
            MeterRegistry meterRegistry,
            String rbacBaseUrl,
            String serviceName) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.rbacBaseUrl = rbacBaseUrl.endsWith("/") ? rbacBaseUrl.substring(0, rbacBaseUrl.length() - 1) : rbacBaseUrl;
        this.serviceName = serviceName;
    }

    public void requireAuthenticatedUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
    }

    public List<RbacPermissionDto> fetchUserPermissions(UUID userId, UUID organizationId) {
        String url = UriComponentsBuilder.fromUriString(rbacBaseUrl + "/api/rbac/authorization/users/" + userId + "/permissions")
                .queryParamIfPresent("organizationId", Optional.ofNullable(organizationId))
                .build(true)
                .toUriString();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<RbacPermissionDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<RbacPermissionDto>>() {});
            List<RbacPermissionDto> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException e) {
            log.error("[{}] RBAC permission fetch failed for user {}: {}", serviceName, userId, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Authorization service unavailable");
        }
    }

    public boolean hasAnyResourceAction(UUID userId, UUID organizationId, String[][] resourceActionPairs) {
        List<RbacPermissionDto> permissions = fetchUserPermissions(userId, organizationId);
        for (RbacPermissionDto p : permissions) {
            if (p == null || Boolean.FALSE.equals(p.getIsActive())) {
                continue;
            }
            String res = p.getResource();
            String act = p.getAction();
            if (res == null || act == null) {
                continue;
            }
            for (String[] pair : resourceActionPairs) {
                if (pair[0].equals(res) && pair[1].equals(act)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void deny(String metricOperation) {
        meterRegistry.counter(METER_DENIED, "service", serviceName, "operation", metricOperation).increment();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
    }

    public void requireAnyResourceAction(
            UUID userId,
            UUID organizationId,
            String[][] resourceActionPairs,
            String metricOperation) {
        requireAuthenticatedUser(userId);
        if (!hasAnyResourceAction(userId, organizationId, resourceActionPairs)) {
            deny(metricOperation);
        }
    }
}
