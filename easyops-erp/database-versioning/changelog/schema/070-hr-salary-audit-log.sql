--liquibase formatted sql

--changeset easyops:070-hr-salary-audit-log
--comment: SS-28 Audit log for salary structure, grade, and band changes (user, timestamp, entity, action, old/new values).

SET search_path TO hr, public;

CREATE TABLE hr.salary_audit_log (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    entity_type VARCHAR(30) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    performed_by VARCHAR(100),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    old_values TEXT,
    new_values TEXT
);

CREATE INDEX idx_salary_audit_org ON hr.salary_audit_log(organization_id);
CREATE INDEX idx_salary_audit_entity ON hr.salary_audit_log(entity_type, entity_id);
CREATE INDEX idx_salary_audit_performed_at ON hr.salary_audit_log(performed_at);

COMMENT ON TABLE hr.salary_audit_log IS 'SS-28: Audit trail for salary structure, grade, and band create/update/delete';
