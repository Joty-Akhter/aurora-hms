--liquibase formatted sql

--changeset easyops:088-patient-insurance-verification-enum splitStatements:true
--comment: Align patient_insurance verification_status CHECK and default with JPA EnumType.STRING (Not_Verified, Not_Applicable)
ALTER TABLE ehr.patient_insurance DROP CONSTRAINT IF EXISTS chk_verification_status;

UPDATE ehr.patient_insurance SET verification_status = 'Not_Verified' WHERE verification_status = 'Not Verified';
UPDATE ehr.patient_insurance SET verification_status = 'Not_Applicable' WHERE verification_status = 'Not Applicable';

ALTER TABLE ehr.patient_insurance
    ALTER COLUMN verification_status SET DEFAULT 'Not_Verified';

ALTER TABLE ehr.patient_insurance ADD CONSTRAINT chk_verification_status CHECK (
    verification_status IN ('Verified', 'Pending', 'Not_Verified', 'Not_Applicable')
);
