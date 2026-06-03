--liquibase formatted sql

--changeset easyops:089-hr-loan-repayment-payroll-run
--comment: Phase 4: link loan repayments to payroll runs for idempotent payroll deduction posting (PI-01–PI-04).

SET search_path TO hr, public;

ALTER TABLE hr.loan_repayment_transactions ADD COLUMN IF NOT EXISTS payroll_run_id UUID;

ALTER TABLE hr.loan_repayment_transactions
    ADD CONSTRAINT fk_loan_repay_tx_payroll_run FOREIGN KEY (payroll_run_id)
        REFERENCES hr.payroll_runs(payroll_run_id) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_loan_repay_tx_loan_payroll
    ON hr.loan_repayment_transactions(loan_id, payroll_run_id)
    WHERE payroll_run_id IS NOT NULL;

COMMENT ON COLUMN hr.loan_repayment_transactions.payroll_run_id IS 'When source=PAYROLL, idempotency key with loan_id (one posting per loan per payroll run).';
