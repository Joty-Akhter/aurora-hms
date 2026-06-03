package com.easyops.hospitalscheduling.events;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event for planned admission lifecycle. Published when status becomes RESERVED or CONVERTED.
 * Event name: planned_admission.reserved | planned_admission.converted.
 * Consumers (Portal BFF, analytics) can subscribe via @EventListener(PlannedAdmissionEvent.class).
 */
public class PlannedAdmissionEvent {

    public enum Type {
        RESERVED,
        CONVERTED
    }

    private final Type type;
    private final UUID plannedAdmissionId;
    private final UUID patientId;
    private final String status;
    private final LocalDate preferredDate;
    private final UUID reservationId;
    private final UUID bedGroupResourceId;
    private final OffsetDateTime expiresAt;
    private final OffsetDateTime timestamp;

    public PlannedAdmissionEvent(Type type, UUID plannedAdmissionId, UUID patientId, String status,
                                 LocalDate preferredDate, UUID reservationId, UUID bedGroupResourceId,
                                 OffsetDateTime expiresAt, OffsetDateTime timestamp) {
        this.type = type;
        this.plannedAdmissionId = plannedAdmissionId;
        this.patientId = patientId;
        this.status = status;
        this.preferredDate = preferredDate;
        this.reservationId = reservationId;
        this.bedGroupResourceId = bedGroupResourceId;
        this.expiresAt = expiresAt;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }

    public Type getType() { return type; }
    public UUID getPlannedAdmissionId() { return plannedAdmissionId; }
    public UUID getPatientId() { return patientId; }
    public String getStatus() { return status; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public UUID getReservationId() { return reservationId; }
    public UUID getBedGroupResourceId() { return bedGroupResourceId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getTimestamp() { return timestamp; }

    /** Event name for logging / outbound: planned_admission.reserved, planned_admission.converted */
    public String getEventName() {
        return "planned_admission." + type.name().toLowerCase();
    }
}
