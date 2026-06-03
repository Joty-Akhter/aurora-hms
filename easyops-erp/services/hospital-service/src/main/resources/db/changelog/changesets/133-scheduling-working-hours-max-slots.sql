--liquibase formatted sql
--changeset easyops:scheduling-working-hours-max-slots-133
--comment: Cap generated appointment slots per doctor schedule segment (max patients per day window)

ALTER TABLE hospital_scheduling.scheduling_working_hours
    ADD COLUMN IF NOT EXISTS max_slots_per_segment INTEGER;

COMMENT ON COLUMN hospital_scheduling.scheduling_working_hours.max_slots_per_segment IS
    'When set with slot_duration_minutes, limits how many bookable time slots are generated for this segment (doctor max patients per session).';
