package com.easyops.hospitalscheduling.domain.appointment;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_appointments", schema = "hospital_scheduling")
@EntityListeners(AuditingEntityListener.class)
public class Appointment {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "clinic_or_location_id")
    private UUID clinicOrLocationId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "slot_start", nullable = false)
    private OffsetDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private OffsetDateTime slotEnd;

    @Column(name = "appointment_type", nullable = false, length = 30)
    private String appointmentType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "token_number")
    private Integer tokenNumber;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "reschedule_idempotency_key", length = 255)
    private String rescheduleIdempotencyKey;

    @Column(name = "cancel_idempotency_key", length = 255)
    private String cancelIdempotencyKey;

    @Column(name = "booking_channel", nullable = false, length = 30)
    private String bookingChannel = "INTERNAL";

    @Column(name = "booked_by")
    private UUID bookedBy;

    @Column(name = "slot_template_id")
    private UUID slotTemplateId;

    @Column(name = "session_shift", length = 20)
    private String sessionShift;

    @Column(name = "session_label", length = 255)
    private String sessionLabel;

    /** Snapshot for SMS / communication payloads; persisted so reschedule and reloads still have contact. */
    @Column(name = "notification_patient_name", length = 255)
    private String notificationPatientName;

    @Column(name = "notification_patient_phone", length = 64)
    private String notificationPatientPhone;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRescheduleIdempotencyKey() { return rescheduleIdempotencyKey; }
    public void setRescheduleIdempotencyKey(String rescheduleIdempotencyKey) { this.rescheduleIdempotencyKey = rescheduleIdempotencyKey; }
    public String getCancelIdempotencyKey() { return cancelIdempotencyKey; }
    public void setCancelIdempotencyKey(String cancelIdempotencyKey) { this.cancelIdempotencyKey = cancelIdempotencyKey; }
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
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getNotificationPatientName() { return notificationPatientName; }
    public void setNotificationPatientName(String notificationPatientName) { this.notificationPatientName = notificationPatientName; }
    public String getNotificationPatientPhone() { return notificationPatientPhone; }
    public void setNotificationPatientPhone(String notificationPatientPhone) { this.notificationPatientPhone = notificationPatientPhone; }
}
