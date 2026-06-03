--liquibase formatted sql

--changeset easyops:104-hr-epf-audit-events-ledger splitStatements:false

-- Immutable event-level audit ledger for EPF domain actions.
CREATE TABLE IF NOT EXISTS hr.epf_audit_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    epf_account_id UUID REFERENCES hr.epf_accounts(epf_account_id),
    employee_id UUID REFERENCES hr.employees(employee_id),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    event_type VARCHAR(80) NOT NULL,
    actor_user_id VARCHAR(100),
    event_data TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_epf_audit_events_org_created
    ON hr.epf_audit_events(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_epf_audit_events_entity
    ON hr.epf_audit_events(entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_epf_audit_events_account
    ON hr.epf_audit_events(epf_account_id, created_at DESC);
