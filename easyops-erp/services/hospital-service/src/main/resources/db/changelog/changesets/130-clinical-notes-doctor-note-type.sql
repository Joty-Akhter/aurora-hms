--liquibase formatted sql

--changeset hospital-service:130-clinical-notes-doctor-note-type
--comment Allow DOCTOR_NOTE in clinical_notes.note_type (Doctor Notes module)
ALTER TABLE ehr.clinical_notes DROP CONSTRAINT IF EXISTS chk_note_type;

ALTER TABLE ehr.clinical_notes ADD CONSTRAINT chk_note_type CHECK (note_type IN
    ('SOAP', 'PROGRESS', 'CONSULTATION', 'DISCHARGE', 'PROCEDURE', 'ADMISSION', 'OPERATIVE', 'DOCTOR_NOTE', 'OTHER'));
