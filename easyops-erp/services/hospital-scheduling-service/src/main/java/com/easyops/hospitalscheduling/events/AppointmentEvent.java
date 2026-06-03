package com.easyops.hospitalscheduling.events;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event for appointment lifecycle. Published after create, reschedule, cancel, check-in, no-show.
 * Event name (type): appointment.created | appointment.rescheduled | appointment.cancelled |
 * appointment.checked_in | appointment.no_show.
 * Consumers (e.g. Billing for no-show fee) can subscribe via @EventListener(AppointmentEvent.class).
 */
public class AppointmentEvent {

    public enum Type {
        CREATED,
        RESCHEDULED,
        CANCELLED,
        CHECKED_IN,
        NO_SHOW,
        COMPLETED
    }

    private final Type type;
    private final UUID appointmentId;
    private final UUID reservationId;
    private final UUID patientId;
    private final UUID resourceId;
    private final String status;
    private final OffsetDateTime slotStart;
    private final OffsetDateTime slotEnd;
    private final LocalDate appointmentDate;
    private final OffsetDateTime timestamp;

    public AppointmentEvent(Type type, UUID appointmentId, UUID reservationId, UUID patientId,
                            UUID resourceId, String status, OffsetDateTime slotStart, OffsetDateTime slotEnd,
                            LocalDate appointmentDate, OffsetDateTime timestamp) {
        this.type = type;
        this.appointmentId = appointmentId;
        this.reservationId = reservationId;
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.status = status;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.appointmentDate = appointmentDate;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }

    public Type getType() { return type; }
    public UUID getAppointmentId() { return appointmentId; }
    public UUID getReservationId() { return reservationId; }
    public UUID getPatientId() { return patientId; }
    public UUID getResourceId() { return resourceId; }
    public String getStatus() { return status; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public OffsetDateTime getSlotEnd() { return slotEnd; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public OffsetDateTime getTimestamp() { return timestamp; }

    /** Event name for logging / outbound: appointment.created, appointment.rescheduled, etc. */
    public String getEventName() {
        return "appointment." + type.name().toLowerCase();
    }
}
