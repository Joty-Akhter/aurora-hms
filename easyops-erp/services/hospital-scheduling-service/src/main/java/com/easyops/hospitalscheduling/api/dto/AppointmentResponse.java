package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AppointmentResponse {

    private UUID id;
    private UUID reservationId;
    private UUID patientId;
    private UUID resourceId;
    private UUID clinicOrLocationId;
    private LocalDate appointmentDate;
    private OffsetDateTime slotStart;
    private OffsetDateTime slotEnd;
    private String appointmentType;
    private String status;
    private UUID visitId;
    private Integer tokenNumber;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private String bookingChannel;
    private UUID bookedBy;
    private UUID slotTemplateId;
    private String sessionShift;
    private String sessionLabel;
    /** Snapshot phone used for SMS at booking time. */
    private String notificationPatientPhone;
    private String notificationPatientName;
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getClinicOrLocationId() { return clinicOrLocationId; }
    public void setClinicOrLocationId(UUID clinicOrLocationId) { this.clinicOrLocationId = clinicOrLocationId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public OffsetDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(OffsetDateTime slotStart) { this.slotStart = slotStart; }
    public OffsetDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(OffsetDateTime slotEnd) { this.slotEnd = slotEnd; }
    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public String getBookingChannel() { return bookingChannel; }
    public void setBookingChannel(String bookingChannel) { this.bookingChannel = bookingChannel; }
    public UUID getBookedBy() { return bookedBy; }
    public void setBookedBy(UUID bookedBy) { this.bookedBy = bookedBy; }
    public UUID getSlotTemplateId() { return slotTemplateId; }
    public void setSlotTemplateId(UUID slotTemplateId) { this.slotTemplateId = slotTemplateId; }
    public String getSessionShift() { return sessionShift; }
    public void setSessionShift(String sessionShift) { this.sessionShift = sessionShift; }
    public String getSessionLabel() { return sessionLabel; }
    public void setSessionLabel(String sessionLabel) { this.sessionLabel = sessionLabel; }
    public String getNotificationPatientPhone() { return notificationPatientPhone; }
    public void setNotificationPatientPhone(String notificationPatientPhone) {
        this.notificationPatientPhone = notificationPatientPhone;
    }
    public String getNotificationPatientName() { return notificationPatientName; }
    public void setNotificationPatientName(String notificationPatientName) {
        this.notificationPatientName = notificationPatientName;
    }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
