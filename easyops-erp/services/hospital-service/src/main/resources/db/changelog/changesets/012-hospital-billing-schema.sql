--liquibase formatted sql

--changeset easyops:hosp-bill-001-create-schema context:hospital-billing
--comment: Create hospital_billing schema (owned by hospital-service)
CREATE SCHEMA IF NOT EXISTS hospital_billing;
GRANT ALL PRIVILEGES ON SCHEMA hospital_billing TO easyops;

--changeset easyops:hosp-bill-002-create-core-tables context:hospital-billing
--comment: Create core billing tables: charge_lines and invoices

CREATE TABLE IF NOT EXISTS hospital_billing.invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    patient_id UUID NOT NULL,
    visit_id UUID,
    payer_type VARCHAR(20) NOT NULL,
    payer_id UUID,
    status VARCHAR(20) NOT NULL,
    gross_amount NUMERIC(19,4) NOT NULL,
    total_discount NUMERIC(19,4) DEFAULT 0,
    tax_amount NUMERIC(19,4) DEFAULT 0,
    net_amount NUMERIC(19,4) NOT NULL,
    balance_due NUMERIC(19,4) NOT NULL,
    issued_at TIMESTAMPTZ,
    due_date DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE TABLE IF NOT EXISTS hospital_billing.charge_lines (
    id UUID PRIMARY KEY,
    source_service VARCHAR(50) NOT NULL,
    source_reference_id VARCHAR(255) NOT NULL,
    patient_id UUID NOT NULL,
    visit_id UUID,
    corporate_contract_id UUID,
    item_code VARCHAR(100) NOT NULL,
    item_description VARCHAR(500),
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    gross_amount NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4) DEFAULT 0,
    discount_source VARCHAR(50),
    tax_amount NUMERIC(19,4) DEFAULT 0,
    net_amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invoice_id UUID,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_charge_lines_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hb_charge_lines_idempotency
    ON hospital_billing.charge_lines (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_hb_charge_lines_patient_status_created
    ON hospital_billing.charge_lines (patient_id, status, created_at);

CREATE INDEX IF NOT EXISTS idx_hb_charge_lines_invoice
    ON hospital_billing.charge_lines (invoice_id);

CREATE INDEX IF NOT EXISTS idx_hb_invoices_patient_status
    ON hospital_billing.invoices (patient_id, status);

CREATE INDEX IF NOT EXISTS idx_hb_invoices_invoice_number
    ON hospital_billing.invoices (invoice_number);


