--liquibase formatted sql

--changeset easyops:110a-supplier-return-orders
CREATE TABLE IF NOT EXISTS hospital_pharmacy.supplier_return_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manufacturer_id UUID NOT NULL REFERENCES hospital_pharmacy.manufacturers(id),
    from_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reason TEXT NOT NULL,
    requested_by UUID NOT NULL,
    submitted_at TIMESTAMPTZ,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    dispatched_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_sro_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','DISPATCHED'))
);
CREATE INDEX IF NOT EXISTS idx_hp_sro_manufacturer ON hospital_pharmacy.supplier_return_orders (manufacturer_id);
CREATE INDEX IF NOT EXISTS idx_hp_sro_location ON hospital_pharmacy.supplier_return_orders (from_location_id);
CREATE INDEX IF NOT EXISTS idx_hp_sro_status ON hospital_pharmacy.supplier_return_orders (status);

--changeset easyops:110b-supplier-return-order-lines
CREATE TABLE IF NOT EXISTS hospital_pharmacy.supplier_return_order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_order_id UUID NOT NULL REFERENCES hospital_pharmacy.supplier_return_orders(id) ON DELETE CASCADE,
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity NUMERIC(19,4) NOT NULL,
    return_reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_sro_line_reason CHECK (return_reason IN ('EXPIRED','DAMAGED','EXCESS','OTHER')),
    CONSTRAINT chk_hp_sro_line_qty CHECK (quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_hp_sro_line_order ON hospital_pharmacy.supplier_return_order_lines (return_order_id);
CREATE INDEX IF NOT EXISTS idx_hp_sro_line_drug ON hospital_pharmacy.supplier_return_order_lines (drug_id);
