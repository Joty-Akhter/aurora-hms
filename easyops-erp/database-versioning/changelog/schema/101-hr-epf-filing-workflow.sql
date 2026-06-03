--liquibase formatted sql

--changeset easyops:101-hr-epf-filing-workflow splitStatements:false

-- Dedicated EPF filing lifecycle table for ECR/challan generation and traceability.
CREATE TABLE IF NOT EXISTS hr.epf_filings (
    filing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    filing_month INTEGER NOT NULL,
    filing_year INTEGER NOT NULL,
    filing_type VARCHAR(30) NOT NULL, -- ECR, CHALLAN
    filing_status VARCHAR(30) NOT NULL DEFAULT 'draft', -- draft, generated, submitted, verified, rejected
    artifact_format VARCHAR(20), -- CSV, TEXT
    artifact_content TEXT,
    artifact_checksum VARCHAR(64),
    submission_reference VARCHAR(120),
    submission_date DATE,
    verified_date DATE,
    compliance_record_id UUID REFERENCES hr.epf_compliance_records(compliance_record_id),
    employee_contribution_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    employer_contribution_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    employer_pension_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    employer_edli_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    employer_admin_charge_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_liability_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, filing_month, filing_year, filing_type)
);

CREATE INDEX IF NOT EXISTS idx_epf_filings_org_period
    ON hr.epf_filings(organization_id, filing_year, filing_month);
CREATE INDEX IF NOT EXISTS idx_epf_filings_status
    ON hr.epf_filings(filing_status);
CREATE INDEX IF NOT EXISTS idx_epf_filings_compliance
    ON hr.epf_filings(compliance_record_id);
