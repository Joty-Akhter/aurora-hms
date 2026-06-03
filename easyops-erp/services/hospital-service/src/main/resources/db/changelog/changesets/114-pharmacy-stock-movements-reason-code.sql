--liquibase formatted sql

--changeset easyops:114-stock-movements-reason-code
ALTER TABLE hospital_pharmacy.stock_movements
    ADD COLUMN IF NOT EXISTS reason_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS requested_by UUID;

CREATE INDEX IF NOT EXISTS idx_hp_stock_mov_reason_code
    ON hospital_pharmacy.stock_movements (reason_code)
    WHERE reason_code IS NOT NULL;
