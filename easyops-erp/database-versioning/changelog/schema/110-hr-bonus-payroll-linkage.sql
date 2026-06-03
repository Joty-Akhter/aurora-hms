--liquibase formatted sql

-- HR-PY-03: Link approved bonuses to payroll runs and track bonus amount on payroll detail.
--changeset easyops:110-hr-bonus-payroll-linkage splitStatements:false

SET search_path TO hr, admin, public;

-- payroll_run_id: set when a bonus is picked up by a payroll populate run.
ALTER TABLE hr.bonuses
    ADD COLUMN IF NOT EXISTS payroll_run_id UUID REFERENCES hr.payroll_runs(payroll_run_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_bonuses_payroll_run_id ON hr.bonuses(payroll_run_id);
CREATE INDEX IF NOT EXISTS idx_bonuses_org_status_payment_date ON hr.bonuses(organization_id, status, payment_date);

COMMENT ON COLUMN hr.bonuses.payroll_run_id IS 'HR-PY-03: Set when this bonus is included in a payroll populate run. Null = not yet processed via payroll.';

-- bonus_amount: denormalised total of approved bonus lines included in this payroll detail row.
ALTER TABLE hr.payroll_details
    ADD COLUMN IF NOT EXISTS bonus_amount NUMERIC(12, 2);

COMMENT ON COLUMN hr.payroll_details.bonus_amount IS 'HR-PY-03: Sum of approved bonus amounts applied to this employee in this payroll run.';
