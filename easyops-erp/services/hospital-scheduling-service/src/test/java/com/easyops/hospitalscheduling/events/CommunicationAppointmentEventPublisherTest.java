package com.easyops.hospitalscheduling.events;

import com.easyops.hospitalscheduling.config.SchedulingTimeZoneResolver;
import com.easyops.hospitalscheduling.domain.appointment.Appointment;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunicationAppointmentEventPublisherTest {

    @Test
    void buildsCanonicalAppointmentEnvelopeWhenEnabled() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setResourceId(UUID.randomUUID());
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 1));
        appointment.setSlotStart(OffsetDateTime.parse("2026-05-01T10:00:00+06:00"));
        appointment.setSlotEnd(OffsetDateTime.parse("2026-05-01T14:00:00+06:00"));
        appointment.setStatus("BOOKED");
        appointment.setTokenNumber(4);
        appointment.setNotificationPatientName("Test Patient");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CREATED, appointment);
        assertThat(envelope.get("eventType")).isEqualTo("appointment.created.v1");
        assertThat(envelope.get("eventVersion")).isEqualTo("v1");
        assertThat(envelope).containsKeys("eventId", "occurredAt", "organizationId", "entityId", "actorId", "correlationId", "payload");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("patientName")).isEqualTo("Test Patient");
        assertThat(payload.get("recipientPhone")).isEqualTo("+8801700000000");
        assertThat(payload.get("tokenLabel")).isEqualTo("SlNo-004");
        assertThat(String.valueOf(payload.get("reportTimeWindow"))).contains(" to ");
        assertThat(String.valueOf(payload.get("reportTimeWindow"))).doesNotStartWith("between ");
        assertThat(payload.get("lifecycleKind")).isEqualTo("CREATED");
    }

    @Test
    void buildEnvelope_forRescheduled_usesAppointmentUpdatedEventType() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setResourceId(UUID.randomUUID());
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 1));
        appointment.setSlotStart(OffsetDateTime.parse("2026-05-01T10:00:00+06:00"));
        appointment.setSlotEnd(OffsetDateTime.parse("2026-05-01T14:00:00+06:00"));
        appointment.setStatus("CONFIRMED");
        appointment.setTokenNumber(4);
        appointment.setNotificationPatientName("Test Patient");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.RESCHEDULED, appointment);
        assertThat(envelope.get("eventType")).isEqualTo("appointment.updated.v1");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("lifecycleKind")).isEqualTo("RESCHEDULED");
    }

    @Test
    void buildEnvelope_fallsBackAppointmentDateFromSlotStartWhenDateMissing() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAppointmentDate(null);
        appointment.setSlotStart(OffsetDateTime.parse("2026-05-01T10:00:00+06:00"));
        appointment.setSlotEnd(OffsetDateTime.parse("2026-05-01T14:00:00+06:00"));
        appointment.setNotificationPatientName("A");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CREATED, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("appointmentDate")).isEqualTo("2026-05-01");
    }

    @Test
    void buildEnvelope_forNonCommLifecycleTypes_mapsLifecycleKindUnknown() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 1));
        appointment.setSlotStart(OffsetDateTime.parse("2026-05-01T10:00:00+06:00"));
        appointment.setSlotEnd(OffsetDateTime.parse("2026-05-01T14:00:00+06:00"));
        appointment.setNotificationPatientName("A");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CANCELLED, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("lifecycleKind")).isEqualTo("UNKNOWN");
    }

    @Test
    void buildEnvelope_fallsBackAppointmentDateFromSlotEndWhenStartMissing() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAppointmentDate(null);
        appointment.setSlotStart(null);
        appointment.setSlotEnd(OffsetDateTime.parse("2026-06-02T14:00:00+06:00"));
        appointment.setNotificationPatientName("A");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CREATED, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("appointmentDate")).isEqualTo("2026-06-02");
    }

    @Test
    void buildEnvelope_usesSingleClockTimeWhenOnlySlotStartPresent() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 1));
        appointment.setSlotStart(OffsetDateTime.parse("2026-05-01T10:00:00+06:00"));
        appointment.setSlotEnd(null);
        appointment.setNotificationPatientName("A");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CREATED, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(String.valueOf(payload.get("reportTimeWindow"))).doesNotContain(" to ");
    }

    @Test
    void buildEnvelope_usesSingleClockTimeWhenOnlySlotEndPresent() {
        var meterRegistry = new SimpleMeterRegistry();
        SchedulingResourceRepository resourceRepository = mock(SchedulingResourceRepository.class);
        when(resourceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        CommunicationAppointmentEventPublisher publisher = new CommunicationAppointmentEventPublisher(
                null,
                null,
                meterRegistry,
                true,
                "appointment-lifecycle-events",
                "00000000-0000-0000-0000-000000000000",
                resourceRepository,
                new SchedulingTimeZoneResolver("Asia/Dhaka"),
                "601 in Aurora Specialized Hospital Ltd. Dhaka",
                "https://maps.app.goo.gl/rVnySGoJPEy9jKwTA"
        );

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 1));
        appointment.setSlotStart(null);
        appointment.setSlotEnd(OffsetDateTime.parse("2026-05-01T14:00:00+06:00"));
        appointment.setNotificationPatientName("A");
        appointment.setNotificationPatientPhone("+8801700000000");

        Map<String, Object> envelope = publisher.buildEnvelope(AppointmentEvent.Type.CREATED, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(String.valueOf(payload.get("reportTimeWindow"))).doesNotContain(" to ");
    }
}
