package com.easyops.hospital.integration.usermanagement;

import com.easyops.hospital.config.LoadBalancedRestTemplateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calls user-management-service to create users (same {@code users.users} rows auth-service uses for login),
 * activate them when a deactivated doctor is restored, and deactivate when the doctor is retired.
 */
@Service
@Slf4j
public class UserManagementClient {

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate plainRestTemplate;

    @Value("${services.user-management.base-url:http://localhost:8082}")
    private String userManagementBaseUrl;

    public UserManagementClient(
            @Qualifier(LoadBalancedRestTemplateConfig.BEAN_NAME) RestTemplate loadBalancedRestTemplate,
            @Qualifier("restTemplate") RestTemplate plainRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
    }

    private RestTemplate rt() {
        if (userManagementBaseUrl.startsWith("http://localhost")
                || userManagementBaseUrl.startsWith("https://localhost")
                || userManagementBaseUrl.startsWith("http://127.")
                || userManagementBaseUrl.startsWith("https://127.")) {
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
     * POST /api/users — returns created user id or throws {@link RestClientException}.
     */
    public UserManagementUserResponse createUser(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            UUID actorUserId,
            UUID organizationId) {
        String url = userManagementBaseUrl.replaceAll("/$", "") + "/api/users";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        if (email != null && !email.isBlank()) {
            body.put("email", email.trim());
        }
        body.put("password", password);
        if (firstName != null && !firstName.isBlank()) {
            body.put("firstName", firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            body.put("lastName", lastName.trim());
        }
        if (phone != null && !phone.isBlank()) {
            body.put("phone", phone.trim());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers(actorUserId, organizationId));
        try {
            ResponseEntity<UserManagementUserResponse> response = rt().exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    UserManagementUserResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            String msg = "user-management create user failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * PATCH /api/users/{id}/deactivate — soft delete (is_active = false).
     */
    public void deactivateUser(UUID userId, UUID actorUserId, UUID organizationId) {
        String url = userManagementBaseUrl.replaceAll("/$", "") + "/api/users/" + userId + "/deactivate";
        HttpEntity<Void> entity = new HttpEntity<>(headers(actorUserId, organizationId));
        try {
            rt().exchange(url, HttpMethod.PATCH, entity, UserManagementUserResponse.class);
        } catch (HttpStatusCodeException e) {
            String msg = "user-management deactivate user failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        } catch (RestClientException e) {
            String msg = "user-management deactivate user failed: " + e.getMessage();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * PATCH /api/users/{id}/activate — set {@code is_active = true} (e.g. doctor profile restored).
     */
    public void activateUser(UUID userId, UUID actorUserId, UUID organizationId) {
        String url = userManagementBaseUrl.replaceAll("/$", "") + "/api/users/" + userId + "/activate";
        HttpEntity<Void> entity = new HttpEntity<>(headers(actorUserId, organizationId));
        try {
            rt().exchange(url, HttpMethod.PATCH, entity, UserManagementUserResponse.class);
        } catch (HttpStatusCodeException e) {
            String msg = "user-management activate user failed: HTTP " + e.getStatusCode().value()
                    + " — " + e.getResponseBodyAsString();
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }
    }
}
