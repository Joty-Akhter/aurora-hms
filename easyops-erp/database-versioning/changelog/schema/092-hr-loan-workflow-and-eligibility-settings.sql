--liquibase formatted sql

--changeset easyops:092-hr-loan-workflow-and-eligibility-settings
--comment: AL-03 multi-step approval, AL-01 attachment refs, EL-02 disqualifying statuses, ST-03 settlement priority

SET search_path TO hr, admin, public;

ALTER TABLE hr.loan_applications
    ADD COLUMN IF NOT EXISTS attachment_references TEXT,
    ADD COLUMN IF NOT EXISTS delegated_to_user_id UUID,
    ADD COLUMN IF NOT EXISTS clarification_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS clarification_requested_by_user_id UUID;

COMMENT ON COLUMN hr.loan_applications.attachment_references IS 'JSON array of attachment reference strings (URLs or document ids).';
COMMENT ON COLUMN hr.loan_applications.delegated_to_user_id IS 'AL-03: when set, HR approval step may be limited to this user.';
COMMENT ON COLUMN hr.loan_applications.clarification_message IS 'AL-03: last clarification request text.';
COMMENT ON COLUMN hr.loan_applications.clarification_requested_by_user_id IS 'AL-03: user who requested clarification.';

COMMENT ON COLUMN hr.loan_applications.status IS 'DRAFT, SUBMITTED, PENDING_FINANCE_APPROVAL, AWAITING_CLARIFICATION, APPROVED, REJECTED, CANCELLED.';

ALTER TABLE hr.loan_organization_settings
    ADD COLUMN IF NOT EXISTS disqualifying_employment_statuses TEXT NOT NULL DEFAULT '["LONG_TERM_SUSPENSION","SUSPENDED"]',
    ADD COLUMN IF NOT EXISTS settlement_allocation_priority TEXT NOT NULL DEFAULT '["PF_SETTLEMENT","FINAL_SALARY","OTHER_DUES"]',
    ADD COLUMN IF NOT EXISTS enforce_settlement_allocation_order BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS skip_finance_approval BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN hr.loan_organization_settings.disqualifying_employment_statuses IS 'JSON array of employment_status codes that block loan eligibility (EL-02), case-insensitive match.';
COMMENT ON COLUMN hr.loan_organization_settings.settlement_allocation_priority IS 'JSON array: order of LoanRepaymentSource for exit settlement (ST-03).';
COMMENT ON COLUMN hr.loan_organization_settings.enforce_settlement_allocation_order IS 'When true, PF must be allocated first if PF amount available (ST-02/ST-03).';
COMMENT ON COLUMN hr.loan_organization_settings.skip_finance_approval IS 'When true, HR approval finalizes application (single-step); when false, Finance approval required (AL-03).';
