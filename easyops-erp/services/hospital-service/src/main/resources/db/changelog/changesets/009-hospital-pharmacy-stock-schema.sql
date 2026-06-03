--liquibase formatted sql

--changeset easyops:hosp-pharm-006-create-pharmacy-stock context:hospital-pharmacy
--comment: Create pharmacy_stock table to track on-hand quantities by location, drug, and batch
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_stock (
    id UUID PRIMARY KEY,
    pharmacy_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity_on_hand NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_hp_pharmacy_stock_location_drug_batch
    ON hospital_pharmacy.pharmacy_stock (pharmacy_location_id, drug_id, COALESCE(batch_number, ''));

CREATE INDEX IF NOT EXISTS idx_hp_pharmacy_stock_drug
    ON hospital_pharmacy.pharmacy_stock (drug_id);

--changeset easyops:hosp-pharm-007-create-stock-movements context:hospital-pharmacy
--comment: Create stock_movements table to provide an audit trail of stock changes
CREATE TABLE IF NOT EXISTS hospital_pharmacy.stock_movements (
    id UUID PRIMARY KEY,
    pharmacy_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    movement_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    batch_number VARCHAR(100),
    movement_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reference_type VARCHAR(100),
    reference_id UUID,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_hp_stock_movements_location_time
    ON hospital_pharmacy.stock_movements (pharmacy_location_id, movement_time);

CREATE INDEX IF NOT EXISTS idx_hp_stock_movements_drug
    ON hospital_pharmacy.stock_movements (drug_id);

