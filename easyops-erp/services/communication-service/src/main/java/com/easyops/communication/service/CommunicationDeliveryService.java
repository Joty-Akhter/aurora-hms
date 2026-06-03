package com.easyops.communication.service;

import com.easyops.communication.dto.CommunicationDeliveryResponse;
import com.easyops.communication.dto.InboundCommunicationEvent;
import com.easyops.communication.entity.CommunicationDelivery;
import com.easyops.communication.entity.CommunicationTemplate;
import com.easyops.communication.provider.CommunicationProvider;
import com.easyops.communication.provider.ProviderDispatchRequest;
import com.easyops.communication.provider.ProviderDispatchResult;
import com.easyops.communication.repository.CommunicationDeliveryRepository;
import com.easyops.communication.repository.CommunicationTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CommunicationDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(CommunicationDeliveryService.class);

    private static final Set<String> RETRIABLE_STATUSES = Set.of("QUEUED", "RETRYING");
    private static final List<Duration> RETRY_BACKOFF = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    );

    private final CommunicationDeliveryRepository deliveryRepository;
    private final CommunicationTemplateRepository templateRepository;
    private final CommunicationEventValidator validator;
    private final TemplateRenderer templateRenderer;
    private final ProviderRouterService providerRouterService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String dlqTopic;
    private final boolean dlqPublishingEnabled;
    private final MeterRegistry meterRegistry;
    private final Timer dispatchLatencyTimer;
    private final int workerBatchSize;

    public CommunicationDeliveryService(
            CommunicationDeliveryRepository deliveryRepository,
            CommunicationTemplateRepository templateRepository,
            CommunicationEventValidator validator,
            TemplateRenderer templateRenderer,
            ProviderRouterService providerRouterService,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${communication.phase6.worker.batch-size:50}") int workerBatchSize,
            @Value("${communication.events.dlq-topic:communication-events-dlq}") String dlqTopic,
            @Value("${communication.phase3.dlq.publishing-enabled:true}") boolean dlqPublishingEnabled
    ) {
        this.deliveryRepository = deliveryRepository;
        this.templateRepository = templateRepository;
        this.validator = validator;
        this.templateRenderer = templateRenderer;
        this.providerRouterService = providerRouterService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.workerBatchSize = workerBatchSize;
        this.dlqTopic = dlqTopic;
        this.dlqPublishingEnabled = dlqPublishingEnabled;
        this.dispatchLatencyTimer = Timer.builder("communication.delivery.dispatch.latency")
                .description("Dispatch latency per channel/provider")
                .register(meterRegistry);
        Gauge.builder("communication.delivery.retry.queue.depth", this::retryQueueDepth)
                .description("Count of delivery records waiting for retry/queue processing")
                .register(meterRegistry);
    }

    @Transactional
    public CommunicationDeliveryResponse ingest(InboundCommunicationEvent event) {
        validator.validate(event);
        MDC.put("correlationId", event.correlationId());
        try {
            RoutingDecision route = resolveRouting(event);
            String idempotencyKey = buildIdempotencyKey(
                    event.eventId(),
                    route.channel(),
                    route.recipient(),
                    route.templateVersion(),
                    route.templateLocale()
            );
            Optional<CommunicationDelivery> existing = deliveryRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                meterRegistry.counter("communication.delivery.ingest.duplicate").increment();
                return toResponse(existing.get());
            }

            CommunicationDelivery delivery = new CommunicationDelivery();
            delivery.setEventId(event.eventId());
            delivery.setCorrelationId(event.correlationId());
            delivery.setEventType(event.eventType());
            delivery.setEventVersion(event.eventVersion());
            delivery.setOrganizationId(event.organizationId());
            delivery.setEntityId(event.entityId());
            delivery.setTemplateKey(route.templateKey());
            delivery.setChannel(route.channel());
            delivery.setRecipient(route.recipient());
            delivery.setTemplateVersion(route.templateVersion());
            delivery.setTemplateLocale(route.templateLocale());
            delivery.setIdempotencyKey(idempotencyKey);
            delivery.setPayloadJson(toJson(event.payload()));
            delivery.setAttemptCount(0);
            delivery.setNextAttemptAt(Instant.now());

            if (route.skipReason() != null) {
                delivery.setStatus("SKIPPED");
                delivery.setFailureCategory("NO_ROUTE");
                delivery.setFailureReason(route.skipReason());
                delivery.setPolicyDecision("BLOCKED");
                delivery.setPolicyReason("NO_ROUTE");
            } else {
                PolicyDecision policy = evaluatePolicy(event.payload(), route.channel());
                delivery.setPolicyDecision(policy.outcome());
                delivery.setPolicyReason(policy.reason());
                if ("BLOCKED".equals(policy.outcome())) {
                    delivery.setStatus("SKIPPED");
                    delivery.setFailureCategory("POLICY_BLOCKED");
                    delivery.setFailureReason(policy.reason());
                    delivery.setNextAttemptAt(null);
                } else if ("DEFERRED".equals(policy.outcome())) {
                    delivery.setStatus("QUEUED");
                    delivery.setFailureCategory("POLICY_DEFERRED");
                    delivery.setFailureReason(policy.reason());
                    delivery.setNextAttemptAt(policy.nextAttemptAt());
                } else {
                    delivery.setStatus("QUEUED");
                }
            }

            CommunicationDelivery saved = deliveryRepository.save(delivery);
            meterRegistry.counter("communication.delivery.ingest.status", "status", saved.getStatus()).increment();
            return toResponse(saved);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Transactional(readOnly = true)
    public Page<CommunicationDeliveryResponse> query(
            String correlationId,
            String eventId,
            String status,
            String channel,
            Pageable pageable
    ) {
        Pageable paged = withoutSort(pageable);
        if (correlationId != null && !correlationId.isBlank()) {
            return deliveryRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId, paged).map(this::toResponse);
        }
        if (eventId != null && !eventId.isBlank()) {
            return deliveryRepository.findByEventIdOrderByCreatedAtDesc(eventId, paged).map(this::toResponse);
        }
        if (status != null && !status.isBlank() && channel != null && !channel.isBlank()) {
            return deliveryRepository.findByStatusAndChannelOrderByCreatedAtDesc(
                    status.trim().toUpperCase(Locale.ROOT),
                    channel.trim().toUpperCase(Locale.ROOT),
                    paged
            ).map(this::toResponse);
        }
        if (status != null && !status.isBlank()) {
            return deliveryRepository.findByStatusOrderByCreatedAtDesc(
                    status.trim().toUpperCase(Locale.ROOT), paged
            ).map(this::toResponse);
        }
        if (channel != null && !channel.isBlank()) {
            return deliveryRepository.findByChannelOrderByCreatedAtDesc(
                    channel.trim().toUpperCase(Locale.ROOT), paged
            ).map(this::toResponse);
        }
        return deliveryRepository.findAllByOrderByCreatedAtDesc(paged).map(this::toResponse);
    }

    /** Repository methods already apply {@code OrderByCreatedAtDesc}; strip client sort to avoid Spring Data conflicts. */
    private static Pageable withoutSort(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
    }

    @Transactional
    public CommunicationDeliveryResponse resendFailed(UUID id, String actorUserId, String reason) {
        CommunicationDelivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        String status = delivery.getStatus() == null ? "" : delivery.getStatus().toUpperCase(Locale.ROOT);
        if (!Set.of("FAILED", "DLQ", "SKIPPED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED/DLQ/SKIPPED deliveries can be resent");
        }
        delivery.setStatus("QUEUED");
        delivery.setFailureCategory(null);
        delivery.setFailureReason(null);
        delivery.setNextAttemptAt(Instant.now());
        CommunicationDelivery saved = deliveryRepository.save(delivery);
        log.info(
                "Delivery resend requested deliveryId={} eventId={} actor={} reason={}",
                id,
                delivery.getEventId(),
                actorUserId,
                reason == null || reason.isBlank() ? "manual_resend" : reason
        );
        meterRegistry.counter("communication.delivery.resend.requested", "previousStatus", status).increment();
        return toResponse(saved);
    }

    @Transactional
    public void processReadyDeliveries() {
        List<CommunicationDelivery> records = deliveryRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        RETRIABLE_STATUSES,
                        Instant.now(),
                        PageRequest.of(0, workerBatchSize)
                );
        for (CommunicationDelivery delivery : records) {
            processSingle(delivery);
        }
    }

    private void processSingle(CommunicationDelivery delivery) {
        MDC.put("correlationId", delivery.getCorrelationId());
        Instant started = Instant.now();
        try {
            CommunicationTemplate template = templateRepository
                    .findByTemplateKeyAndChannelAndLocaleAndVersion(
                            delivery.getTemplateKey(),
                            delivery.getChannel(),
                            delivery.getTemplateLocale() == null ? "en" : delivery.getTemplateLocale(),
                            delivery.getTemplateVersion()
                    )
                    .orElseThrow(() -> new IllegalStateException("No active template for " + delivery.getTemplateKey()));
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(delivery.getPayloadJson(), Map.class);
            CommunicationProvider provider = providerRouterService.resolveByChannel(delivery.getChannel());
            ProviderDispatchResult result = provider.send(new ProviderDispatchRequest(
                    delivery.getRecipient(),
                    templateRenderer.render(template.getSubjectTemplate(), payload),
                    templateRenderer.render(template.getBodyTemplate(), payload)
            ));
            delivery.setStatus("SENT");
            delivery.setProviderName(result.providerName());
            delivery.setProviderReference(result.providerReference());
            delivery.setFailureCategory(null);
            delivery.setFailureReason(null);
            delivery.setLastAttemptAt(Instant.now());
            delivery.setNextAttemptAt(null);
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            deliveryRepository.save(delivery);
            meterRegistry.counter(
                    "communication.delivery.dispatch.status",
                    "status", "SENT",
                    "channel", safeTag(delivery.getChannel()),
                    "provider", safeTag(result.providerName())
            ).increment();
        } catch (RuntimeException ex) {
            handleDeliveryFailure(delivery, ex);
        } catch (java.io.IOException ex) {
            handleDeliveryFailure(delivery, ex);
        } finally {
            dispatchLatencyTimer.record(Duration.between(started, Instant.now()));
            MDC.remove("correlationId");
        }
    }

    private void handleDeliveryFailure(CommunicationDelivery delivery, Exception ex) {
        int nextAttemptCount = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(nextAttemptCount);
        delivery.setLastAttemptAt(Instant.now());

        String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        log.warn(
                "Delivery failed for eventId={} status={} reason={} recipient={} payload={}",
                delivery.getEventId(),
                delivery.getStatus(),
                reason,
                maskRecipient(delivery.getRecipient()),
                maskPayloadForLogs(delivery.getPayloadJson())
        );
        String normalized = reason.toLowerCase(Locale.ROOT);
        boolean permanent = normalized.contains("permanent")
                || normalized.contains("validation")
                || normalized.contains("no active template");

        if (permanent) {
            delivery.setStatus("FAILED");
            delivery.setFailureCategory("PERMANENT");
            delivery.setFailureReason(reason);
            delivery.setNextAttemptAt(null);
            deliveryRepository.save(delivery);
            publishDlq(delivery);
            meterRegistry.counter(
                    "communication.delivery.dispatch.status",
                    "status", delivery.getStatus(),
                    "channel", safeTag(delivery.getChannel()),
                    "provider", safeTag(delivery.getProviderName())
            ).increment();
            return;
        }

        if (nextAttemptCount > RETRY_BACKOFF.size()) {
            delivery.setStatus("DLQ");
            delivery.setFailureCategory("RETRY_EXHAUSTED");
            delivery.setFailureReason(reason);
            delivery.setNextAttemptAt(null);
            deliveryRepository.save(delivery);
            publishDlq(delivery);
            meterRegistry.counter(
                    "communication.delivery.dispatch.status",
                    "status", delivery.getStatus(),
                    "channel", safeTag(delivery.getChannel()),
                    "provider", safeTag(delivery.getProviderName())
            ).increment();
            return;
        }

        Duration backoff = RETRY_BACKOFF.get(nextAttemptCount - 1);
        delivery.setStatus("RETRYING");
        delivery.setFailureCategory("TRANSIENT");
        delivery.setFailureReason(reason);
        delivery.setNextAttemptAt(Instant.now().plus(backoff));
        deliveryRepository.save(delivery);
        meterRegistry.counter(
                "communication.delivery.dispatch.status",
                "status", "RETRYING",
                "channel", safeTag(delivery.getChannel()),
                "provider", safeTag(delivery.getProviderName())
        ).increment();
    }

    private void publishDlq(CommunicationDelivery delivery) {
        if (!dlqPublishingEnabled) {
            return;
        }
        try {
            kafkaTemplate.send(dlqTopic, delivery.getEventId(), toJson(Map.of(
                    "eventId", delivery.getEventId(),
                    "correlationId", delivery.getCorrelationId(),
                    "status", delivery.getStatus(),
                    "failureCategory", delivery.getFailureCategory(),
                    "failureReason", delivery.getFailureReason(),
                    "idempotencyKey", delivery.getIdempotencyKey()
            )));
        } catch (Exception ignored) {
            // DLQ publishing is best-effort; record remains persisted as FAILED/DLQ.
        }
    }

    private RoutingDecision resolveRouting(InboundCommunicationEvent event) {
        String type = event.eventType();
        String normalized = type.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("APPOINTMENT_") || type.startsWith("appointment.")) {
            return resolveWithTemplate(event, "appointment.lifecycle", "SMS", "recipientPhone");
        }
        if (normalized.startsWith("INVOICE_") || type.startsWith("invoice.")) {
            return resolveWithTemplate(event, "invoice.lifecycle", "EMAIL", "recipientEmail");
        }
        return new RoutingDecision(null, "UNKNOWN", "UNKNOWN", null, "en", "No route for eventType " + event.eventType());
    }

    private RoutingDecision resolveWithTemplate(InboundCommunicationEvent event, String templateKey, String channel, String recipientField) {
        String resolvedLocale = resolveLocale(event.payload());
        Object rawRecipient = event.payload().get(recipientField);
        String recipient = rawRecipient == null ? null : String.valueOf(rawRecipient).trim();
        if (recipient == null || recipient.isBlank() || "null".equalsIgnoreCase(recipient)) {
            return new RoutingDecision(templateKey, channel, "", null, resolvedLocale, "Recipient missing");
        }
        Optional<CommunicationTemplate> template = pickTemplate(templateKey, channel, resolvedLocale);
        if (template.isEmpty()) {
            return new RoutingDecision(templateKey, channel, recipient, null, resolvedLocale, "No ACTIVE template found");
        }
        return new RoutingDecision(
                templateKey,
                channel,
                recipient,
                template.get().getVersion(),
                template.get().getLocale(),
                null
        );
    }

    private String buildIdempotencyKey(
            String eventId,
            String channel,
            String recipient,
            Integer templateVersion,
            String templateLocale
    ) {
        String source = eventId + "|" + channel + "|" + recipient + "|" + templateVersion + "|" + templateLocale;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to compute idempotency key", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize payload", e);
        }
    }

    private CommunicationDeliveryResponse toResponse(CommunicationDelivery entity) {
        return new CommunicationDeliveryResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getCorrelationId(),
                entity.getEventType(),
                entity.getEventVersion(),
                entity.getOrganizationId(),
                entity.getEntityId(),
                entity.getTemplateKey(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getTemplateVersion(),
                entity.getTemplateLocale(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getPolicyDecision(),
                entity.getPolicyReason(),
                entity.getFailureCategory(),
                entity.getFailureReason(),
                entity.getProviderName(),
                entity.getProviderReference(),
                entity.getAttemptCount(),
                entity.getNextAttemptAt(),
                entity.getLastAttemptAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private record RoutingDecision(
            String templateKey,
            String channel,
            String recipient,
            Integer templateVersion,
            String templateLocale,
            String skipReason
    ) {
    }

    private record PolicyDecision(
            String outcome,
            String reason,
            Instant nextAttemptAt
    ) {
    }

    private PolicyDecision evaluatePolicy(Map<String, Object> payload, String channel) {
        if (payload.containsKey("consentGranted") && !asBoolean(payload.get("consentGranted"), true)) {
            return new PolicyDecision("BLOCKED", "CONSENT_BLOCKED", null);
        }

        if (payload.containsKey("preferredChannels")) {
            Set<String> preferredChannels = parseChannelSet(payload.get("preferredChannels"));
            if (!preferredChannels.isEmpty() && !preferredChannels.contains(channel.toUpperCase(Locale.ROOT))) {
                return new PolicyDecision("BLOCKED", "CHANNEL_PREFERENCE_BLOCKED", null);
            }
        }

        Instant quietEnd = quietHoursEnd(payload);
        if (quietEnd != null) {
            return new PolicyDecision("DEFERRED", "QUIET_HOURS_DEFERRED", quietEnd);
        }

        return new PolicyDecision("ALLOWED", "ALLOWED", null);
    }

    private Optional<CommunicationTemplate> pickTemplate(String templateKey, String channel, String preferredLocale) {
        List<String> locales = new ArrayList<>();
        if (preferredLocale != null && !preferredLocale.isBlank()) {
            locales.add(preferredLocale.toLowerCase(Locale.ROOT));
        }
        if (!locales.contains("en")) {
            locales.add("en");
        }
        if (!locales.contains("bn")) {
            locales.add("bn");
        }
        for (String locale : locales) {
            Optional<CommunicationTemplate> match = templateRepository.findByTemplateKeyAndChannelAndLocaleAndStatus(
                    templateKey, channel, locale, "ACTIVE"
            );
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private String resolveLocale(Map<String, Object> payload) {
        String recipientLocale = asString(payload.get("recipientLocale"));
        if (recipientLocale != null) {
            return recipientLocale.toLowerCase(Locale.ROOT);
        }
        String orgLocale = asString(payload.get("organizationLocale"));
        if (orgLocale != null) {
            return orgLocale.toLowerCase(Locale.ROOT);
        }
        return "en";
    }

    private Instant quietHoursEnd(Map<String, Object> payload) {
        String start = asString(payload.get("quietHoursStart"));
        String end = asString(payload.get("quietHoursEnd"));
        if (start == null || end == null) {
            return null;
        }
        String timezone = asString(payload.get("timezone"));
        ZoneId zone = ZoneId.of(timezone == null ? "UTC" : timezone);
        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(start);
            endTime = LocalTime.parse(end);
        } catch (RuntimeException ex) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalTime nowTime = now.toLocalTime();
        boolean wrapsMidnight = endTime.isBefore(startTime);
        boolean inQuietHours = wrapsMidnight
                ? !nowTime.isBefore(startTime) || nowTime.isBefore(endTime)
                : !nowTime.isBefore(startTime) && nowTime.isBefore(endTime);
        if (!inQuietHours) {
            return null;
        }
        ZonedDateTime nextEnd = now.with(endTime);
        if (!nextEnd.isAfter(now)) {
            nextEnd = nextEnd.plusDays(1);
        }
        return nextEnd.toInstant();
    }

    private Set<String> parseChannelSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(v -> v.trim().toUpperCase(Locale.ROOT))
                    .filter(v -> !v.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
        }
        String text = value.toString();
        if (text.isBlank()) {
            return Set.of();
        }
        String[] parts = text.split(",");
        Set<String> channels = new java.util.HashSet<>();
        for (String part : parts) {
            String normalized = part.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                channels.add(normalized);
            }
        }
        return channels;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private double retryQueueDepth() {
        return deliveryRepository.countByStatusIn(RETRIABLE_STATUSES);
    }

    private String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return recipient;
        }
        String value = recipient.trim();
        if (value.contains("@")) {
            int at = value.indexOf('@');
            String local = at <= 0 ? "" : value.substring(0, at);
            String domain = value.substring(at);
            String maskedLocal = local.length() <= 2 ? "***" : local.substring(0, 2) + "***";
            return maskedLocal + domain;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "***";
        }
        return "***" + digits.substring(digits.length() - 4);
    }

    private String maskPayloadForLogs(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return payloadJson;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(payloadJson, Map.class);
            if (map.containsKey("recipientPhone")) {
                map.put("recipientPhone", maskRecipient(asString(map.get("recipientPhone"))));
            }
            if (map.containsKey("recipientEmail")) {
                map.put("recipientEmail", maskRecipient(asString(map.get("recipientEmail"))));
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return "{\"masked\":true}";
        }
    }
}
