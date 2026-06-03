--liquibase formatted sql
+
--changeset easyops:hosp-clinical-orders-006-order-audit-logs context:hospital-clinical-orders
--comment: Phase 3 – audit log for clinical order status transitions (e.g. cancellation overrides)
CREATE TABLE IF NOT EXISTS hospital_clinical_orders.order_audit_logs (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    changed_by UUID,
    changed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    event_type VARCHAR(50),
    CONSTRAINT fk_co_order_audit_order
        FOREIGN KEY (order_id) REFERENCES hospital_clinical_orders.clinical_orders(id)
);

CREATE INDEX IF NOT EXISTS idx_co_order_audit_order
    ON hospital_clinical_orders.order_audit_logs (order_id, changed_at);

