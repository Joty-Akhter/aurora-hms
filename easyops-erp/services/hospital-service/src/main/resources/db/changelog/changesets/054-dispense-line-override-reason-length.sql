--liquibase formatted sql

--changeset easyops:hospital-service:054-dispense-line-override-reason-length context:hospital-pharmacy
--comment: P1 — stockOverrideReason is free-text audit; align column with API (was VARCHAR(100))
ALTER TABLE hospital_pharmacy.dispense_lines
    ALTER COLUMN override_reason_code TYPE VARCHAR(2000);

COMMENT ON COLUMN hospital_pharmacy.dispense_lines.override_reason_code IS 'Free-text stock override reason when FILLED_WITH_STOCK_OVERRIDE (pharmacy.md §4.1.5)';
