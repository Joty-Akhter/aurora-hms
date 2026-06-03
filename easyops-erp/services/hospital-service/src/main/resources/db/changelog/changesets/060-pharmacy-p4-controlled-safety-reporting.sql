--liquibase formatted sql

--changeset easyops:hospital-service:060-pharmacy-p4-controlled-safety-reporting context:hospital-pharmacy
--comment: Phase P4 — controlled substance profile code, witness on dispense line, reporting indexes (WS-H, WS-L)

-- H1: optional profile discriminator (e.g. US_DEA_II, NHS_CD) — null means "use legacy controlled_drug_flag only"
ALTER TABLE hospital_pharmacy.drugs
    ADD COLUMN IF NOT EXISTS controlled_profile_code VARCHAR(64);

COMMENT ON COLUMN hospital_pharmacy.drugs.controlled_profile_code IS 'Jurisdiction-specific controlled profile; NONE or empty disables profile-based rules beyond controlled_drug_flag';

-- H2: witness attestation for controlled-line dispense (when policy requires)
ALTER TABLE hospital_pharmacy.dispense_lines
    ADD COLUMN IF NOT EXISTS witness_user_id UUID;

COMMENT ON COLUMN hospital_pharmacy.dispense_lines.witness_user_id IS 'Second pharmacist/user witnessing controlled substance issue when policy requires';

-- Performance: reporting / register queries by time window
CREATE INDEX IF NOT EXISTS idx_hp_dispense_lines_created_at
    ON hospital_pharmacy.dispense_lines (created_at);

CREATE INDEX IF NOT EXISTS idx_hp_stock_movements_loc_time_type
    ON hospital_pharmacy.stock_movements (pharmacy_location_id, movement_time, movement_type);
