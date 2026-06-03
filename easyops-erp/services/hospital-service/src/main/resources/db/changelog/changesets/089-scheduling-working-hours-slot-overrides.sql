--liquibase formatted sql

--changeset easyops:089-scheduling-working-hours-slot-overrides splitStatements:true
--comment: Optional per–working-hours slot length and capacity (from doctor max appointments sync)
ALTER TABLE hospital_scheduling.scheduling_working_hours
    ADD COLUMN IF NOT EXISTS slot_duration_minutes INT;

ALTER TABLE hospital_scheduling.scheduling_working_hours
    ADD COLUMN IF NOT EXISTS slots_per_interval INT;

COMMENT ON COLUMN hospital_scheduling.scheduling_working_hours.slot_duration_minutes IS
    'When set, overrides default slot template duration for this working-hours segment (e.g. from doctor schedule window / max appointments).';

COMMENT ON COLUMN hospital_scheduling.scheduling_working_hours.slots_per_interval IS
    'When set, overrides default slot template capacity for this segment (usually 1 for one patient per slot).';
