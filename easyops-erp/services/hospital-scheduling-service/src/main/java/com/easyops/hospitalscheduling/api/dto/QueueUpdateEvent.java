package com.easyops.hospitalscheduling.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QueueUpdateEvent {
    private String eventId;
    private String eventType; // CHECKED_IN, CANCELLED, NO_SHOW, RESCHEDULED, TOKEN_CHANGED, CREATED
    private UUID appointmentId;
    private UUID patientId;
    private Integer tokenNumber;
    private String status;
    private OffsetDateTime slotStart;
    private OffsetDateTime timestamp;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(OffsetDateTime slotStart) { this.slotStart = slotStart; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
