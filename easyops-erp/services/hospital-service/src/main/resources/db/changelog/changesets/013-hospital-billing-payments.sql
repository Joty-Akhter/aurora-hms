--liquibase formatted sql

--changeset easyops:hosp-bill-003-create-payments-and-refunds context:hospital-billing
--comment: Create payments and refunds tables for hospital billing

CREATE TABLE IF NOT EXISTS hospital_billing.payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    payment_reference VARCHAR(100),
    payment_method VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    payment_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    received_by_user_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hb_payments_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id)
);

CREATE TABLE IF NOT EXISTS hospital_billing.refunds (
    id UUID PRIMARY KEY,
    original_payment_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reason TEXT,
    processed_at TIMESTAMPTZ,
    processed_by_user_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hb_refunds_payment
        FOREIGN KEY (original_payment_id) REFERENCES hospital_billing.payments(id),
    CONSTRAINT fk_hb_refunds_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id)
);

CREATE INDEX IF NOT EXISTS idx_hb_payments_invoice
    ON hospital_billing.payments (invoice_id);

CREATE INDEX IF NOT EXISTS idx_hb_refunds_original_payment
    ON hospital_billing.refunds (original_payment_id);

CREATE INDEX IF NOT EXISTS idx_hb_refunds_invoice
    ON hospital_billing.refunds (invoice_id);

