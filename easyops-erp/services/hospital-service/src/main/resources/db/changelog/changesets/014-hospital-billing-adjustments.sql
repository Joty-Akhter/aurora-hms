--liquibase formatted sql

--changeset easyops:hosp-bill-004-create-adjustments context:hospital-billing
--comment: Create adjustments table for hospital billing (write-offs, credits, adjustments)

CREATE TABLE IF NOT EXISTS hospital_billing.adjustments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reason TEXT,
    approved_by_user_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hb_adjustments_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id)
);

CREATE INDEX IF NOT EXISTS idx_hb_adjustments_invoice
    ON hospital_billing.adjustments (invoice_id);

