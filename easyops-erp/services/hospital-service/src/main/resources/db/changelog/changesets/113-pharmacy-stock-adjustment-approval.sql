--liquibase formatted sql

--changeset easyops:113-stock-adjustment-approvals
CREATE TABLE IF NOT EXISTS hospital_pharmacy.stock_adjustment_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pharmacy_location_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_locations(id),
    requested_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    request_payload JSONB NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_saa_status CHECK (status IN ('PENDING_APPROVAL','APPROVED','REJECTED'))
);
CREATE INDEX IF NOT EXISTS idx_hp_saa_location ON hospital_pharmacy.stock_adjustment_approvals (pharmacy_location_id);
CREATE INDEX IF NOT EXISTS idx_hp_saa_status ON hospital_pharmacy.stock_adjustment_approvals (status);
