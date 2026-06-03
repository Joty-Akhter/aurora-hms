--liquibase formatted sql

--changeset easyops:076-doctor-appointment-slots
--comment: Add appointment_slots column to hospital.doctors — stores JSON array of DoctorAppointmentSlot (startTime, endTime, days[], maxPatients). Replaces the per-day weekly_schedule model with a per-slot-with-days model.

ALTER TABLE hospital.doctors
    ADD COLUMN IF NOT EXISTS appointment_slots TEXT;
