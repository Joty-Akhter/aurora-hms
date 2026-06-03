--liquibase formatted sql

--changeset easyops:100-hr-epf-statutory-split splitStatements:false

-- Store statutory employer contribution split required for EPF filing/remittance.
ALTER TABLE hr.epf_contributions
    ADD COLUMN IF NOT EXISTS employer_epf_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employer_pension_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employer_edli_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employer_admin_charge_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN hr.epf_contributions.employer_epf_amount IS 'Employer contribution portion allocated to EPF account.';
COMMENT ON COLUMN hr.epf_contributions.employer_pension_amount IS 'Employer contribution portion allocated to EPS (pension).';
COMMENT ON COLUMN hr.epf_contributions.employer_edli_amount IS 'EDLI contribution amount computed on pension wage base.';
COMMENT ON COLUMN hr.epf_contributions.employer_admin_charge_amount IS 'PF administration charge amount computed on pension wage base.';
