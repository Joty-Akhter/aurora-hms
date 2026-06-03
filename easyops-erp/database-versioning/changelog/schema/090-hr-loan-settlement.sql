--liquibase formatted sql

--changeset easyops:090-hr-loan-settlement
--comment: Phase 5: exit settlement (ST-01–ST-05, BR-09); settlement shortfall and repayment source extensions.

SET search_path TO hr, admin, public;

ALTER TABLE hr.employee_loans
    ADD COLUMN IF NOT EXISTS settlement_shortfall_amount NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS settlement_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS separation_effective_date DATE;

COMMENT ON COLUMN hr.employee_loans.settlement_shortfall_amount IS 'ST-04: remaining debt recognized when dues insufficient (audit / legal follow-up).';
COMMENT ON COLUMN hr.employee_loans.settlement_started_at IS 'When loan moved to SETTLEMENT_PENDING.';
COMMENT ON COLUMN hr.employee_loans.separation_effective_date IS 'Optional resignation/termination effective date for settlement.';

COMMENT ON COLUMN hr.employee_loans.status IS 'PENDING_DISBURSEMENT, ACTIVE, SETTLEMENT_PENDING, CLOSED.';

ALTER TABLE hr.loan_repayment_transactions
    ALTER COLUMN source TYPE VARCHAR(30);

COMMENT ON COLUMN hr.loan_repayment_transactions.source IS 'MANUAL, PAYROLL, PF_SETTLEMENT, FINAL_SALARY, OTHER_DUES.';
