--liquibase formatted sql

--changeset easyops:096-hr-loan-ad03-pi05-coa-holiday
--comment: AD-03 holiday shift flags on loan org settings; PI-05 optional COA mapping table for accounting export

SET search_path TO hr, admin, public;

-- AD-03: shift installment due dates off weekends / org holiday calendar when enabled
ALTER TABLE hr.loan_organization_settings
    ADD COLUMN IF NOT EXISTS shift_installment_due_dates_for_holidays BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS loan_holiday_shift_mode VARCHAR(32) NOT NULL DEFAULT 'NEXT_BUSINESS_DAY';

COMMENT ON COLUMN hr.loan_organization_settings.shift_installment_due_dates_for_holidays IS 'AD-03: when true, new schedules shift due dates off Sat/Sun and hr.holidays (active).';
COMMENT ON COLUMN hr.loan_organization_settings.loan_holiday_shift_mode IS 'AD-03: NEXT_BUSINESS_DAY or PREVIOUS_BUSINESS_DAY when shifting.';

-- PI-05 optional: map journal suggestion keys to org chart-of-account codes (full GL posting remains in accounting module)
CREATE TABLE IF NOT EXISTS hr.loan_accounting_coa_mappings (
    mapping_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    mapping_key VARCHAR(64) NOT NULL,
    debit_account_code VARCHAR(64) NOT NULL,
    credit_account_code VARCHAR(64) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_loan_coa_org_key UNIQUE (organization_id, mapping_key)
);

CREATE INDEX IF NOT EXISTS idx_loan_coa_org ON hr.loan_accounting_coa_mappings (organization_id);

COMMENT ON TABLE hr.loan_accounting_coa_mappings IS 'PI-05 optional: COA codes for loan disbursement/repayment export lines (journal staging).';
COMMENT ON COLUMN hr.loan_accounting_coa_mappings.mapping_key IS 'LOAN_DISBURSEMENT | LOAN_REPAYMENT (extensible).';
