package com.easyops.communication.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PonditSmsProvider implements CommunicationProvider {

    private static final Logger log = LoggerFactory.getLogger(PonditSmsProvider.class);

    // Pondit can return negative statuses (e.g. -42 for authorization failure)
    private static final Pattern STATUS_PATTERN = Pattern.compile("\"Status\"\\s*:\\s*\"?(-?\\d+)\"?");
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"Text\"\\s*:\\s*\"([^\"]*)\"");
    // 0=DELIVRD, 4=SENT, 2=PENDING
    private static final Set<String> SUCCESS_STATUSES = Set.of("0", "2", "4");
    // 101=internal server error (retryable)
    private static final Set<String> TRANSIENT_STATUSES = Set.of("101");

    private final RestTemplate restTemplate;
    private final boolean stubEnabled;
    private final String baseUrl;
    private final String sendPath;
    private final String apiKey;
    private final String secretKey;
    private final String senderId;

    /**
     * Test-friendly constructor: defaults to stub mode so unit tests
     * don't attempt real gateway calls.
     */
    public PonditSmsProvider() {
        this(new RestTemplateBuilder(), true, "http://sms3.pondit.com:7788", "/sendtext", "", "", "");
    }

    @Autowired
    public PonditSmsProvider(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${communication.providers.pondit.stub:false}") boolean stubEnabled,
            @Value("${communication.providers.pondit.base-url:http://sms3.pondit.com:7788}") String baseUrl,
            @Value("${communication.providers.pondit.send-path:/sendtext}") String sendPath,
            @Value("${communication.providers.pondit.api-key:}") String apiKey,
            @Value("${communication.providers.pondit.secret-key:}") String secretKey,
            @Value("${communication.providers.pondit.sender-id:}") String senderId
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.stubEnabled = stubEnabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.sendPath = sendPath == null ? "" : sendPath;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.secretKey = secretKey == null ? "" : secretKey;
        this.senderId = senderId == null ? "" : senderId;
    }

    @Override
    public String providerName() {
        return "pondit";
    }

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public ProviderDispatchResult send(ProviderDispatchRequest request) {
        if (stubEnabled) {
            String recipient = request == null ? "" : String.valueOf(request.recipient());
            if (recipient.contains("transient")) {
                throw new IllegalStateException("Transient provider timeout");
            }
            if (recipient.contains("permanent")) {
                throw new IllegalArgumentException("Permanent recipient validation failure");
            }
            return new ProviderDispatchResult(channel(), providerName(), "ACCEPTED", "sms-" + UUID.randomUUID());
        }

        if (request == null) {
            throw new IllegalArgumentException("Permanent validation failure: dispatch request is missing");
        }
        String recipient = request.recipient();
        String message = request.body();
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Permanent validation failure: recipient is missing");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Permanent validation failure: message body is missing");
        }
        if (!hasProviderCredentials()) {
            throw new IllegalStateException("Transient provider error: Pondit credentials are not configured");
        }
        String mobile = normalizeBangladeshMobile(recipient);
        if (!StringUtils.hasText(mobile)) {
            throw new IllegalArgumentException("Permanent validation failure: recipient phone format is invalid");
        }

        try {
            String requestUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(sendPath)
                    .queryParam("apikey", apiKey)
                    .queryParam("secretkey", secretKey)
                    .queryParam("callerID", senderId)
                    .queryParam("toUser", mobile)
                    .queryParam("messageContent", message)
                    .build()
                    .encode()
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
            String body = response.getBody() == null ? "" : response.getBody();
            String providerStatus = extractValue(STATUS_PATTERN, body);
            String providerText = extractValue(TEXT_PATTERN, body);

            if (SUCCESS_STATUSES.contains(providerStatus)) {
                String ref = "pondit-status-" + (providerStatus.isBlank() ? "unknown" : providerStatus);
                return new ProviderDispatchResult(channel(), providerName(), "ACCEPTED", ref);
            }
            if (TRANSIENT_STATUSES.contains(providerStatus)) {
                throw new IllegalStateException("Transient provider timeout: Pondit returned status " + providerStatus + " (" + providerText + ")");
            }
            throw new IllegalArgumentException("Permanent validation failure: Pondit rejected request with status " + providerStatus + " (" + providerText + ")");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Pondit SMS dispatch failed", ex);
            throw new IllegalStateException("Transient provider timeout");
        }
    }

    @Override
    public ProviderHealthStatus health() {
        if (stubEnabled) {
            return new ProviderHealthStatus(providerName(), channel(), "UP", "Pondit adapter running in stub mode");
        }
        if (!hasProviderCredentials()) {
            return new ProviderHealthStatus(providerName(), channel(), "DOWN", "Pondit credentials not configured");
        }
        return new ProviderHealthStatus(providerName(), channel(), "UP", "Pondit provider configured");
    }

    private boolean hasProviderCredentials() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(secretKey) && StringUtils.hasText(senderId);
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

    private String extractValue(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "";
    }
}
