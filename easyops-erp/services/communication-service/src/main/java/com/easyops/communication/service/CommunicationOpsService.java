package com.easyops.communication.service;

import com.easyops.communication.dto.OpsAlertStatusResponse;
import com.easyops.communication.dto.ProviderSecretStatusResponse;
import com.easyops.communication.dto.TestSmsSendResponse;
import com.easyops.communication.provider.ProviderDispatchRequest;
import com.easyops.communication.provider.ProviderHealthStatus;
import com.easyops.communication.provider.ProviderDispatchResult;
import com.easyops.communication.repository.CommunicationDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CommunicationOpsService {

    private static final List<String> RETRIABLE_STATUSES = List.of("QUEUED", "RETRYING");
    private static final List<String> FAILURE_STATUSES = List.of("FAILED", "DLQ");

    private final CommunicationDeliveryRepository deliveryRepository;
    private final ProviderRouterService providerRouterService;
    private final int backlogWarnThreshold;
    private final int backlogCriticalThreshold;
    private final double failureWarnRatio;
    private final double failureCriticalRatio;
    private final String ponditApiKey;
    private final String ponditSecretKey;
    private final String ponditSenderId;
    private final String smtpUsername;
    private final String smtpPassword;

    public CommunicationOpsService(
            CommunicationDeliveryRepository deliveryRepository,
            ProviderRouterService providerRouterService,
            @Value("${communication.phase6.alerts.backlog.warn:25}") int backlogWarnThreshold,
            @Value("${communication.phase6.alerts.backlog.critical:50}") int backlogCriticalThreshold,
            @Value("${communication.phase6.alerts.failure-ratio.warn:0.20}") double failureWarnRatio,
            @Value("${communication.phase6.alerts.failure-ratio.critical:0.40}") double failureCriticalRatio,
            @Value("${communication.providers.pondit.api-key:}") String ponditApiKey,
            @Value("${communication.providers.pondit.secret-key:}") String ponditSecretKey,
            @Value("${communication.providers.pondit.sender-id:}") String ponditSenderId,
            @Value("${communication.providers.smtp.username:}") String smtpUsername,
            @Value("${communication.providers.smtp.password:}") String smtpPassword
    ) {
        this.deliveryRepository = deliveryRepository;
        this.providerRouterService = providerRouterService;
        this.backlogWarnThreshold = backlogWarnThreshold;
        this.backlogCriticalThreshold = backlogCriticalThreshold;
        this.failureWarnRatio = failureWarnRatio;
        this.failureCriticalRatio = failureCriticalRatio;
        this.ponditApiKey = ponditApiKey;
        this.ponditSecretKey = ponditSecretKey;
        this.ponditSenderId = ponditSenderId;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    public OpsAlertStatusResponse evaluateAlerts() {
        List<OpsAlertStatusResponse.AlertItem> alerts = new ArrayList<>();
        long retryBacklog = deliveryRepository.countByStatusIn(RETRIABLE_STATUSES);
        alerts.add(alertItem(
                "retry_backlog",
                retryBacklog >= backlogCriticalThreshold ? "CRITICAL" : retryBacklog >= backlogWarnThreshold ? "WARN" : "OK",
                retryBacklog >= backlogWarnThreshold,
                "Current queued/retrying backlog is " + retryBacklog
        ));

        Instant windowStart = Instant.now().minus(15, ChronoUnit.MINUTES);
        long windowTotal = deliveryRepository.countByCreatedAtAfter(windowStart);
        long windowFailures = deliveryRepository.countByStatusInAndCreatedAtAfter(FAILURE_STATUSES, windowStart);
        double ratio = windowTotal == 0 ? 0.0d : (double) windowFailures / (double) windowTotal;
        alerts.add(alertItem(
                "failure_ratio_15m",
                ratio >= failureCriticalRatio ? "CRITICAL" : ratio >= failureWarnRatio ? "WARN" : "OK",
                ratio >= failureWarnRatio,
                String.format(Locale.ROOT, "Failure ratio in 15m window is %.2f (%d/%d)", ratio, windowFailures, windowTotal)
        ));

        boolean providerDown = providerRouterService.healthStatuses().stream()
                .anyMatch(status -> !"UP".equalsIgnoreCase(status.status()));
        alerts.add(alertItem(
                "provider_health",
                providerDown ? "CRITICAL" : "OK",
                providerDown,
                providerDown ? "At least one provider health check is not UP" : "All providers healthy"
        ));

        return new OpsAlertStatusResponse(Instant.now(), alerts);
    }

    public ProviderSecretStatusResponse providerSecretStatus() {
        List<ProviderSecretStatusResponse.ProviderSecretItem> items = providerRouterService.healthStatuses().stream()
                .map(this::toSecretItem)
                .toList();
        return new ProviderSecretStatusResponse(items);
    }

    public TestSmsSendResponse sendTestSms(String recipient) {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Dhaka"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String body = "This is a test SMS at " + now;
        ProviderDispatchResult result = sendTransactionalSms(recipient, body);
        return new TestSmsSendResponse(body, result.channel(), result.providerName(), result.status(), result.providerReference());
    }

    /** Service-to-service SMS (password reset OTP, etc.). */
    public ProviderDispatchResult sendTransactionalSms(String recipient, String body) {
        return providerRouterService
                .resolveByChannel("SMS")
                .send(new ProviderDispatchRequest(recipient, "otp", body));
    }

    /** Service-to-service email (password reset OTP, etc.). SMTP adapter may stub in tests. */
    public ProviderDispatchResult sendTransactionalEmail(String recipient, String subject, String body) {
        return providerRouterService
                .resolveByChannel("EMAIL")
                .send(new ProviderDispatchRequest(recipient, subject, body));
    }

    private ProviderSecretStatusResponse.ProviderSecretItem toSecretItem(ProviderHealthStatus status) {
        if ("pondit".equalsIgnoreCase(status.providerName())) {
            return new ProviderSecretStatusResponse.ProviderSecretItem(
                    status.providerName(),
                    status.channel(),
                    isConfigured(ponditApiKey) && isConfigured(ponditSecretKey) && isConfigured(ponditSenderId),
                    "env-or-secret-manager"
            );
        }
        if ("smtp".equalsIgnoreCase(status.providerName())) {
            return new ProviderSecretStatusResponse.ProviderSecretItem(
                    status.providerName(),
                    status.channel(),
                    isConfigured(smtpUsername) && isConfigured(smtpPassword),
                    "env-or-secret-manager"
            );
        }
        return new ProviderSecretStatusResponse.ProviderSecretItem(
                status.providerName(),
                status.channel(),
                false,
                "unknown"
        );
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private OpsAlertStatusResponse.AlertItem alertItem(String key, String level, boolean triggered, String message) {
        return new OpsAlertStatusResponse.AlertItem(key, level, triggered, message);
    }
}
