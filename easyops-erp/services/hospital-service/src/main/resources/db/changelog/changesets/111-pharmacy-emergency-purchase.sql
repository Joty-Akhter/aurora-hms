--liquibase formatted sql

--changeset easyops:111a-emergency-purchase-entries
CREATE TABLE IF NOT EXISTS hospital_pharmacy.emergency_purchase_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    to_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reason TEXT NOT NULL,
    supplier_name VARCHAR(255),
    invoice_ref VARCHAR(100),
    requested_by UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_epe_status CHECK (status IN ('DRAFT','APPROVED','RECEIVED'))
);
CREATE INDEX IF NOT EXISTS idx_hp_epe_location ON hospital_pharmacy.emergency_purchase_entries (to_location_id);
CREATE INDEX IF NOT EXISTS idx_hp_epe_status ON hospital_pharmacy.emergency_purchase_entries (status);

--changeset easyops:111b-emergency-purchase-lines
CREATE TABLE IF NOT EXISTS hospital_pharmacy.emergency_purchase_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_entry_id UUID NOT NULL REFERENCES hospital_pharmacy.emergency_purchase_entries(id) ON DELETE CASCADE,
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity NUMERIC(19,4) NOT NULL,
    unit_cost NUMERIC(10,2),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_epl_qty CHECK (quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_hp_epl_entry ON hospital_pharmacy.emergency_purchase_lines (purchase_entry_id);
CREATE INDEX IF NOT EXISTS idx_hp_epl_drug ON hospital_pharmacy.emergency_purchase_lines (drug_id);
