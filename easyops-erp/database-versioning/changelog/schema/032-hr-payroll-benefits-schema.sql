--liquibase formatted sql

--changeset easyops:032-create-hr-payroll-benefits-schema splitStatements:false

-- =====================================================
-- HR PAYROLL & BENEFITS SCHEMA
-- Phase 5.3: Payroll & Benefits Administration
-- =====================================================

-- Salary Structures Table
CREATE TABLE hr.salary_structures (
    salary_structure_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    position_id UUID REFERENCES hr.positions(position_id),
    structure_name VARCHAR(200) NOT NULL,
    base_salary DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    pay_frequency VARCHAR(50) DEFAULT 'monthly',
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_salary_structure_organization ON hr.salary_structures(organization_id);
CREATE INDEX idx_salary_structure_position ON hr.salary_structures(position_id);

-- Salary Components Table
CREATE TABLE hr.salary_components (
    component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    component_name VARCHAR(100) NOT NULL,
    component_type VARCHAR(50) NOT NULL,
    calculation_type VARCHAR(50) DEFAULT 'fixed',
    is_taxable BOOLEAN DEFAULT true,
    is_statutory BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, component_name)
);

CREATE INDEX idx_salary_component_organization ON hr.salary_components(organization_id);
CREATE INDEX idx_salary_component_type ON hr.salary_components(component_type);

-- Employee Salary Details Table
CREATE TABLE hr.employee_salary_details (
    salary_detail_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    salary_structure_id UUID REFERENCES hr.salary_structures(salary_structure_id),
    component_id UUID NOT NULL REFERENCES hr.salary_components(component_id),
    amount DECIMAL(12,2) NOT NULL,
    percentage DECIMAL(5,2),
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_salary_employee ON hr.employee_salary_details(employee_id);
CREATE INDEX idx_employee_salary_organization ON hr.employee_salary_details(organization_id);
CREATE INDEX idx_employee_salary_component ON hr.employee_salary_details(component_id);

-- Payroll Runs Table
CREATE TABLE hr.payroll_runs (
    payroll_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    run_name VARCHAR(200) NOT NULL,
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    payment_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'draft',
    total_gross_pay DECIMAL(15,2),
    total_deductions DECIMAL(15,2),
    total_net_pay DECIMAL(15,2),
    employee_count INTEGER,
    processed_by UUID REFERENCES hr.employees(employee_id),
    processed_at TIMESTAMP,
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_run_organization ON hr.payroll_runs(organization_id);
CREATE INDEX idx_payroll_run_period ON hr.payroll_runs(pay_period_start, pay_period_end);
CREATE INDEX idx_payroll_run_status ON hr.payroll_runs(status);

-- Payroll Details Table
CREATE TABLE hr.payroll_details (
    payroll_detail_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_run_id UUID NOT NULL REFERENCES hr.payroll_runs(payroll_run_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    basic_salary DECIMAL(12,2) NOT NULL,
    gross_salary DECIMAL(12,2) NOT NULL,
    total_deductions DECIMAL(12,2) DEFAULT 0,
    total_reimbursements DECIMAL(12,2) DEFAULT 0,
    net_salary DECIMAL(12,2) NOT NULL,
    working_days INTEGER,
    present_days DECIMAL(5,2),
    leave_days DECIMAL(4,1),
    overtime_hours DECIMAL(6,2),
    overtime_amount DECIMAL(10,2),
    lop_days DECIMAL(4,1),
    lop_amount DECIMAL(12,2),
    status VARCHAR(50) DEFAULT 'pending',
    payment_method VARCHAR(50) DEFAULT 'bank_transfer',
    payment_reference VARCHAR(100),
    paid_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_detail_run ON hr.payroll_details(payroll_run_id);
CREATE INDEX idx_payroll_detail_employee ON hr.payroll_details(employee_id);
CREATE INDEX idx_payroll_detail_organization ON hr.payroll_details(organization_id);

COMMENT ON COLUMN hr.payroll_details.lop_days IS 'Loss-of-pay days rolled up from attendance (unpaid absence) for the pay period.';
COMMENT ON COLUMN hr.payroll_details.lop_amount IS 'LOP deduction amount applied in payroll (pro-rated from basic).';

-- Payroll Components Table (Earnings and Deductions per employee per payroll)
CREATE TABLE hr.payroll_components (
    payroll_component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_detail_id UUID NOT NULL REFERENCES hr.payroll_details(payroll_detail_id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES hr.salary_components(component_id),
    component_type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    is_taxable BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_component_detail ON hr.payroll_components(payroll_detail_id);
CREATE INDEX idx_payroll_component_type ON hr.payroll_components(component_type);

-- Tax Slabs Table
CREATE TABLE hr.tax_slabs (
    tax_slab_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    slab_name VARCHAR(100) NOT NULL,
    min_amount DECIMAL(12,2) NOT NULL,
    max_amount DECIMAL(12,2),
    tax_percentage DECIMAL(5,2) NOT NULL,
    fixed_amount DECIMAL(10,2) DEFAULT 0,
    effective_year INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_slab_organization ON hr.tax_slabs(organization_id);
CREATE INDEX idx_tax_slab_year ON hr.tax_slabs(effective_year);

-- Benefits Table
CREATE TABLE hr.benefits (
    benefit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    benefit_name VARCHAR(200) NOT NULL,
    benefit_type VARCHAR(50) NOT NULL,
    description TEXT,
    provider_name VARCHAR(200),
    coverage_type VARCHAR(50),
    employer_contribution DECIMAL(12,2),
    employee_contribution DECIMAL(12,2),
    is_mandatory BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, benefit_name)
);

CREATE INDEX idx_benefit_organization ON hr.benefits(organization_id);
CREATE INDEX idx_benefit_type ON hr.benefits(benefit_type);

-- Employee Benefits Table
CREATE TABLE hr.employee_benefits (
    employee_benefit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    benefit_id UUID NOT NULL REFERENCES hr.benefits(benefit_id),
    enrollment_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'active',
    employee_contribution DECIMAL(12,2),
    employer_contribution DECIMAL(12,2),
    coverage_amount DECIMAL(12,2),
    beneficiary_name VARCHAR(200),
    beneficiary_relationship VARCHAR(50),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_benefit_employee ON hr.employee_benefits(employee_id);
CREATE INDEX idx_employee_benefit_organization ON hr.employee_benefits(organization_id);
CREATE INDEX idx_employee_benefit_benefit ON hr.employee_benefits(benefit_id);

-- Reimbursements Table
CREATE TABLE hr.reimbursements (
    reimbursement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    reimbursement_type VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    expense_date DATE NOT NULL,
    claim_date DATE DEFAULT CURRENT_DATE,
    description TEXT,
    receipt_number VARCHAR(100),
    status VARCHAR(50) DEFAULT 'pending',
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    payment_date DATE,
    payment_reference VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reimbursement_employee ON hr.reimbursements(employee_id);
CREATE INDEX idx_reimbursement_organization ON hr.reimbursements(organization_id);
CREATE INDEX idx_reimbursement_status ON hr.reimbursements(status);

-- Bonuses Table
CREATE TABLE hr.bonuses (
    bonus_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    bonus_type VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    bonus_period VARCHAR(100),
    description TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    payment_date DATE,
    is_taxable BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bonus_employee ON hr.bonuses(employee_id);
CREATE INDEX idx_bonus_organization ON hr.bonuses(organization_id);
CREATE INDEX idx_bonus_status ON hr.bonuses(status);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================================

CREATE OR REPLACE FUNCTION hr.update_salary_structures_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_salary_structures_updated_at
    BEFORE UPDATE ON hr.salary_structures
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_salary_structures_updated_at();

CREATE OR REPLACE FUNCTION hr.update_salary_components_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_salary_components_updated_at
    BEFORE UPDATE ON hr.salary_components
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_salary_components_updated_at();

CREATE OR REPLACE FUNCTION hr.update_employee_salary_details_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employee_salary_details_updated_at
    BEFORE UPDATE ON hr.employee_salary_details
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_employee_salary_details_updated_at();

CREATE OR REPLACE FUNCTION hr.update_payroll_runs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payroll_runs_updated_at
    BEFORE UPDATE ON hr.payroll_runs
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_payroll_runs_updated_at();

CREATE OR REPLACE FUNCTION hr.update_payroll_details_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payroll_details_updated_at
    BEFORE UPDATE ON hr.payroll_details
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_payroll_details_updated_at();

CREATE OR REPLACE FUNCTION hr.update_tax_slabs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tax_slabs_updated_at
    BEFORE UPDATE ON hr.tax_slabs
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_tax_slabs_updated_at();

CREATE OR REPLACE FUNCTION hr.update_benefits_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_benefits_updated_at
    BEFORE UPDATE ON hr.benefits
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_benefits_updated_at();

CREATE OR REPLACE FUNCTION hr.update_employee_benefits_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employee_benefits_updated_at
    BEFORE UPDATE ON hr.employee_benefits
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_employee_benefits_updated_at();

CREATE OR REPLACE FUNCTION hr.update_reimbursements_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reimbursements_updated_at
    BEFORE UPDATE ON hr.reimbursements
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_reimbursements_updated_at();

CREATE OR REPLACE FUNCTION hr.update_bonuses_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bonuses_updated_at
    BEFORE UPDATE ON hr.bonuses
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_bonuses_updated_at();

-- HR Payroll & Benefits schema complete

