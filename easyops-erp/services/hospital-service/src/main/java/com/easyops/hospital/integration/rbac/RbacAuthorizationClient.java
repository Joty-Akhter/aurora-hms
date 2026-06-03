package com.easyops.hospital.integration.rbac;

import com.easyops.hospital.config.LoadBalancedRestTemplateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Calls rbac-service to resolve roles and assign them to users (e.g. prescribing for new doctor portal accounts).
 */
@Service
@Slf4j
public class RbacAuthorizationClient {

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate plainRestTemplate;

    @Value("${services.rbac.base-url:http://localhost:8084}")
    private String rbacBaseUrl;

    public RbacAuthorizationClient(
            @Qualifier(LoadBalancedRestTemplateConfig.BEAN_NAME) RestTemplate loadBalancedRestTemplate,
            @Qualifier("restTemplate") RestTemplate plainRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
    }

    private RestTemplate rt() {
        if (rbacBaseUrl.startsWith("http://localhost")
                || rbacBaseUrl.startsWith("https://localhost")
                || rbacBaseUrl.startsWith("http://127.")
                || rbacBaseUrl.startsWith("https://127.")) {
            return plainRestTemplate;
        }
        return loadBalancedRestTemplate;
    }

    private HttpHeaders headers(UUID actorUserId, UUID organizationId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            h.set("X-Organization-Id", organizationId.toString());
        }
        return h;
    }

    /**
     * GET /api/rbac/roles/code/{code}
     */
    public RbacRoleResponse getRoleByCode(String code, UUID actorUserId, UUID organizationId) {
        String safeCode = UriUtils.encodePathSegment(code, StandardCharsets.UTF_8);
        String url = rbacBaseUrl.replaceAll("/$", "") + "/api/rbac/roles/code/" + safeCode;
        HttpEntity<Void> entity = new HttpEntity<>(headers(actorUserId, organizationId));
        try {
            ResponseEntity<RbacRoleResponse> response = rt().exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RbacRoleResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            String msg = "rbac get role by code failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * GET /api/rbac/authorization/users/{userId}/roles
     */
    public List<RbacRoleResponse> getUserRoles(UUID userId, UUID organizationId, UUID actorUserId) {
        String base = rbacBaseUrl.replaceAll("/$", "") + "/api/rbac/authorization/users/" + userId + "/roles";
        String url = organizationId != null
                ? UriComponentsBuilder.fromUriString(base).queryParam("organizationId", organizationId).build().toUriString()
                : base;
        HttpEntity<Void> entity = new HttpEntity<>(headers(actorUserId, organizationId));
        try {
            ResponseEntity<List<RbacRoleResponse>> response = rt().exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<RbacRoleResponse>>() {});
            List<RbacRoleResponse> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpStatusCodeException e) {
            String msg = "rbac get user roles failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * POST /api/rbac/authorization/users/roles — org-scoped assignment for the target user.
     */
    public void assignRolesToUser(
            UUID targetUserId,
            Set<UUID> roleIds,
            UUID assignmentOrganizationId,
            UUID actorUserId,
            UUID organizationHeader) {
        String url = rbacBaseUrl.replaceAll("/$", "") + "/api/rbac/authorization/users/roles";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", targetUserId);
        body.put("roleIds", roleIds);
        body.put("organizationId", assignmentOrganizationId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers(actorUserId, organizationHeader));
        try {
            rt().exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (HttpStatusCodeException e) {
            String msg = "rbac assign roles failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }
}
