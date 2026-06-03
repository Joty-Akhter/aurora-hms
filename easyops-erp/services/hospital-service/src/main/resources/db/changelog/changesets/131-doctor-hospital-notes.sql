--liquibase formatted sql

--changeset hospital-service:131-doctor-hospital-notes
--comment Hospital-wide doctor broadcast notes (messages to staff, not patient-specific)

CREATE TABLE IF NOT EXISTS hospital.doctor_hospital_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES hospital.doctors(doctor_id) ON DELETE RESTRICT,
    doctor_name VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

CREATE INDEX IF NOT EXISTS idx_doctor_hospital_notes_doctor ON hospital.doctor_hospital_notes(doctor_id);
CREATE INDEX IF NOT EXISTS idx_doctor_hospital_notes_created_at ON hospital.doctor_hospital_notes(created_at DESC);

COMMENT ON TABLE hospital.doctor_hospital_notes IS 'Hospital-wide messages from doctors (e.g. chamber closed, running late); not tied to patients';
