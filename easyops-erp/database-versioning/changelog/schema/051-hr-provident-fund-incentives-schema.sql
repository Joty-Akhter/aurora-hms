--liquibase formatted sql

--changeset easyops:051-create-hr-provident-fund-incentives-schema splitStatements:false

-- =====================================================
-- HR PROVIDENT FUND & INCENTIVES SCHEMA
-- Phase 1: Foundation & Core Infrastructure
-- =====================================================

-- =====================================================
-- PROVIDENT FUND TABLES
-- =====================================================

-- Employee Provident Fund (EPF) Accounts Table
CREATE TABLE hr.epf_accounts (
    epf_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    epf_account_number VARCHAR(50) NOT NULL,
    uan_number VARCHAR(50), -- Universal Account Number
    account_status VARCHAR(50) DEFAULT 'active', -- active, closed, transferred
    opening_date DATE NOT NULL,
    closing_date DATE,
    current_balance DECIMAL(15,2) DEFAULT 0,
    employee_contribution_balance DECIMAL(15,2) DEFAULT 0,
    employer_contribution_balance DECIMAL(15,2) DEFAULT 0,
    interest_balance DECIMAL(15,2) DEFAULT 0,
    last_contribution_date DATE,
    last_interest_calculation_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, epf_account_number)
);

CREATE INDEX idx_epf_account_employee ON hr.epf_accounts(employee_id);
CREATE INDEX idx_epf_account_organization ON hr.epf_accounts(organization_id);
CREATE INDEX idx_epf_account_number ON hr.epf_accounts(epf_account_number);
CREATE INDEX idx_epf_account_status ON hr.epf_accounts(account_status);

-- EPF Contributions Table
CREATE TABLE hr.epf_contributions (
    contribution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    contribution_period_start DATE NOT NULL,
    contribution_period_end DATE NOT NULL,
    contribution_month INTEGER NOT NULL,
    contribution_year INTEGER NOT NULL,
    employee_basic_salary DECIMAL(12,2) NOT NULL,
    employee_contribution_rate DECIMAL(5,2) DEFAULT 12.00, -- Percentage
    employee_contribution_amount DECIMAL(12,2) NOT NULL,
    employer_contribution_rate DECIMAL(5,2) DEFAULT 12.00, -- Percentage
    employer_contribution_amount DECIMAL(12,2) NOT NULL,
    total_contribution DECIMAL(12,2) NOT NULL,
    interest_rate DECIMAL(5,2),
    interest_amount DECIMAL(12,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending', -- pending, processed, failed
    processed_date DATE,
    payroll_run_id UUID REFERENCES hr.payroll_runs(payroll_run_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_contribution_account ON hr.epf_contributions(epf_account_id);
CREATE INDEX idx_epf_contribution_employee ON hr.epf_contributions(employee_id);
CREATE INDEX idx_epf_contribution_organization ON hr.epf_contributions(organization_id);
CREATE INDEX idx_epf_contribution_period ON hr.epf_contributions(contribution_month, contribution_year);
CREATE INDEX idx_epf_contribution_payroll ON hr.epf_contributions(payroll_run_id);

-- EPF Interest Calculations Table
CREATE TABLE hr.epf_interest_calculations (
    interest_calculation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    financial_year INTEGER NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    opening_balance DECIMAL(15,2) NOT NULL,
    total_contributions DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    closing_balance DECIMAL(15,2) NOT NULL,
    calculation_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'calculated', -- calculated, posted, reversed
    posted_date DATE,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_interest_account ON hr.epf_interest_calculations(epf_account_id);
CREATE INDEX idx_epf_interest_organization ON hr.epf_interest_calculations(organization_id);
CREATE INDEX idx_epf_interest_financial_year ON hr.epf_interest_calculations(financial_year);

-- EPF Withdrawals Table
CREATE TABLE hr.epf_withdrawals (
    withdrawal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    withdrawal_type VARCHAR(50) NOT NULL, -- partial, full, advance, pension
    withdrawal_reason VARCHAR(200),
    requested_amount DECIMAL(15,2) NOT NULL,
    approved_amount DECIMAL(15,2),
    withdrawal_date DATE,
    status VARCHAR(50) DEFAULT 'pending', -- pending, approved, rejected, processed, cancelled
    approval_workflow JSONB,
    processed_date DATE,
    payment_reference VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_withdrawal_account ON hr.epf_withdrawals(epf_account_id);
CREATE INDEX idx_epf_withdrawal_employee ON hr.epf_withdrawals(employee_id);
CREATE INDEX idx_epf_withdrawal_organization ON hr.epf_withdrawals(organization_id);
CREATE INDEX idx_epf_withdrawal_status ON hr.epf_withdrawals(status);

-- EPF Transfers Table
CREATE TABLE hr.epf_transfers (
    transfer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id),
    target_epf_account_id UUID REFERENCES hr.epf_accounts(epf_account_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    transfer_type VARCHAR(50) NOT NULL, -- inter_organization, intra_organization
    transfer_amount DECIMAL(15,2) NOT NULL,
    transfer_date DATE,
    status VARCHAR(50) DEFAULT 'pending', -- pending, processed, failed
    source_uan_number VARCHAR(50),
    target_uan_number VARCHAR(50),
    transfer_reference VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_transfer_source ON hr.epf_transfers(source_epf_account_id);
CREATE INDEX idx_epf_transfer_target ON hr.epf_transfers(target_epf_account_id);
CREATE INDEX idx_epf_transfer_employee ON hr.epf_transfers(employee_id);
CREATE INDEX idx_epf_transfer_status ON hr.epf_transfers(status);

-- EPF Nominations Table
CREATE TABLE hr.epf_nominations (
    nomination_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epf_account_id UUID NOT NULL REFERENCES hr.epf_accounts(epf_account_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    nominee_name VARCHAR(200) NOT NULL,
    nominee_relationship VARCHAR(100) NOT NULL,
    nominee_date_of_birth DATE,
    nominee_address TEXT,
    nominee_phone VARCHAR(50),
    nominee_email VARCHAR(255),
    share_percentage DECIMAL(5,2) NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_nomination_account ON hr.epf_nominations(epf_account_id);
CREATE INDEX idx_epf_nomination_employee ON hr.epf_nominations(employee_id);

-- EPF Compliance Records Table
CREATE TABLE hr.epf_compliance_records (
    compliance_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    compliance_type VARCHAR(100) NOT NULL, -- monthly_return, annual_return, challan
    compliance_period_start DATE NOT NULL,
    compliance_period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    submission_date DATE,
    status VARCHAR(50) DEFAULT 'pending', -- pending, submitted, verified, rejected
    file_reference VARCHAR(255),
    amount DECIMAL(15,2),
    penalty_amount DECIMAL(12,2) DEFAULT 0,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_epf_compliance_organization ON hr.epf_compliance_records(organization_id);
CREATE INDEX idx_epf_compliance_type ON hr.epf_compliance_records(compliance_type);
CREATE INDEX idx_epf_compliance_status ON hr.epf_compliance_records(status);
CREATE INDEX idx_epf_compliance_period ON hr.epf_compliance_records(compliance_period_start, compliance_period_end);

-- =====================================================
-- INCENTIVES TABLES
-- =====================================================

-- Incentive Plans Table
CREATE TABLE hr.incentive_plans (
    incentive_plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    plan_name VARCHAR(200) NOT NULL,
    plan_code VARCHAR(50) NOT NULL,
    plan_type VARCHAR(50) NOT NULL, -- performance, sales, project, retention, referral
    description TEXT,
    eligibility_rules JSONB, -- JSON structure for flexible eligibility rules
    calculation_formula TEXT, -- Formula or script for calculation
    calculation_type VARCHAR(50) DEFAULT 'formula', -- formula, script, manual
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT true,
    approval_required BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, plan_code)
);

CREATE INDEX idx_incentive_plan_organization ON hr.incentive_plans(organization_id);
CREATE INDEX idx_incentive_plan_type ON hr.incentive_plans(plan_type);
CREATE INDEX idx_incentive_plan_active ON hr.incentive_plans(is_active);

-- Employee Incentive Eligibility Table
CREATE TABLE hr.employee_incentive_eligibility (
    eligibility_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incentive_plan_id UUID NOT NULL REFERENCES hr.incentive_plans(incentive_plan_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    department_id UUID REFERENCES admin.departments(id),
    is_eligible BOOLEAN DEFAULT true,
    eligibility_start_date DATE,
    eligibility_end_date DATE,
    eligibility_notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(incentive_plan_id, employee_id)
);

CREATE INDEX idx_incentive_eligibility_plan ON hr.employee_incentive_eligibility(incentive_plan_id);
CREATE INDEX idx_incentive_eligibility_employee ON hr.employee_incentive_eligibility(employee_id);
CREATE INDEX idx_incentive_eligibility_organization ON hr.employee_incentive_eligibility(organization_id);

-- Sales Targets Table
CREATE TABLE hr.sales_targets (
    sales_target_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    department_id UUID REFERENCES admin.departments(id),
    target_period_start DATE NOT NULL,
    target_period_end DATE NOT NULL,
    target_month INTEGER NOT NULL,
    target_year INTEGER NOT NULL,
    target_type VARCHAR(50) DEFAULT 'individual', -- individual, team, department
    target_amount DECIMAL(15,2) NOT NULL,
    target_currency VARCHAR(3) DEFAULT 'USD',
    target_quantity INTEGER, -- For quantity-based targets
    achievement_amount DECIMAL(15,2) DEFAULT 0,
    achievement_percentage DECIMAL(5,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'draft', -- draft, assigned, active, completed, cancelled
    assigned_by UUID REFERENCES hr.employees(employee_id),
    assigned_date DATE,
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_date DATE,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_target_employee ON hr.sales_targets(employee_id);
CREATE INDEX idx_sales_target_organization ON hr.sales_targets(organization_id);
CREATE INDEX idx_sales_target_department ON hr.sales_targets(department_id);
CREATE INDEX idx_sales_target_period ON hr.sales_targets(target_month, target_year);
CREATE INDEX idx_sales_target_status ON hr.sales_targets(status);

-- Sales Achievements Table
CREATE TABLE hr.sales_achievements (
    achievement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_target_id UUID NOT NULL REFERENCES hr.sales_targets(sales_target_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    achievement_date DATE NOT NULL,
    achievement_amount DECIMAL(15,2) NOT NULL,
    achievement_quantity INTEGER,
    sales_order_id UUID, -- Reference to sales order if integrated
    customer_id UUID, -- Reference to customer
    product_id UUID, -- Reference to product
    verified BOOLEAN DEFAULT false,
    verified_by UUID REFERENCES hr.employees(employee_id),
    verified_date DATE,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_achievement_target ON hr.sales_achievements(sales_target_id);
CREATE INDEX idx_sales_achievement_employee ON hr.sales_achievements(employee_id);
CREATE INDEX idx_sales_achievement_organization ON hr.sales_achievements(organization_id);
CREATE INDEX idx_sales_achievement_date ON hr.sales_achievements(achievement_date);
CREATE INDEX idx_sales_achievement_order ON hr.sales_achievements(sales_order_id);

-- Incentive Calculations Table
CREATE TABLE hr.incentive_calculations (
    calculation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incentive_plan_id UUID NOT NULL REFERENCES hr.incentive_plans(incentive_plan_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    calculation_period_start DATE NOT NULL,
    calculation_period_end DATE NOT NULL,
    calculation_month INTEGER,
    calculation_year INTEGER,
    base_amount DECIMAL(15,2), -- Base amount for calculation (e.g., salary, sales amount)
    achievement_percentage DECIMAL(5,2), -- Achievement percentage if applicable
    incentive_rate DECIMAL(5,2), -- Rate or percentage for calculation
    calculated_amount DECIMAL(15,2) NOT NULL,
    approved_amount DECIMAL(15,2),
    adjustment_amount DECIMAL(15,2) DEFAULT 0,
    final_amount DECIMAL(15,2) NOT NULL,
    tier_level VARCHAR(50), -- Tier level if tiered structure
    calculation_details JSONB, -- Detailed calculation breakdown
    status VARCHAR(50) DEFAULT 'calculated', -- calculated, approved, paid, cancelled
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_date DATE,
    payroll_run_id UUID REFERENCES hr.payroll_runs(payroll_run_id),
    paid_date DATE,
    payment_reference VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incentive_calculation_plan ON hr.incentive_calculations(incentive_plan_id);
CREATE INDEX idx_incentive_calculation_employee ON hr.incentive_calculations(employee_id);
CREATE INDEX idx_incentive_calculation_organization ON hr.incentive_calculations(organization_id);
CREATE INDEX idx_incentive_calculation_period ON hr.incentive_calculations(calculation_month, calculation_year);
CREATE INDEX idx_incentive_calculation_status ON hr.incentive_calculations(status);
CREATE INDEX idx_incentive_calculation_payroll ON hr.incentive_calculations(payroll_run_id);

-- Incentive Payout Tracking Table
CREATE TABLE hr.incentive_payouts (
    payout_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    calculation_id UUID NOT NULL REFERENCES hr.incentive_calculations(calculation_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    payout_amount DECIMAL(15,2) NOT NULL,
    payout_date DATE NOT NULL,
    payout_method VARCHAR(50) DEFAULT 'salary', -- salary, separate_payment, bank_transfer
    payroll_run_id UUID REFERENCES hr.payroll_runs(payroll_run_id),
    payment_reference VARCHAR(100),
    status VARCHAR(50) DEFAULT 'pending', -- pending, processed, failed, reversed
    processed_date DATE,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incentive_payout_calculation ON hr.incentive_payouts(calculation_id);
CREATE INDEX idx_incentive_payout_employee ON hr.incentive_payouts(employee_id);
CREATE INDEX idx_incentive_payout_organization ON hr.incentive_payouts(organization_id);
CREATE INDEX idx_incentive_payout_date ON hr.incentive_payouts(payout_date);
CREATE INDEX idx_incentive_payout_status ON hr.incentive_payouts(status);

-- Incentive Audit Trail Table
CREATE TABLE hr.incentive_audit_trail (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    entity_type VARCHAR(50) NOT NULL, -- plan, calculation, payout, target
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- create, update, delete, approve, reject, calculate
    old_values JSONB,
    new_values JSONB,
    changed_by UUID REFERENCES hr.employees(employee_id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT
);

CREATE INDEX idx_incentive_audit_organization ON hr.incentive_audit_trail(organization_id);
CREATE INDEX idx_incentive_audit_entity ON hr.incentive_audit_trail(entity_type, entity_id);
CREATE INDEX idx_incentive_audit_changed_at ON hr.incentive_audit_trail(changed_at);

