--liquibase formatted sql

--changeset easyops:102-hr-epf-remittance-tracking splitStatements:false

-- Persist EPF remittance lifecycle (period, accounting posting, payment status, references).
CREATE TABLE IF NOT EXISTS hr.epf_remittances (
    remittance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    remittance_month INTEGER NOT NULL,
    remittance_year INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'pending', -- pending, posted_to_accounting, initiated, paid, failed, cancelled
    liability_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    amount_paid NUMERIC(15, 2),
    payment_date DATE,
    payment_reference VARCHAR(120),
    payment_channel VARCHAR(50),
    accounting_reference VARCHAR(120),
    accounting_posted_date DATE,
    filing_id UUID REFERENCES hr.epf_filings(filing_id),
    compliance_record_id UUID REFERENCES hr.epf_compliance_records(compliance_record_id),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, remittance_month, remittance_year)
);

CREATE INDEX IF NOT EXISTS idx_epf_remittances_org_period
    ON hr.epf_remittances(organization_id, remittance_year, remittance_month);
CREATE INDEX IF NOT EXISTS idx_epf_remittances_status
    ON hr.epf_remittances(status);
CREATE INDEX IF NOT EXISTS idx_epf_remittances_filing
    ON hr.epf_remittances(filing_id);
CREATE INDEX IF NOT EXISTS idx_epf_remittances_compliance
    ON hr.epf_remittances(compliance_record_id);
