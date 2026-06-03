--liquibase formatted sql

--changeset easyops:107a-near-expiry-add-cols
ALTER TABLE hospital_pharmacy.pharmacy_near_expiry_rules
    ADD COLUMN IF NOT EXISTS effective_to DATE,
    ADD COLUMN IF NOT EXISTS discount_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approver_role_code VARCHAR(100);

--changeset easyops:107b-near-expiry-update-action-constraint
ALTER TABLE hospital_pharmacy.pharmacy_near_expiry_rules
    DROP CONSTRAINT IF EXISTS chk_hp_near_expiry_action;

ALTER TABLE hospital_pharmacy.pharmacy_near_expiry_rules
    ADD CONSTRAINT chk_hp_near_expiry_action
    CHECK (action IN ('BLOCK', 'WARN', 'ALLOW', 'ALLOW_WITH_APPROVAL'));

ALTER TABLE hospital_pharmacy.pharmacy_near_expiry_rules
    ADD CONSTRAINT chk_hp_near_expiry_effective_dates
    CHECK (effective_to IS NULL OR effective_to >= effective_from);
