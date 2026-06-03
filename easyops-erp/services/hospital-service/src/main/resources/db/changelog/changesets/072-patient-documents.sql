--liquibase formatted sql

--changeset easyops:072-patient-documents splitStatements:false
--comment: Patient document storage table for uploaded files (pathology reports, radiology, consent forms, etc.)

CREATE TABLE IF NOT EXISTS ehr.patient_documents (
    document_id         UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          UUID            NOT NULL REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    organization_id     UUID,
    encounter_id        UUID,
    clinical_note_id    UUID,
    lab_result_id       UUID,
    prescription_id     UUID,

    -- Classification
    document_type       VARCHAR(50)     NOT NULL,
    document_category   VARCHAR(100),
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,

    -- File metadata
    file_name           VARCHAR(255)    NOT NULL,
    original_file_name  VARCHAR(255),
    file_path           VARCHAR(500)    NOT NULL,
    file_url            VARCHAR(500),
    file_size           BIGINT,
    mime_type           VARCHAR(100),
    file_hash           VARCHAR(255),

    -- Source / provenance
    source_facility     VARCHAR(255),
    document_date       TIMESTAMP,

    -- Authoring
    uploaded_by         UUID            NOT NULL,
    uploaded_date       TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Status
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_confidential     BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Audit
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_patient_documents_patient_id
    ON ehr.patient_documents (patient_id);

CREATE INDEX IF NOT EXISTS idx_patient_documents_patient_type
    ON ehr.patient_documents (patient_id, document_type);

CREATE INDEX IF NOT EXISTS idx_patient_documents_note_id
    ON ehr.patient_documents (clinical_note_id) WHERE clinical_note_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_patient_documents_prescription_id
    ON ehr.patient_documents (prescription_id) WHERE prescription_id IS NOT NULL;

COMMENT ON TABLE ehr.patient_documents IS 'Uploaded patient documents (reports, consent forms, images, etc.)';
COMMENT ON COLUMN ehr.patient_documents.document_type IS 'DocumentType enum: PATHOLOGY_REPORT, RADIOLOGY_REPORT, LAB_REPORT, CLINICAL_PHOTO, SURGICAL_REPORT, PRESCRIPTION, REFERRAL_LETTER, DISCHARGE_SUMMARY, CONSENT_FORM, INSURANCE_DOCUMENT, VITAL_RECORDS, IDENTITY_DOCUMENT, EXTERNAL_RECORD, ADVANCE_DIRECTIVE, OTHER';
