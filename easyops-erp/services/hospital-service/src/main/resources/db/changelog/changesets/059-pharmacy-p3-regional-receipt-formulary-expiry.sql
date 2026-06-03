--liquibase formatted sql

--changeset easyops:059a-pharmacy-p3-dispense-order-regional
--comment: Phase P3 WS-E — optional paper Rx ref and external validation status
ALTER TABLE hospital_pharmacy.dispense_orders
    ADD COLUMN IF NOT EXISTS paper_prescription_ref VARCHAR(500);
ALTER TABLE hospital_pharmacy.dispense_orders
    ADD COLUMN IF NOT EXISTS prescription_image_attachment_id UUID;
ALTER TABLE hospital_pharmacy.dispense_orders
    ADD COLUMN IF NOT EXISTS external_validation_status VARCHAR(32);

COMMENT ON COLUMN hospital_pharmacy.dispense_orders.external_validation_status IS
    'NOT_REQUIRED | PENDING | VERIFIED | FAILED_SOFT';

--changeset easyops:059b-pharmacy-p3-near-expiry-rules
--comment: Phase P3 WS-J — parameterized near-expiry rules (v1)
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_near_expiry_rules (
    id UUID PRIMARY KEY,
    effective_from DATE NOT NULL,
    therapeutic_class_id UUID,
    days_before_expiry INT NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_near_expiry_days CHECK (days_before_expiry >= 0),
    CONSTRAINT chk_hp_near_expiry_action CHECK (action IN ('BLOCK', 'WARN', 'ALLOW'))
);

CREATE INDEX IF NOT EXISTS idx_hp_near_expiry_effective
    ON hospital_pharmacy.pharmacy_near_expiry_rules (effective_from DESC);

-- Default: warn-only within 30 days of expiry for any class (therapeutic_class_id NULL = global)
INSERT INTO hospital_pharmacy.pharmacy_near_expiry_rules (id, effective_from, therapeutic_class_id, days_before_expiry, action)
SELECT gen_random_uuid(), DATE '2000-01-01', NULL, 30, 'WARN'
WHERE NOT EXISTS (SELECT 1 FROM hospital_pharmacy.pharmacy_near_expiry_rules LIMIT 1);

--changeset easyops:059c-pharmacy-p3-receipt-reprint-audit
--comment: Phase P3 WS-F — receipt PDF reprint audit
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_receipt_reprint_audit (
    id UUID PRIMARY KEY,
    dispense_order_id UUID NOT NULL REFERENCES hospital_pharmacy.dispense_orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    printed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    duplicate_of_previous BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_hp_receipt_audit_order_time
    ON hospital_pharmacy.pharmacy_receipt_reprint_audit (dispense_order_id, printed_at DESC);
