--liquibase formatted sql

--changeset easyops:hosp-sched-012-appointment-reschedule-cancel-idempotency
--comment: Phase 2 – reschedule and cancel idempotency keys on scheduling_appointments
ALTER TABLE hospital_scheduling.scheduling_appointments
    ADD COLUMN IF NOT EXISTS reschedule_idempotency_key VARCHAR(255);
ALTER TABLE hospital_scheduling.scheduling_appointments
    ADD COLUMN IF NOT EXISTS cancel_idempotency_key VARCHAR(255);
