--liquibase formatted sql

--changeset hospital-service:066-clinical-notes-authoring-provider-columns
--comment Add missing clinical_notes columns used by ClinicalNote entity
ALTER TABLE ehr.clinical_notes
    ADD COLUMN IF NOT EXISTS authoring_provider_id UUID,
    ADD COLUMN IF NOT EXISTS authoring_provider_name VARCHAR(200);

