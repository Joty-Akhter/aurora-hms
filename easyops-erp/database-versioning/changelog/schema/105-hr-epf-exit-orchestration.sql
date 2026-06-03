--liquibase formatted sql

--changeset easyops:105-hr-epf-exit-orchestration splitStatements:false

-- EPF exit orchestration: tracks employee termination transfer/settlement lifecycle.
CREATE TABLE IF NOT EXISTS hr.epf_exit_cases (
    exit_case_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    source_epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id),
    target_epf_account_id UUID REFERENCES hr.epf_accounts(epf_account_id),
    exit_type VARCHAR(40) NOT NULL, -- transfer, settlement, close_only
    status VARCHAR(40) NOT NULL DEFAULT 'initiated', -- initiated, transfer_pending, settlement_pending, completed, cancelled
    termination_date DATE,
    transfer_id UUID REFERENCES hr.epf_transfers(transfer_id),
    withdrawal_id UUID REFERENCES hr.epf_withdrawals(withdrawal_id),
    completion_reference VARCHAR(120),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_epf_exit_cases_org_created
    ON hr.epf_exit_cases(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_epf_exit_cases_employee
    ON hr.epf_exit_cases(employee_id, created_at DESC);
