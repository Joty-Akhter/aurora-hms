package com.easyops.communication.service;

import com.easyops.communication.dto.InboundCommunicationEvent;
import com.easyops.communication.entity.CommunicationTemplate;
import com.easyops.communication.repository.CommunicationDeliveryRepository;
import com.easyops.communication.repository.CommunicationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CommunicationDeliveryServiceIT {

    @Autowired
    private CommunicationDeliveryService deliveryService;

    @Autowired
    private CommunicationTemplateRepository templateRepository;

    @Autowired
    private CommunicationDeliveryRepository deliveryRepository;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        templateRepository.deleteAll();
        templateRepository.save(template("appointment.lifecycle", "SMS", "en", "Hello {{patientName}} on {{appointmentDate}}"));
        templateRepository.save(template("invoice.lifecycle", "EMAIL", "en", "Invoice {{invoiceNo}} for {{patientName}}"));
        templateRepository.save(template("invoice.lifecycle", "EMAIL", "bn", "চালান {{invoiceNo}} {{patientName}}"));
    }

    @Test
    void successPath_movesFromQueuedToSent() {
        InboundCommunicationEvent event = appointmentEvent("evt-success", "corr-success", "+8801700001111");
        deliveryService.ingest(event);

        deliveryService.processReadyDeliveries();

        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void retryThenSuccess_transientFailureThenManualRecovery() {
        InboundCommunicationEvent event = appointmentEvent("evt-retry", "corr-retry", "transient-number");
        deliveryService.ingest(event);
        deliveryService.processReadyDeliveries();

        var firstAttempt = deliveryRepository.findAll().get(0);
        assertThat(firstAttempt.getStatus()).isEqualTo("RETRYING");
        assertThat(firstAttempt.getFailureCategory()).isEqualTo("TRANSIENT");

        firstAttempt.setRecipient("+8801700002222");
        firstAttempt.setNextAttemptAt(Instant.now().minusSeconds(1));
        deliveryRepository.save(firstAttempt);

        deliveryService.processReadyDeliveries();
        var finalAttempt = deliveryRepository.findById(firstAttempt.getId()).orElseThrow();
        assertThat(finalAttempt.getStatus()).isEqualTo("SENT");
    }

    @Test
    void permanentFailure_goesToFailed() {
        InboundCommunicationEvent event = invoiceEvent("evt-permanent", "corr-permanent", "permanent@example.com");
        deliveryService.ingest(event);
        deliveryService.processReadyDeliveries();

        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("FAILED");
        assertThat(delivery.getFailureCategory()).isEqualTo("PERMANENT");
    }

    @Test
    void idempotency_preventsDuplicateSends() {
        InboundCommunicationEvent event = appointmentEvent("evt-idempotent", "corr-idempotent", "+8801700003333");
        deliveryService.ingest(event);
        deliveryService.ingest(event);

        assertThat(deliveryRepository.count()).isEqualTo(1);
    }

    @Test
    void transientFailure_exhaustedRetries_goesToDlq() {
        InboundCommunicationEvent event = appointmentEvent("evt-dlq", "corr-dlq", "transient-number");
        deliveryService.ingest(event);

        for (int i = 0; i < 5; i++) {
            var current = deliveryRepository.findAll().get(0);
            if (current.getNextAttemptAt() != null) {
                current.setNextAttemptAt(Instant.now().minusSeconds(1));
                deliveryRepository.save(current);
            }
            deliveryService.processReadyDeliveries();
        }

        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("DLQ");
        assertThat(delivery.getFailureCategory()).isEqualTo("RETRY_EXHAUSTED");
    }

    @Test
    void consentDenied_isBlockedBeforeDispatch() {
        InboundCommunicationEvent event = appointmentEvent("evt-consent", "corr-consent", "+8801700004444");
        InboundCommunicationEvent denied = new InboundCommunicationEvent(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt(),
                event.organizationId(),
                event.entityId(),
                event.actorId(),
                event.correlationId(),
                Map.of(
                        "patientName", "John",
                        "appointmentDate", "2026-05-10T10:00:00Z",
                        "recipientPhone", "+8801700004444",
                        "consentGranted", false
                )
        );
        deliveryService.ingest(denied);
        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("SKIPPED");
        assertThat(delivery.getPolicyDecision()).isEqualTo("BLOCKED");
        assertThat(delivery.getPolicyReason()).isEqualTo("CONSENT_BLOCKED");
    }

    @Test
    void channelPreferenceBlocked_whenPreferredDoesNotContainRoutedChannel() {
        InboundCommunicationEvent event = appointmentEvent("evt-pref", "corr-pref", "+8801700005555");
        InboundCommunicationEvent blocked = new InboundCommunicationEvent(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt(),
                event.organizationId(),
                event.entityId(),
                event.actorId(),
                event.correlationId(),
                Map.of(
                        "patientName", "John",
                        "appointmentDate", "2026-05-10T10:00:00Z",
                        "recipientPhone", "+8801700005555",
                        "preferredChannels", "EMAIL"
                )
        );
        deliveryService.ingest(blocked);
        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("SKIPPED");
        assertThat(delivery.getPolicyReason()).isEqualTo("CHANNEL_PREFERENCE_BLOCKED");
    }

    @Test
    void quietHours_deferredWithFutureNextAttempt() {
        InboundCommunicationEvent event = appointmentEvent("evt-quiet", "corr-quiet", "+8801700006666");
        InboundCommunicationEvent deferred = new InboundCommunicationEvent(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt(),
                event.organizationId(),
                event.entityId(),
                event.actorId(),
                event.correlationId(),
                Map.of(
                        "patientName", "John",
                        "appointmentDate", "2026-05-10T10:00:00Z",
                        "recipientPhone", "+8801700006666",
                        "quietHoursStart", "00:00",
                        "quietHoursEnd", "23:59",
                        "timezone", "UTC"
                )
        );
        deliveryService.ingest(deferred);
        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getStatus()).isEqualTo("QUEUED");
        assertThat(delivery.getPolicyDecision()).isEqualTo("DEFERRED");
        assertThat(delivery.getPolicyReason()).isEqualTo("QUIET_HOURS_DEFERRED");
        assertThat(delivery.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void localeResolution_prefersRecipientLocaleThenFallback() {
        InboundCommunicationEvent event = new InboundCommunicationEvent(
                "evt-locale",
                "invoice.created.v1",
                "v1",
                Instant.now(),
                "org-1",
                "inv-1",
                "actor-1",
                "corr-locale",
                Map.of(
                        "patientName", "John",
                        "invoiceNo", "INV-2",
                        "recipientEmail", "john@example.com",
                        "recipientLocale", "bn"
                )
        );
        deliveryService.ingest(event);
        var delivery = deliveryRepository.findAll().get(0);
        assertThat(delivery.getTemplateLocale()).isEqualTo("bn");
        deliveryService.processReadyDeliveries();
        var sent = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo("SENT");
    }

    private CommunicationTemplate template(String key, String channel, String locale, String body) {
        CommunicationTemplate t = new CommunicationTemplate();
        t.setTemplateKey(key);
        t.setChannel(channel);
        t.setLocale(locale);
        t.setVersion(1);
        t.setStatus("ACTIVE");
        t.setSubjectTemplate(channel.equals("EMAIL") ? "Subject {{invoiceNo}}" : null);
        t.setBodyTemplate(body);
        t.setVariablesSchema(channel.equals("EMAIL")
                ? "{\"required\":[\"patientName\",\"invoiceNo\"]}"
                : "{\"required\":[\"patientName\",\"appointmentDate\"]}");
        t.setCreatedBy("test");
        return t;
    }

    private InboundCommunicationEvent appointmentEvent(String eventId, String correlationId, String recipientPhone) {
        return new InboundCommunicationEvent(
                eventId,
                "appointment.created.v1",
                "v1",
                Instant.now(),
                "org-1",
                "appt-1",
                "actor-1",
                correlationId,
                Map.of(
                        "patientName", "John",
                        "appointmentDate", "2026-05-10T10:00:00Z",
                        "recipientPhone", recipientPhone
                )
        );
    }

    private InboundCommunicationEvent invoiceEvent(String eventId, String correlationId, String recipientEmail) {
        return new InboundCommunicationEvent(
                eventId,
                "invoice.created.v1",
                "v1",
                Instant.now(),
                "org-1",
                "inv-1",
                "actor-1",
                correlationId,
                Map.of(
                        "patientName", "John",
                        "invoiceNo", "INV-1",
                        "recipientEmail", recipientEmail
                )
        );
    }
}
