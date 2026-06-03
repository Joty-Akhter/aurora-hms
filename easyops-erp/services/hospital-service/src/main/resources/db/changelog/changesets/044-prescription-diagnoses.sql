--liquibase formatted sql

-- FR-P1.4a: Multi-Diagnosis support for prescriptions
-- Replaces single diagnosis_code VARCHAR on ehr.prescriptions with a
-- normalised ehr.prescription_diagnoses table that supports 1-N ICD codes
-- per prescription, with exactly one marked as primary.

--changeset hospital-service:044-prescription-diagnoses-table
CREATE TABLE ehr.prescription_diagnoses (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    prescription_id      UUID         NOT NULL,
    diagnosis_code       VARCHAR(20)  NOT NULL,
    diagnosis_description VARCHAR(500),
    is_primary           BOOLEAN      NOT NULL DEFAULT FALSE,
    sequence_order       INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_prescription_diagnoses PRIMARY KEY (id),
    CONSTRAINT fk_prx_diag_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES ehr.prescriptions (prescription_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_prx_diag_prescription_id
    ON ehr.prescription_diagnoses (prescription_id);

CREATE INDEX idx_prx_diag_code
    ON ehr.prescription_diagnoses (diagnosis_code);

COMMENT ON TABLE  ehr.prescription_diagnoses IS 'FR-P1.4a: One or more ICD-10 diagnoses linked to a prescription; exactly one row per prescription should have is_primary = TRUE.';
COMMENT ON COLUMN ehr.prescription_diagnoses.is_primary        IS 'TRUE for the primary/principal diagnosis; only one row per prescription should be primary.';
COMMENT ON COLUMN ehr.prescription_diagnoses.sequence_order    IS 'Display order (0-based); primary diagnosis is typically 0.';
COMMENT ON COLUMN ehr.prescription_diagnoses.diagnosis_description IS 'Human-readable ICD-10 description stored at write time for audit/print purposes.';

--changeset hospital-service:044-backfill-diagnosis-from-legacy
-- Migrate any existing single diagnosis_code rows into the new table.
INSERT INTO ehr.prescription_diagnoses (prescription_id, diagnosis_code, is_primary, sequence_order, created_at)
SELECT prescription_id, diagnosis_code, TRUE, 0, COALESCE(created_at, now())
FROM   ehr.prescriptions
WHERE  diagnosis_code IS NOT NULL
  AND  diagnosis_code <> '';
