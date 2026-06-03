package com.easyops.hospitalscheduling.events;

import com.easyops.hospitalscheduling.config.SchedulingTimeZoneResolver;
import com.easyops.hospitalscheduling.domain.appointment.Appointment;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResource;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CommunicationAppointmentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(CommunicationAppointmentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final boolean commAppointmentSmsEnabled;
    private final String appointmentTopic;
    private final String defaultOrganizationId;
    private final SchedulingResourceRepository schedulingResourceRepository;
    private final SchedulingTimeZoneResolver schedulingTimeZoneResolver;
    private final String appointmentSmsLocationLine;
    private final String appointmentSmsMapsUrl;

    public CommunicationAppointmentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${comm.appointment.sms.enabled:false}") boolean commAppointmentSmsEnabled,
            @Value("${comm.kafka.appointment-topic:appointment-lifecycle-events}") String appointmentTopic,
            @Value("${comm.default-organization-id:00000000-0000-0000-0000-000000000000}") String defaultOrganizationId,
            SchedulingResourceRepository schedulingResourceRepository,
            SchedulingTimeZoneResolver schedulingTimeZoneResolver,
            @Value("${comm.appointment.sms.location-line:601 in Aurora Specialized Hospital Ltd. Dhaka}") String appointmentSmsLocationLine,
            @Value("${comm.appointment.sms.maps-url:https://maps.app.goo.gl/rVnySGoJPEy9jKwTA}") String appointmentSmsMapsUrl
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.commAppointmentSmsEnabled = commAppointmentSmsEnabled;
        this.appointmentTopic = appointmentTopic;
        this.defaultOrganizationId = defaultOrganizationId;
        this.schedulingResourceRepository = schedulingResourceRepository;
        this.schedulingTimeZoneResolver = schedulingTimeZoneResolver;
        this.appointmentSmsLocationLine = appointmentSmsLocationLine;
        this.appointmentSmsMapsUrl = appointmentSmsMapsUrl;
    }

    public void publish(AppointmentEvent.Type type, Appointment appointment) {
        if (!commAppointmentSmsEnabled) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "flag_disabled").increment();
            return;
        }
        if (type != AppointmentEvent.Type.CREATED && type != AppointmentEvent.Type.RESCHEDULED) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "unsupported_event_type").increment();
            return;
        }
        String phone = normalizeRecipientPhone(appointment.getNotificationPatientPhone());
        if (phone.isBlank()) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "no_recipient_phone").increment();
            return;
        }
        String eventType = resolveEventType(type);
        if (kafkaTemplate == null) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "kafka_template_unavailable").increment();
            log.warn("Skipping appointment {} event {}: KafkaTemplate is not configured", appointment.getId(), eventType);
            return;
        }
        if (objectMapper == null) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "object_mapper_unavailable").increment();
            log.warn("Skipping appointment {} event {}: ObjectMapper is not configured", appointment.getId(), eventType);
            return;
        }
        Map<String, Object> envelope = buildEnvelope(type, appointment);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        Object appointmentDateVal = payload != null ? payload.get("appointmentDate") : null;
        if (appointmentDateVal == null || String.valueOf(appointmentDateVal).isBlank()) {
            meterRegistry.counter("comm.phase4.producer.appointment.skipped", "reason", "missing_appointment_date").increment();
            log.warn("Skipping appointment {} event {}: appointmentDate missing after fallbacks", appointment.getId(), eventType);
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(appointmentTopic, String.valueOf(appointment.getId()), body);
            meterRegistry.counter("comm.phase4.producer.appointment.published", "eventType", eventType).increment();
        } catch (Exception ex) {
            meterRegistry.counter("comm.phase4.producer.appointment.failed", "eventType", eventType).increment();
            log.warn("Communication appointment event publish failed for appointmentId={} eventType={}",
                    appointment.getId(), eventType, ex);
        }
    }

    String resolveEventType(AppointmentEvent.Type type) {
        return switch (type) {
            case CREATED -> "appointment.created.v1";
            case RESCHEDULED, CANCELLED, CHECKED_IN, NO_SHOW, COMPLETED -> "appointment.updated.v1";
        };
    }

    Map<String, Object> buildEnvelope(AppointmentEvent.Type type, Appointment appointment) {
        String eventType = resolveEventType(type);
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ZoneId zone = schedulingTimeZoneResolver.getDefaultZone();
        DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US).withZone(zone);

        String doctorName = "";
        if (appointment.getResourceId() != null) {
            doctorName = schedulingResourceRepository.findById(appointment.getResourceId())
                    .map(SchedulingResource::getName)
                    .orElse("");
        }
        if (doctorName.isBlank()) {
            doctorName = "your doctor";
        }

        String patientName = appointment.getNotificationPatientName();
        if (patientName == null || patientName.isBlank()) {
            patientName = "Patient";
        } else {
            patientName = patientName.trim();
        }

        String phone = appointment.getNotificationPatientPhone() == null ? "" : appointment.getNotificationPatientPhone().trim();

        String reportTimeWindow;
        if (appointment.getSlotStart() != null && appointment.getSlotEnd() != null) {
            reportTimeWindow = clockFmt.format(appointment.getSlotStart().toInstant())
                    + " to "
                    + clockFmt.format(appointment.getSlotEnd().toInstant());
        } else if (appointment.getSlotStart() != null) {
            reportTimeWindow = clockFmt.format(appointment.getSlotStart().toInstant());
        } else if (appointment.getSlotEnd() != null) {
            reportTimeWindow = clockFmt.format(appointment.getSlotEnd().toInstant());
        } else {
            reportTimeWindow = "time to be confirmed";
        }

        String tokenLabel = formatTokenLabel(appointment.getTokenNumber());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointment.getId() != null ? appointment.getId().toString() : null);
        payload.put("patientId", appointment.getPatientId() != null ? appointment.getPatientId().toString() : null);
        payload.put("patientName", patientName);
        String appointmentDateStr = appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().toString()
                : null;
        if (appointmentDateStr == null && appointment.getSlotStart() != null) {
            appointmentDateStr = appointment.getSlotStart().toLocalDate().toString();
        }
        if (appointmentDateStr == null && appointment.getSlotEnd() != null) {
            appointmentDateStr = appointment.getSlotEnd().toLocalDate().toString();
        }
        payload.put("appointmentDate", appointmentDateStr);
        payload.put("slotStart", appointment.getSlotStart() != null ? appointment.getSlotStart().toString() : null);
        payload.put("slotEnd", appointment.getSlotEnd() != null ? appointment.getSlotEnd().toString() : null);
        payload.put("status", appointment.getStatus());
        payload.put("recipientPhone", normalizeRecipientPhone(phone));
        payload.put("doctorName", doctorName);
        payload.put("tokenLabel", tokenLabel);
        payload.put("reportTimeWindow", reportTimeWindow);
        payload.put("locationLine", appointmentSmsLocationLine);
        payload.put("mapsUrl", appointmentSmsMapsUrl);
        payload.put("lifecycleKind", switch (type) {
            case CREATED -> "CREATED";
            case RESCHEDULED -> "RESCHEDULED";
            default -> "UNKNOWN";
        });

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", "v1");
        envelope.put("occurredAt", OffsetDateTime.now().toInstant().toString());
        envelope.put("organizationId", defaultOrganizationId);
        envelope.put("entityId", appointment.getId() != null ? appointment.getId().toString() : "");
        envelope.put("actorId", appointment.getBookedBy() != null ? appointment.getBookedBy().toString() : "system:hospital-scheduling-service");
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);
        return envelope;
    }

    private static String formatTokenLabel(Integer tokenNumber) {
        if (tokenNumber == null || tokenNumber <= 0) {
            return "SlNo-000";
        }
        return String.format("SlNo-%03d", tokenNumber);
    }

    /** Digits-only BD numbers are normalized to E.164 (+880…) for SMS gateways. */
    public static String normalizeRecipientPhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        if (digits.startsWith("880") && digits.length() >= 12) {
            return "+" + digits;
        }
        if (digits.startsWith("0") && digits.length() >= 10) {
            return "+880" + digits.substring(1);
        }
        if (digits.length() == 10 || digits.length() == 11) {
            return "+880" + digits;
        }
        return trimmed.startsWith("+") ? trimmed : "+" + digits;
    }
}
