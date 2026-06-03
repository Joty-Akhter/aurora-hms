--liquibase formatted sql

--changeset easyops:hospital-service:053-pharmacy-dispense-phase1-gaps context:hospital-pharmacy
--comment: P1 gaps plan — dispense line audit columns, idempotency store for POST lines/returns
-- Phase 1 from pharmacy-gaps-implementation-plan.md (C1, K2, D1 schema support)

-- Dispense line extensions (pharmacy.md §4.1.1, §4.1.5)
ALTER TABLE hospital_pharmacy.dispense_lines
    ADD COLUMN IF NOT EXISTS reason_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS documenting_user_id UUID,
    ADD COLUMN IF NOT EXISTS stock_snapshot_ref VARCHAR(500),
    ADD COLUMN IF NOT EXISTS substituted_drug_id UUID REFERENCES hospital_pharmacy.drugs (id),
    ADD COLUMN IF NOT EXISTS override_reason_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS override_approver_id UUID,
    ADD COLUMN IF NOT EXISTS remaining_quantity NUMERIC(19, 4);

COMMENT ON COLUMN hospital_pharmacy.dispense_lines.reason_code IS 'Line-level reason (refusal, out of stock, etc.)';
COMMENT ON COLUMN hospital_pharmacy.dispense_lines.documenting_user_id IS 'Pharmacist/user who confirmed line outcome';
COMMENT ON COLUMN hospital_pharmacy.dispense_lines.override_reason_code IS 'Stock override reason when FILLED_WITH_STOCK_OVERRIDE';
COMMENT ON COLUMN hospital_pharmacy.dispense_lines.remaining_quantity IS 'Remaining qty for PARTIAL lines';

-- Idempotency keys for POST /dispense-orders/{id}/lines and /returns (pharmacy-gaps plan K2)
CREATE TABLE IF NOT EXISTS hospital_pharmacy.dispense_idempotency (
    id UUID PRIMARY KEY,
    scope VARCHAR(220) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    response_body TEXT NOT NULL,
    http_status INT NOT NULL DEFAULT 200,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hp_dispense_idempotency_scope_key UNIQUE (scope, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_idempotency_created
    ON hospital_pharmacy.dispense_idempotency (created_at);
