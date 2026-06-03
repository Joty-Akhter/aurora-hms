package com.easyops.communication.service;

import com.easyops.communication.dto.InboundCommunicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class CommunicationEventValidator {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "APPOINTMENT_CREATED",
            "APPOINTMENT_CONFIRMED",
            "APPOINTMENT_CANCELLED",
            "INVOICE_CREATED",
            "INVOICE_OVERDUE",
            "INVOICE_PAID",
            "appointment.created.v1",
            "appointment.updated.v1",
            "invoice.created.v1"
    );

    public void validate(InboundCommunicationEvent event) {
        if (!"v1".equalsIgnoreCase(event.eventVersion())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported eventVersion: " + event.eventVersion());
        }
        if (!SUPPORTED_EVENT_TYPES.contains(event.eventType().toUpperCase(Locale.ROOT))
                && !SUPPORTED_EVENT_TYPES.contains(event.eventType())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported eventType: " + event.eventType());
        }
        validatePayloadFields(event);
    }

    private void validatePayloadFields(InboundCommunicationEvent event) {
        Map<String, Object> payload = event.payload();
        if (isAppointmentEvent(event.eventType())) {
            require(payload, "patientName");
            require(payload, "appointmentDate");
            require(payload, "recipientPhone");
            return;
        }
        require(payload, "invoiceNo");
        require(payload, "patientName");
        require(payload, "recipientEmail");
    }

    private boolean isAppointmentEvent(String eventType) {
        String normalized = eventType.toUpperCase(Locale.ROOT);
        return normalized.startsWith("APPOINTMENT_")
                || eventType.startsWith("appointment.");
    }

    private void require(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Malformed payload: missing " + key);
        }
    }
}
