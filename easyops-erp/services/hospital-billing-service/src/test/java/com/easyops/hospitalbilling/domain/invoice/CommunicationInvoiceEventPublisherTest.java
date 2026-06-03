package com.easyops.hospitalbilling.domain.invoice;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommunicationInvoiceEventPublisherTest {

    @Test
    void buildsCanonicalInvoiceEnvelopeWhenEnabled() {
        var meterRegistry = new SimpleMeterRegistry();
        CommunicationInvoiceEventPublisher publisher = new CommunicationInvoiceEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "invoice-lifecycle-events",
                "00000000-0000-0000-0000-000000000000"
        );

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-001");
        invoice.setPatientId(UUID.randomUUID());
        invoice.setNetAmount(new BigDecimal("123.45"));
        invoice.setDueDate(LocalDate.of(2026, 5, 15));

        Map<String, Object> envelope = publisher.buildEnvelope(invoice);
        assertThat(envelope.get("eventType")).isEqualTo("invoice.created.v1");
        assertThat(envelope.get("eventVersion")).isEqualTo("v1");
        assertThat(envelope).containsKeys("eventId", "occurredAt", "organizationId", "entityId", "actorId", "correlationId", "payload");
    }
}
