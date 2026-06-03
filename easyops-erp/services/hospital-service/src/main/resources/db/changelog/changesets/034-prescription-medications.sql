-- Line items for prescriptions (multiple medicines per prescription)

CREATE TABLE IF NOT EXISTS ehr.prescription_medications (
    prescription_medication_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    line_number INT NOT NULL,

    medication_name VARCHAR(500) NOT NULL,
    medication_code VARCHAR(100),
    medication_code_type VARCHAR(20),

    dosage_strength DECIMAL(10, 3),
    dosage_unit VARCHAR(50),
    dosage_form VARCHAR(50) NOT NULL,

    quantity DECIMAL(10, 2),
    quantity_unit VARCHAR(50),
    route VARCHAR(50) NOT NULL,
    frequency VARCHAR(200),
    instructions TEXT,

    start_date DATE NOT NULL,
    end_date DATE,
    duration_days INTEGER,

    refills_authorized INTEGER DEFAULT 0,
    refills_remaining INTEGER DEFAULT 0,

    substitution_allowed BOOLEAN DEFAULT true,
    daw_code VARCHAR(10),

    is_controlled_substance BOOLEAN DEFAULT false,
    schedule VARCHAR(10),
    dea_number VARCHAR(20),

    CONSTRAINT fk_prescription_medication_prescription FOREIGN KEY (prescription_id)
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT uq_prescription_medication_line UNIQUE (prescription_id, line_number),
    CONSTRAINT chk_pm_dosage_form CHECK (dosage_form IN
        ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'TOPICAL', 'INHALATION',
         'SUBLINGUAL', 'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER')),
    CONSTRAINT chk_pm_route CHECK (route IN
        ('ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALATION', 'SUBLINGUAL',
         'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER')),
    CONSTRAINT chk_pm_schedule CHECK (schedule IS NULL OR schedule IN ('II', 'III', 'IV', 'V')),
    CONSTRAINT chk_pm_medication_code_type CHECK (medication_code_type IS NULL OR medication_code_type IN
        ('RXNORM', 'NDC', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_prescription_medications_rx ON ehr.prescription_medications(prescription_id);

-- Backfill one line per existing prescription from denormalized columns
INSERT INTO ehr.prescription_medications (
    prescription_medication_id,
    prescription_id,
    line_number,
    medication_name,
    medication_code,
    medication_code_type,
    dosage_strength,
    dosage_unit,
    dosage_form,
    quantity,
    quantity_unit,
    route,
    frequency,
    instructions,
    start_date,
    end_date,
    duration_days,
    refills_authorized,
    refills_remaining,
    substitution_allowed,
    daw_code,
    is_controlled_substance,
    schedule,
    dea_number
)
SELECT
    gen_random_uuid(),
    p.prescription_id,
    1,
    p.medication_name,
    p.medication_code,
    p.medication_code_type,
    p.dosage_strength,
    p.dosage_unit,
    p.dosage_form,
    p.quantity,
    p.quantity_unit,
    p.route,
    p.frequency,
    p.instructions,
    p.start_date,
    p.end_date,
    p.duration_days,
    p.refills_authorized,
    p.refills_remaining,
    p.substitution_allowed,
    p.daw_code,
    p.is_controlled_substance,
    p.schedule,
    p.dea_number
FROM ehr.prescriptions p
WHERE NOT EXISTS (
    SELECT 1 FROM ehr.prescription_medications pm WHERE pm.prescription_id = p.prescription_id
);
