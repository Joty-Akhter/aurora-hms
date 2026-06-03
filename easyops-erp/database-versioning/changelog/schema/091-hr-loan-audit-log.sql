--liquibase formatted sql

--changeset easyops:091-hr-loan-audit-log
--comment: Phase 6 (RE-04): audit trail for loan principal, schedule, repayments, settlement.

SET search_path TO hr, admin, public;

CREATE TABLE hr.loan_audit_log (
    audit_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(100),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_values TEXT,
    new_values TEXT,
    CONSTRAINT fk_loan_audit_org FOREIGN KEY (organization_id)
        REFERENCES admin.organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_loan_audit_org_entity ON hr.loan_audit_log(organization_id, entity_type, entity_id);
CREATE INDEX idx_loan_audit_org_time ON hr.loan_audit_log(organization_id, performed_at DESC);

COMMENT ON TABLE hr.loan_audit_log IS 'RE-04: loan account, repayment, and settlement changes.';
COMMENT ON COLUMN hr.loan_audit_log.entity_type IS 'LOAN, REPAYMENT, or INSTALLMENT_BULK.';
