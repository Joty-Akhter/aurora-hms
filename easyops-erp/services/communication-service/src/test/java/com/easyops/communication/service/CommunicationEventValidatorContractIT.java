package com.easyops.communication.service;

import com.easyops.communication.dto.InboundCommunicationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class CommunicationEventValidatorContractIT {

    @Autowired
    private CommunicationEventValidator validator;

    @Test
    void acceptsAppointmentCreatedV1FromProducerContract() {
        InboundCommunicationEvent event = new InboundCommunicationEvent(
                "evt-appointment-1",
                "appointment.created.v1",
                "v1",
                Instant.now(),
                "00000000-0000-0000-0000-000000000000",
                "appt-1",
                "system:hospital-scheduling-service",
                "corr-appt-1",
                Map.of(
                        "appointmentId", "appt-1",
                        "patientId", "pat-1",
                        "patientName", "Test Patient",
                        "appointmentDate", "2026-05-01",
                        "recipientPhone", "+8801700000000"
                )
        );

        assertThatCode(() -> validator.validate(event)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAppointmentUpdatedV1FromSchedulingReschedule() {
        InboundCommunicationEvent event = new InboundCommunicationEvent(
                "evt-appointment-2",
                "appointment.updated.v1",
                "v1",
                Instant.now(),
                "00000000-0000-0000-0000-000000000000",
                "appt-2",
                "system:hospital-scheduling-service",
                "corr-appt-2",
                Map.of(
                        "appointmentId", "appt-2",
                        "patientId", "pat-2",
                        "patientName", "Test Patient",
                        "appointmentDate", "2026-05-02",
                        "recipientPhone", "+8801700000001"
                )
        );

        assertThatCode(() -> validator.validate(event)).doesNotThrowAnyException();
    }

    @Test
    void acceptsInvoiceCreatedV1FromProducerContract() {
        InboundCommunicationEvent event = new InboundCommunicationEvent(
                "evt-invoice-1",
                "invoice.created.v1",
                "v1",
                Instant.now(),
                "00000000-0000-0000-0000-000000000000",
                "inv-1",
                "system:hospital-billing-service",
                "corr-inv-1",
                Map.of(
                        "invoiceId", "inv-1",
                        "invoiceNo", "INV-1",
                        "patientId", "pat-1",
                        "patientName", "John",
                        "recipientEmail", "billing@example.com"
                )
        );

        assertThatCode(() -> validator.validate(event)).doesNotThrowAnyException();
    }
}
