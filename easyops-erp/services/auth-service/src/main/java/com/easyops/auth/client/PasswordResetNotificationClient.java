package com.easyops.auth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends OTP delivery payloads to communication-service internal outbound API (SMS / EMAIL).
 */
@Component
public class PasswordResetNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotificationClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.communication.base-url:}")
    private String communicationBaseUrl;

    @Value("${services.communication-internal-key:}")
    private String serviceKey;

    public PasswordResetNotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendSmsOtp(String phone, String otpBodyText) {
        if (!canDispatch()) {
            log.warn("[pwd-reset] Skipping SMS (communication base URL or service key not configured)");
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Key", serviceKey.trim());
        var payload = Map.of(
                "recipient", phone.trim(),
                "message", otpBodyText
        );
        String url = trimTrailingSlash(communicationBaseUrl) + "/api/communications/internal/outbound/sms";
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);
        } catch (RestClientException ex) {
            log.error("[pwd-reset] SMS dispatch failed: {}", ex.getMessage());
            throw new RuntimeException("Unable to send SMS verification code. Please try again later.");
        }
    }

    public void sendEmailOtp(String email, String subject, String bodyText) {
        if (!canDispatch()) {
            log.warn("[pwd-reset] Skipping email (communication base URL or service key not configured)");
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Key", serviceKey.trim());
        var payload = Map.of(
                "recipient", email.trim(),
                "subject", subject,
                "body", bodyText
        );
        String url = trimTrailingSlash(communicationBaseUrl) + "/api/communications/internal/outbound/email";
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);
        } catch (RestClientException ex) {
            log.error("[pwd-reset] Email dispatch failed: {}", ex.getMessage());
            throw new RuntimeException("Unable to send email verification code. Please try again later.");
        }
    }

    /** True when auth-service should attempt SMS/email calls (production setup). */
    public boolean isDeliveryConfigured() {
        return StringUtils.hasText(communicationBaseUrl) && StringUtils.hasText(serviceKey);
    }

    private boolean canDispatch() {
        return isDeliveryConfigured();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
