--liquibase formatted sql

--changeset easyops:patient-identity-card-audit-log-063
--comment: Add audit surface for patient identity card print/reprint/replace operations.
CREATE TABLE IF NOT EXISTS ehr.patient_identity_card_audit_log (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES ehr.patients(patient_id),
    card_id UUID NULL,
    card_number VARCHAR(100) NULL,
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    printed_by UUID NULL,
    printed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_identity_card_audit_patient
    ON ehr.patient_identity_card_audit_log (patient_id, printed_at DESC);

CREATE INDEX IF NOT EXISTS idx_patient_identity_card_audit_action
    ON ehr.patient_identity_card_audit_log (action, printed_at DESC);
