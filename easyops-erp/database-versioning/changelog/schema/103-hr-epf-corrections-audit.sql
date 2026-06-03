--liquibase formatted sql

--changeset easyops:103-hr-epf-corrections-audit splitStatements:false

-- Audit log for EPF correction lifecycle (reversals/adjustments).
CREATE TABLE IF NOT EXISTS hr.epf_correction_logs (
    correction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    epf_account_id UUID REFERENCES hr.epf_accounts(epf_account_id),
    entity_type VARCHAR(40) NOT NULL, -- contribution, withdrawal, transfer
    entity_id UUID NOT NULL,
    action_type VARCHAR(40) NOT NULL, -- reversal, adjustment
    amount_impact NUMERIC(15, 2),
    reason TEXT NOT NULL,
    notes TEXT,
    reversed_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_epf_correction_logs_org_created
    ON hr.epf_correction_logs(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_epf_correction_logs_entity
    ON hr.epf_correction_logs(entity_type, entity_id);
