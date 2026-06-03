--liquibase formatted sql

--changeset easyops:060-pharmacy-p3-formulary-line-override
--comment: Phase P3 WS-G — formulary override reason on dispense lines (distinct from stock override_reason_code)
ALTER TABLE hospital_pharmacy.dispense_lines
    ADD COLUMN IF NOT EXISTS formulary_override_reason VARCHAR(2000);

COMMENT ON COLUMN hospital_pharmacy.dispense_lines.formulary_override_reason IS
    'Documented reason when dispensing a formulary-restricted drug without substitution (WS-G v1)';
