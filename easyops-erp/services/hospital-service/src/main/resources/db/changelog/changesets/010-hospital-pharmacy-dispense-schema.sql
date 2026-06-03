--liquibase formatted sql

--changeset easyops:hosp-pharm-008-create-dispense-orders context:hospital-pharmacy
--comment: Create dispense_orders table for prescription fulfillment
CREATE TABLE IF NOT EXISTS hospital_pharmacy.dispense_orders (
    id UUID PRIMARY KEY,
    prescription_id UUID,
    visit_id UUID,
    patient_id UUID,
    pharmacy_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    status VARCHAR(50) NOT NULL,
    context_type VARCHAR(50) NOT NULL,
    department_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_orders_patient
    ON hospital_pharmacy.dispense_orders (patient_id);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_orders_visit
    ON hospital_pharmacy.dispense_orders (visit_id);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_orders_location_status
    ON hospital_pharmacy.dispense_orders (pharmacy_location_id, status);

--changeset easyops:hosp-pharm-009-create-dispense-lines context:hospital-pharmacy
--comment: Create dispense_lines table for items within a dispense order
CREATE TABLE IF NOT EXISTS hospital_pharmacy.dispense_lines (
    id UUID PRIMARY KEY,
    dispense_order_id UUID NOT NULL REFERENCES hospital_pharmacy.dispense_orders(id) ON DELETE CASCADE,
    prescription_line_id UUID,
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    batch_number VARCHAR(100),
    quantity_prescribed NUMERIC(19, 4),
    quantity_dispensed NUMERIC(19, 4) NOT NULL DEFAULT 0,
    quantity_returned NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_lines_order
    ON hospital_pharmacy.dispense_lines (dispense_order_id);

CREATE INDEX IF NOT EXISTS idx_hp_dispense_lines_drug
    ON hospital_pharmacy.dispense_lines (drug_id);

