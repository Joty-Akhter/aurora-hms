--liquibase formatted sql

--changeset easyops:092-disambiguate-verification-check-constraints splitStatements:true
--comment: PostgreSQL requires CHECK constraint names unique per schema; patient_insurance and allergies both used chk_verification_status. Rename/recreate so patient_insurance accepts Not_Verified / Not_Applicable reliably.

-- Allergies: distinct constraint name (CONFIRMED / UNCONFIRMED / REFUTED).
ALTER TABLE ehr.allergies DROP CONSTRAINT IF EXISTS chk_verification_status;
ALTER TABLE ehr.allergies DROP CONSTRAINT IF EXISTS chk_allergy_verification_status;
ALTER TABLE ehr.allergies ADD CONSTRAINT chk_allergy_verification_status CHECK (
    verification_status IN ('CONFIRMED', 'UNCONFIRMED', 'REFUTED')
);

-- Patient insurance: distinct constraint name (JPA EnumType.STRING values).
ALTER TABLE ehr.patient_insurance DROP CONSTRAINT IF EXISTS chk_verification_status;
ALTER TABLE ehr.patient_insurance DROP CONSTRAINT IF EXISTS chk_patient_insurance_verification_status;

UPDATE ehr.patient_insurance SET verification_status = 'Not_Verified' WHERE verification_status IN ('Not Verified', 'NOT_VERIFIED');
UPDATE ehr.patient_insurance SET verification_status = 'Not_Applicable' WHERE verification_status IN ('Not Applicable');
UPDATE ehr.patient_insurance SET verification_status = 'Not_Verified'
WHERE verification_status IS NULL
   OR verification_status NOT IN ('Verified', 'Pending', 'Not_Verified', 'Not_Applicable');

ALTER TABLE ehr.patient_insurance ADD CONSTRAINT chk_patient_insurance_verification_status CHECK (
    verification_status IN ('Verified', 'Pending', 'Not_Verified', 'Not_Applicable')
);
