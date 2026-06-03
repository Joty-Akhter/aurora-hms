--liquibase formatted sql

--changeset easyops:109a-stock-requisitions
CREATE TABLE IF NOT EXISTS hospital_pharmacy.stock_requisitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    to_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    requested_by UUID NOT NULL,
    submitted_at TIMESTAMPTZ,
    approved_by UUID,
    approval_notes TEXT,
    approved_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_sreq_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','PARTIALLY_APPROVED','REJECTED','RECEIVED')),
    CONSTRAINT chk_hp_sreq_locations CHECK (from_location_id <> to_location_id)
);
CREATE INDEX IF NOT EXISTS idx_hp_sreq_from ON hospital_pharmacy.stock_requisitions (from_location_id);
CREATE INDEX IF NOT EXISTS idx_hp_sreq_to ON hospital_pharmacy.stock_requisitions (to_location_id);
CREATE INDEX IF NOT EXISTS idx_hp_sreq_status ON hospital_pharmacy.stock_requisitions (status);

--changeset easyops:109b-stock-requisition-lines
CREATE TABLE IF NOT EXISTS hospital_pharmacy.stock_requisition_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requisition_id UUID NOT NULL REFERENCES hospital_pharmacy.stock_requisitions(id) ON DELETE CASCADE,
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    requested_quantity NUMERIC(19,4) NOT NULL,
    approved_quantity NUMERIC(19,4),
    received_quantity NUMERIC(19,4),
    batch_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_sreq_line_qty CHECK (requested_quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_hp_sreq_line_req ON hospital_pharmacy.stock_requisition_lines (requisition_id);
CREATE INDEX IF NOT EXISTS idx_hp_sreq_line_drug ON hospital_pharmacy.stock_requisition_lines (drug_id);
