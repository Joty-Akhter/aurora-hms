--liquibase formatted sql

--changeset easyops:095-hr-loan-should-gaps-lc04-notifications-legal
--comment: LC-04 category interest method; RE-03 loan_notification_events; ST-04 legal_workflow on employee_loans

SET search_path TO hr, admin, public;

-- LC-04: interest model on categories (schedule remains zero-interest equal-principal until engine supports flat/reducing)
ALTER TABLE hr.loan_categories
    ADD COLUMN IF NOT EXISTS interest_method VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS flat_annual_rate_percent NUMERIC(9, 4) NULL;

COMMENT ON COLUMN hr.loan_categories.interest_method IS 'LC-04: NONE | FLAT | REDUCING_BALANCE — v1 schedule uses equal principal + zero interest when NONE only.';
COMMENT ON COLUMN hr.loan_categories.flat_annual_rate_percent IS 'LC-04: annual % for FLAT interest when implemented; optional hint for REDUCING_BALANCE.';

UPDATE hr.loan_categories SET interest_method = 'NONE' WHERE interest_method IS NULL;

-- RE-03: in-app loan notifications for employees (and future HR routing)
CREATE TABLE IF NOT EXISTS hr.loan_notification_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    title VARCHAR(400) NOT NULL,
    body TEXT,
    loan_application_id UUID,
    loan_id UUID,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loan_notif_recipient_org_created
    ON hr.loan_notification_events (recipient_user_id, organization_id, created_at DESC);

COMMENT ON TABLE hr.loan_notification_events IS 'RE-03: loan-related notifications for linked employee user (self-service feed).';

-- ST-04: lightweight legal workflow state (not a full task engine)
ALTER TABLE hr.employee_loans
    ADD COLUMN IF NOT EXISTS legal_workflow_status VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS legal_workflow_updated_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN hr.employee_loans.legal_workflow_status IS 'ST-04: e.g. PENDING_LEGAL, PENDING_BOARD, RESOLVED — configurable labels; not a full BPM.';
COMMENT ON COLUMN hr.employee_loans.legal_workflow_updated_at IS 'ST-04: last change to legal_workflow_status.';
