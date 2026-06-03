--liquibase formatted sql

--changeset easyops:093-hr-loan-overrides-reversal-writeoff
--comment: BR-08 salary advance workflow flag; AD-02/LC-05 limit & facility overrides; RP-05 reversal link; ST-04 write-off path

SET search_path TO hr, admin, public;

ALTER TABLE hr.loan_organization_settings
    ADD COLUMN IF NOT EXISTS salary_advance_skip_finance_approval BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN hr.loan_organization_settings.salary_advance_skip_finance_approval IS 'BR-08: when true, salary advance applications skip Finance step (HR approval finalizes).';

ALTER TABLE hr.loan_applications
    ADD COLUMN IF NOT EXISTS limit_override_reason VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS limit_override_approved_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS limit_override_expires_at DATE,
    ADD COLUMN IF NOT EXISTS facility_override_reason VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS facility_override_approved_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS facility_override_expires_at DATE;

COMMENT ON COLUMN hr.loan_applications.limit_override_reason IS 'AD-02: narrative when requested amount exceeds policy/category caps.';
COMMENT ON COLUMN hr.loan_applications.facility_override_reason IS 'LC-05/AD-02: narrative when second facility would otherwise be blocked (BR-02).';

ALTER TABLE hr.employee_loans
    ADD COLUMN IF NOT EXISTS settlement_write_off_path VARCHAR(40),
    ADD COLUMN IF NOT EXISTS legal_case_reference VARCHAR(200),
    ADD COLUMN IF NOT EXISTS write_off_notes TEXT;

COMMENT ON COLUMN hr.employee_loans.settlement_write_off_path IS 'ST-04: e.g. LEGAL_REVIEW, BOARD_WRITE_OFF, WRITTEN_OFF.';
COMMENT ON COLUMN hr.employee_loans.legal_case_reference IS 'ST-04: external case / file reference when legal path.';

ALTER TABLE hr.loan_repayment_transactions
    ADD COLUMN IF NOT EXISTS reverses_transaction_id UUID;

COMMENT ON COLUMN hr.loan_repayment_transactions.reverses_transaction_id IS 'RP-05: when set, this row reverses the referenced PAYROLL posting (amount may be negative).';

CREATE UNIQUE INDEX IF NOT EXISTS uq_loan_repayment_one_reversal
    ON hr.loan_repayment_transactions (reverses_transaction_id)
    WHERE reverses_transaction_id IS NOT NULL;
