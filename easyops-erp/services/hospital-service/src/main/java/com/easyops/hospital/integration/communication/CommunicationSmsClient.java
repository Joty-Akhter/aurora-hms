package com.easyops.hospital.integration.communication;

import com.easyops.hospital.config.LoadBalancedRestTemplateConfig;
import com.easyops.hospital.entity.Patient;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends patient SMS via communication-service internal outbound API (Pondit and other providers
 * are configured only in communication-service).
 */
@Component
@Slf4j
public class CommunicationSmsClient {

    private static final int MAX_MESSAGE_LENGTH = 640;

    private final RestTemplate restTemplate;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${services.communication.base-url:http://communication-service}")
    private String communicationBaseUrl;

    @Value("${services.communication-internal-key:easyops-internal-communication-key}")
    private String communicationInternalKey;

    public CommunicationSmsClient(
            @Qualifier(LoadBalancedRestTemplateConfig.BEAN_NAME) RestTemplate loadBalancedRestTemplate
    ) {
        this.restTemplate = loadBalancedRestTemplate;
    }

    public SmsSendResult sendPatientNotification(Patient patient, String message) {
        if (!smsEnabled) {
            return SmsSendResult.skipped("SMS notifications are disabled");
        }
        if (!isDeliveryConfigured()) {
            return SmsSendResult.failed("Communication service URL or internal service key is not configured");
        }
        if (patient == null) {
            return SmsSendResult.skipped("Patient record is missing");
        }
        if (!Boolean.TRUE.equals(patient.getConsentTextMessaging())) {
            return SmsSendResult.skipped("Patient has not consented to text messaging");
        }
        if (!StringUtils.hasText(patient.getPrimaryPhone())) {
            return SmsSendResult.skipped("Patient primary phone is not available");
        }
        if (!StringUtils.hasText(message)) {
            return SmsSendResult.failed("SMS message is empty");
        }

        String mobile = normalizeBangladeshMobile(patient.getPrimaryPhone());
        if (!StringUtils.hasText(mobile)) {
            return SmsSendResult.failed("Patient primary phone format is invalid");
        }

        String outboundMessage = message.trim();
        if (outboundMessage.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Truncating SMS message from {} to {} characters for communication-service limit",
                    outboundMessage.length(), MAX_MESSAGE_LENGTH);
            outboundMessage = outboundMessage.substring(0, MAX_MESSAGE_LENGTH);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Key", communicationInternalKey.trim());

        Map<String, String> payload = Map.of(
                "recipient", mobile,
                "message", outboundMessage
        );

        String url = trimTrailingSlash(communicationBaseUrl) + "/api/communications/internal/outbound/sms";
        try {
            ResponseEntity<DispatchResponse> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(payload, headers),
                    DispatchResponse.class
            );
            DispatchResponse body = response.getBody();
            if (body != null && "ACCEPTED".equalsIgnoreCase(body.status())) {
                return SmsSendResult.delivered(body.providerReference());
            }
            String detail = body == null ? "empty response" : body.status() + " (" + body.providerName() + ")";
            return SmsSendResult.failed("Communication service rejected SMS: " + detail);
        } catch (RestClientException ex) {
            log.error("Failed to send SMS via communication-service", ex);
            return SmsSendResult.failed("Failed to send SMS: " + ex.getMessage());
        }
    }

    public boolean isDeliveryConfigured() {
        return StringUtils.hasText(communicationBaseUrl) && StringUtils.hasText(communicationInternalKey);
    }

    private String normalizeBangladeshMobile(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        if (digits.startsWith("880")) {
            return digits;
        }
        if (digits.startsWith("0")) {
            return "88" + digits;
        }
        if (digits.startsWith("1") && digits.length() == 10) {
            return "88" + digits;
        }
        return digits;
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

    /** Mirrors communication-service {@code InternalOutboundDispatchResponse} for RestTemplate JSON binding. */
    private record DispatchResponse(String channel, String providerName, String status, String providerReference) {
    }

    @Getter
    @Builder
    public static class SmsSendResult {
        private final boolean delivered;
        private final boolean skipped;
        private final String reason;
        private final String providerReference;
        private final String notificationMethod;

        static SmsSendResult delivered(String providerReference) {
            return SmsSendResult.builder()
                    .delivered(true)
                    .skipped(false)
                    .reason("Delivered")
                    .providerReference(providerReference)
                    .notificationMethod("SMS")
                    .build();
        }

        static SmsSendResult skipped(String reason) {
            return SmsSendResult.builder()
                    .delivered(false)
                    .skipped(true)
                    .reason(reason)
                    .notificationMethod("NONE")
                    .build();
        }

        static SmsSendResult failed(String reason) {
            return SmsSendResult.builder()
                    .delivered(false)
                    .skipped(false)
                    .reason(reason)
                    .notificationMethod("SMS")
                    .build();
        }
    }
}
