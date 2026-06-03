package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class RescheduleAppointmentRequest {

    @NotNull
    private OffsetDateTime newSlotStart;

    @NotNull
    private OffsetDateTime newSlotEnd;

    @Size(max = 255)
    private String idempotencyKey;

    /** Optional: for SMS on reschedule (same semantics as create appointment). */
    @Size(max = 255)
    private String patientSmsDisplayName;

    @Size(max = 40)
    private String patientSmsPhone;

    /** Optional: first token in new slot when none booked yet (doctor profile serialStartFrom). */
    private Integer serialStartFrom;

    public OffsetDateTime getNewSlotStart() { return newSlotStart; }
    public void setNewSlotStart(OffsetDateTime newSlotStart) { this.newSlotStart = newSlotStart; }
    public OffsetDateTime getNewSlotEnd() { return newSlotEnd; }
    public void setNewSlotEnd(OffsetDateTime newSlotEnd) { this.newSlotEnd = newSlotEnd; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getPatientSmsDisplayName() { return patientSmsDisplayName; }
    public void setPatientSmsDisplayName(String patientSmsDisplayName) { this.patientSmsDisplayName = patientSmsDisplayName; }
    public String getPatientSmsPhone() { return patientSmsPhone; }
    public void setPatientSmsPhone(String patientSmsPhone) { this.patientSmsPhone = patientSmsPhone; }
    public Integer getSerialStartFrom() { return serialStartFrom; }
    public void setSerialStartFrom(Integer serialStartFrom) { this.serialStartFrom = serialStartFrom; }
}
