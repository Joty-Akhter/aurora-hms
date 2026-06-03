--liquibase formatted sql

--changeset easyops:097-hr-epf-organization-policy splitStatements:false

-- INT-09–INT-12: Org-scoped EPF policy (rates, PF wage ceiling/floor, employment eligibility).
CREATE TABLE hr.epf_organization_policy (
    organization_id UUID PRIMARY KEY REFERENCES admin.organizations(id) ON DELETE CASCADE,
    employee_contribution_rate NUMERIC(5, 2) NOT NULL DEFAULT 12.00,
    employer_contribution_rate NUMERIC(5, 2) NOT NULL DEFAULT 12.00,
    pf_wage_ceiling NUMERIC(14, 2),
    pf_wage_floor NUMERIC(14, 2),
    eligible_employment_types VARCHAR(500),
    ineligible_employment_types VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE hr.epf_organization_policy IS 'Per-organization EPF rates and PF wage rules (INT-09/INT-12).';
COMMENT ON COLUMN hr.epf_organization_policy.pf_wage_ceiling IS 'Max PF wage base per month; NULL = no ceiling.';
COMMENT ON COLUMN hr.epf_organization_policy.pf_wage_floor IS 'Min PF wage to qualify; NULL = no floor.';
COMMENT ON COLUMN hr.epf_organization_policy.eligible_employment_types IS 'Comma list (e.g. FULL_TIME,PART_TIME); empty = all types eligible.';
COMMENT ON COLUMN hr.epf_organization_policy.ineligible_employment_types IS 'Comma list excluded from PF (e.g. INTERN,CONTRACT).';

ALTER TABLE hr.epf_contributions
    ADD COLUMN IF NOT EXISTS pf_wage_base NUMERIC(14, 2);

COMMENT ON COLUMN hr.epf_contributions.pf_wage_base IS 'PF wage used for contribution (after ceiling); INT-09.';
