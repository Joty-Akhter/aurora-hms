--liquibase formatted sql

--changeset hospital-service:140-patient-type
ALTER TABLE ehr.patients ADD COLUMN IF NOT EXISTS patient_type VARCHAR(50);

COMMENT ON COLUMN ehr.patients.patient_type IS 'Registration category: General, Corporate, Insurance, Staff, etc.';
