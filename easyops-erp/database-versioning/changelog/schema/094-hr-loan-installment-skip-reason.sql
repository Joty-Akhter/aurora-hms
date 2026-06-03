--liquibase formatted sql

--changeset easyops:094-hr-loan-installment-skip-reason
--comment: RP-01 skipped-with-reason on installments

SET search_path TO hr, admin, public;

ALTER TABLE hr.loan_installments
    ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(2000);

COMMENT ON COLUMN hr.loan_installments.skip_reason IS 'RP-01: when status=SKIPPED, administrative reason (installment waived/non-collected per policy).';
COMMENT ON COLUMN hr.loan_installments.status IS 'DUE, PARTIAL, PAID, SKIPPED';
