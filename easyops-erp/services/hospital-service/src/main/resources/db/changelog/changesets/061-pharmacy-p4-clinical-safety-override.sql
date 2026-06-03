--liquibase formatted sql

--changeset easyops:hospital-service:061-pharmacy-p4-clinical-safety-override context:hospital-pharmacy
--comment: Phase P4 WS-I — persist documented override when dispensing despite clinical safety block

ALTER TABLE hospital_pharmacy.dispense_lines
    ADD COLUMN IF NOT EXISTS clinical_safety_override_reason VARCHAR(2000);

COMMENT ON COLUMN hospital_pharmacy.dispense_lines.clinical_safety_override_reason IS 'Documented reason when line dispensed despite interaction/allergy block (policy + pharmacist attestation)';
