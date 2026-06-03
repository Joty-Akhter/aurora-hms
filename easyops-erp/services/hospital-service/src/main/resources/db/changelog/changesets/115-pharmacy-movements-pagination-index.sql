--liquibase formatted sql

--changeset easyops:115-stock-movements-pagination-index
CREATE INDEX IF NOT EXISTS idx_hp_stock_mov_loc_type_time
    ON hospital_pharmacy.stock_movements (pharmacy_location_id, movement_type, movement_time DESC);

CREATE INDEX IF NOT EXISTS idx_hp_stock_mov_loc_drug_time
    ON hospital_pharmacy.stock_movements (pharmacy_location_id, drug_id, movement_time DESC);

CREATE INDEX IF NOT EXISTS idx_hp_stock_mov_batch
    ON hospital_pharmacy.stock_movements (batch_number)
    WHERE batch_number IS NOT NULL;
