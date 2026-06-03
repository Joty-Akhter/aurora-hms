package com.easyops.hospitalbilling.domain.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CommunicationInvoiceEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(CommunicationInvoiceEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final boolean commInvoiceEmailEnabled;
    private final String invoiceTopic;
    private final String defaultOrganizationId;

    public CommunicationInvoiceEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${comm.invoice.email.enabled:false}") boolean commInvoiceEmailEnabled,
            @Value("${comm.kafka.invoice-topic:invoice-lifecycle-events}") String invoiceTopic,
            @Value("${comm.default-organization-id:00000000-0000-0000-0000-000000000000}") String defaultOrganizationId
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.commInvoiceEmailEnabled = commInvoiceEmailEnabled;
        this.invoiceTopic = invoiceTopic;
        this.defaultOrganizationId = defaultOrganizationId;
    }

    public void publishInvoiceCreated(Invoice invoice) {
        if (!commInvoiceEmailEnabled) {
            meterRegistry.counter("comm.phase4.producer.invoice.skipped", "reason", "flag_disabled").increment();
            return;
        }
        Map<String, Object> envelope = buildEnvelope(invoice);
        try {
            String body = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(invoiceTopic, String.valueOf(invoice.getId()), body);
            meterRegistry.counter("comm.phase4.producer.invoice.published", "eventType", "invoice.created.v1").increment();
        } catch (Exception ex) {
            // Legacy fallback remains available during rollout window.
            meterRegistry.counter("comm.phase4.producer.invoice.failed", "eventType", "invoice.created.v1").increment();
            log.warn("Communication invoice event publish failed for invoiceId={}", invoice.getId(), ex);
        }
    }

    Map<String, Object> buildEnvelope(Invoice invoice) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId() != null ? invoice.getId().toString() : null);
        payload.put("invoiceNo", invoice.getInvoiceNumber());
        payload.put("patientId", invoice.getPatientId() != null ? invoice.getPatientId().toString() : null);
        payload.put("amount", invoice.getNetAmount() != null ? invoice.getNetAmount().toPlainString() : null);
        payload.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : null);
        // During Phase 4 rollout recipient can be resolved by downstream policy later.
        payload.put("recipientEmail", "");

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", "invoice.created.v1");
        envelope.put("eventVersion", "v1");
        envelope.put("occurredAt", OffsetDateTime.now().toInstant().toString());
        envelope.put("organizationId", defaultOrganizationId);
        envelope.put("entityId", invoice.getId() != null ? invoice.getId().toString() : "");
        envelope.put("actorId", "system:hospital-billing-service");
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);
        return envelope;
    }
}
