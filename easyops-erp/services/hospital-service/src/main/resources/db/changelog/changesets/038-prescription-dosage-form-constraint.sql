--liquibase formatted sql

--changeset easyops:038-prescription-dosage-form-constraint splitStatements:true
--comment: Expand dosage_form CHECK constraints on prescriptions and prescription_medications to match the full DosageForm enum
-- Expand dosage_form CHECK constraints to match the full DosageForm enum

-- prescriptions table
ALTER TABLE ehr.prescriptions DROP CONSTRAINT IF EXISTS chk_dosage_form;
ALTER TABLE ehr.prescriptions ADD CONSTRAINT chk_dosage_form CHECK (dosage_form IN (
    'TABLET', 'CAPSULE', 'SYRUP', 'LIQUID', 'SOLUTION', 'SUSPENSION',
    'INJECTION', 'INFUSION',
    'CREAM', 'OINTMENT', 'LOTION', 'GEL',
    'POWDER', 'GRANULES',
    'INHALER', 'INHALATION',
    'DROPS', 'SUPPOSITORY', 'SPRAY', 'PATCH', 'MOUTHWASH',
    'TOPICAL', 'SUBLINGUAL', 'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL',
    'OTHER'
));

-- prescription_medications table
ALTER TABLE ehr.prescription_medications DROP CONSTRAINT IF EXISTS chk_pm_dosage_form;
ALTER TABLE ehr.prescription_medications ADD CONSTRAINT chk_pm_dosage_form CHECK (dosage_form IN (
    'TABLET', 'CAPSULE', 'SYRUP', 'LIQUID', 'SOLUTION', 'SUSPENSION',
    'INJECTION', 'INFUSION',
    'CREAM', 'OINTMENT', 'LOTION', 'GEL',
    'POWDER', 'GRANULES',
    'INHALER', 'INHALATION',
    'DROPS', 'SUPPOSITORY', 'SPRAY', 'PATCH', 'MOUTHWASH',
    'TOPICAL', 'SUBLINGUAL', 'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL',
    'OTHER'
));
