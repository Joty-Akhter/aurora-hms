--liquibase formatted sql

--changeset easyops:086-hr-loan-categories-and-settings
--comment: Phase 1 employee loans: loan categories and per-organization loan settings (AD-01, LC-01, LC-02).

SET search_path TO hr, admin, public;

CREATE TABLE hr.loan_organization_settings (
    organization_id UUID NOT NULL PRIMARY KEY,
    min_tenure_months INTEGER NOT NULL DEFAULT 6,
    max_principal_amount NUMERIC(15, 2) NOT NULL DEFAULT 150000.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    enforce_single_active_loan BOOLEAN NOT NULL DEFAULT TRUE,
    allow_salary_advance_with_active_term_loan BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_loan_org_settings_organization FOREIGN KEY (organization_id)
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_org_min_tenure CHECK (min_tenure_months >= 0),
    CONSTRAINT chk_loan_org_max_principal CHECK (max_principal_amount > 0)
);

CREATE TABLE hr.loan_categories (
    category_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    max_principal_amount NUMERIC(15, 2),
    max_tenure_months INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_loan_categories_organization FOREIGN KEY (organization_id)
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT uk_loan_categories_org_code UNIQUE (organization_id, code),
    CONSTRAINT chk_loan_cat_max_tenure CHECK (max_tenure_months IS NULL OR max_tenure_months > 0),
    CONSTRAINT chk_loan_cat_max_principal CHECK (max_principal_amount IS NULL OR max_principal_amount > 0)
);

CREATE INDEX idx_loan_categories_org ON hr.loan_categories(organization_id);
CREATE INDEX idx_loan_categories_org_active ON hr.loan_categories(organization_id, is_active);

COMMENT ON TABLE hr.loan_organization_settings IS 'Per-organization employee loan policy (Phase 1; AD-01).';
COMMENT ON TABLE hr.loan_categories IS 'Loan categories: term loans vs salary advance (LC-01, LC-02).';
COMMENT ON COLUMN hr.loan_categories.category_type IS 'TERM_LOAN or SALARY_ADVANCE.';
COMMENT ON COLUMN hr.loan_organization_settings.allow_salary_advance_with_active_term_loan IS 'When false, salary advance is blocked if a term loan is active (configurable; EL-06).';
