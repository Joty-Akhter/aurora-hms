--liquibase formatted sql

--changeset easyops:hosp-sched-017-appointment-notification-columns
--comment: Persist SMS snapshot (patient display name / phone) on scheduling_appointments for reschedule and reloads
ALTER TABLE hospital_scheduling.scheduling_appointments
    ADD COLUMN IF NOT EXISTS notification_patient_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS notification_patient_phone VARCHAR(64);
