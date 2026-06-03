package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CreateAppointmentRequest {

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID resourceId;

    private UUID clinicOrLocationId;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private OffsetDateTime slotStart;

    @NotNull
    private OffsetDateTime slotEnd;

    @NotNull
    @Size(max = 30)
    private String appointmentType; // NEW, FOLLOW_UP, EMERGENCY, ROUTINE, REPORT

    @Size(max = 255)
    private String idempotencyKey;

    /** When slot is at capacity (max_per_slot), provide a reason to allow overbooking. */
    @Size(max = 255)
    private String overbookingOverrideReason;

    /** Phase 4: optional additional resources (e.g. room); creates one reservation per resource with same reference. */
    private List<UUID> additionalResourceIds;

    /** Required: booking channel (WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL). */
    @NotNull
    @Size(max = 30)
    private String bookingChannel = "FRONT_DESK";

    /** Optional: actor user id (staff member booking on behalf of patient). */
    private UUID bookedBy;

    /** Optional: slot template used (§11.12). */
    private UUID slotTemplateId;

    /** Optional: session shift when no template (MORNING, EVENING, NIGHT, FULL_DAY, CUSTOM). */
    @Size(max = 20)
    private String sessionShift;

    /** Optional: human-readable session label. */
    @Size(max = 255)
    private String sessionLabel;

    /** Optional: patient display name for SMS (e.g. full name). */
    @Size(max = 255)
    private String patientSmsDisplayName;

    /** Optional: mobile number for appointment confirmation SMS (E.164 or local digits). */
    @Size(max = 40)
    private String patientSmsPhone;

    /** Optional: first serial/token for this slot when none booked yet (from doctor profile serialStartFrom). */
    private Integer serialStartFrom;

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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getOverbookingOverrideReason() { return overbookingOverrideReason; }
    public void setOverbookingOverrideReason(String overbookingOverrideReason) { this.overbookingOverrideReason = overbookingOverrideReason; }
    public List<UUID> getAdditionalResourceIds() { return additionalResourceIds; }
    public void setAdditionalResourceIds(List<UUID> additionalResourceIds) { this.additionalResourceIds = additionalResourceIds; }
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
    public String getPatientSmsDisplayName() { return patientSmsDisplayName; }
    public void setPatientSmsDisplayName(String patientSmsDisplayName) { this.patientSmsDisplayName = patientSmsDisplayName; }
    public String getPatientSmsPhone() { return patientSmsPhone; }
    public void setPatientSmsPhone(String patientSmsPhone) { this.patientSmsPhone = patientSmsPhone; }
    public Integer getSerialStartFrom() { return serialStartFrom; }
    public void setSerialStartFrom(Integer serialStartFrom) { this.serialStartFrom = serialStartFrom; }
}
