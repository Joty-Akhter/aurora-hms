--liquibase formatted sql

--changeset easyops:053-create-hr-advanced-incentives-schema splitStatements:false

-- =====================================================
-- ADVANCED INCENTIVES SCHEMA
-- Phase 5: Advanced Incentives Features
-- =====================================================

-- Project Incentives Table
CREATE TABLE hr.project_incentives (
    project_incentive_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incentive_plan_id UUID NOT NULL REFERENCES hr.incentive_plans(incentive_plan_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    project_id UUID NOT NULL,
    project_name VARCHAR(255),
    project_value DECIMAL(15,2),
    milestone_id UUID,
    milestone_name VARCHAR(255),
    completion_percentage DECIMAL(5,2),
    milestone_bonus_amount DECIMAL(15,2),
    completion_bonus_amount DECIMAL(15,2),
    total_incentive_amount DECIMAL(15,2),
    milestone_date DATE,
    project_completion_date DATE,
    status VARCHAR(50) DEFAULT 'pending',
    distribution_method VARCHAR(50),
    distribution_details JSONB,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_incentive_plan ON hr.project_incentives(incentive_plan_id);
CREATE INDEX idx_project_incentive_employee ON hr.project_incentives(employee_id);
CREATE INDEX idx_project_incentive_project ON hr.project_incentives(project_id);
CREATE INDEX idx_project_incentive_status ON hr.project_incentives(status);

-- Retention Bonuses Table
CREATE TABLE hr.retention_bonuses (
    retention_bonus_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incentive_plan_id UUID NOT NULL REFERENCES hr.incentive_plans(incentive_plan_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    bonus_type VARCHAR(50),
    bonus_amount DECIMAL(15,2) NOT NULL,
    retention_period_months INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    vesting_schedule JSONB,
    vested_amount DECIMAL(15,2) DEFAULT 0,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'active',
    forfeiture_reason TEXT,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_retention_bonus_plan ON hr.retention_bonuses(incentive_plan_id);
CREATE INDEX idx_retention_bonus_employee ON hr.retention_bonuses(employee_id);
CREATE INDEX idx_retention_bonus_status ON hr.retention_bonuses(status);
CREATE INDEX idx_retention_bonus_dates ON hr.retention_bonuses(start_date, end_date);

-- Referral Incentives Table
CREATE TABLE hr.referral_incentives (
    referral_incentive_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incentive_plan_id UUID NOT NULL REFERENCES hr.incentive_plans(incentive_plan_id),
    referrer_employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    referred_candidate_id UUID,
    referred_employee_id UUID REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    referral_date DATE NOT NULL,
    candidate_name VARCHAR(200),
    candidate_email VARCHAR(255),
    position_applied VARCHAR(200),
    hiring_date DATE,
    validation_status VARCHAR(50) DEFAULT 'pending',
    validated_by UUID REFERENCES hr.employees(employee_id),
    validated_date DATE,
    validation_notes TEXT,
    incentive_amount DECIMAL(15,2),
    payout_schedule VARCHAR(50),
    paid_amount DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_referral_incentive_plan ON hr.referral_incentives(incentive_plan_id);
CREATE INDEX idx_referral_incentive_referrer ON hr.referral_incentives(referrer_employee_id);
CREATE INDEX idx_referral_incentive_referred ON hr.referral_incentives(referred_employee_id);
CREATE INDEX idx_referral_incentive_status ON hr.referral_incentives(status);
CREATE INDEX idx_referral_incentive_validation ON hr.referral_incentives(validation_status);

-- Incentive Disputes Table
CREATE TABLE hr.incentive_disputes (
    dispute_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    calculation_id UUID NOT NULL REFERENCES hr.incentive_calculations(calculation_id),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    dispute_type VARCHAR(50),
    dispute_reason TEXT NOT NULL,
    disputed_amount DECIMAL(15,2),
    expected_amount DECIMAL(15,2),
    status VARCHAR(50) DEFAULT 'submitted',
    submitted_by UUID NOT NULL REFERENCES hr.employees(employee_id),
    submitted_date DATE NOT NULL,
    assigned_to UUID REFERENCES hr.employees(employee_id),
    resolution TEXT,
    resolved_amount DECIMAL(15,2),
    resolved_by UUID REFERENCES hr.employees(employee_id),
    resolved_date DATE,
    resolution_notes TEXT,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incentive_dispute_calculation ON hr.incentive_disputes(calculation_id);
CREATE INDEX idx_incentive_dispute_employee ON hr.incentive_disputes(employee_id);
CREATE INDEX idx_incentive_dispute_status ON hr.incentive_disputes(status);
CREATE INDEX idx_incentive_dispute_assigned ON hr.incentive_disputes(assigned_to);

-- Incentive Notifications Table
CREATE TABLE hr.incentive_notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    employee_id UUID REFERENCES hr.employees(employee_id),
    notification_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL, -- calculation, payout, dispute, target
    entity_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(50) DEFAULT 'normal', -- low, normal, high, urgent
    status VARCHAR(50) DEFAULT 'unread', -- unread, read, archived
    read_at TIMESTAMP,
    action_required BOOLEAN DEFAULT false,
    action_url VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incentive_notification_employee ON hr.incentive_notifications(employee_id);
CREATE INDEX idx_incentive_notification_status ON hr.incentive_notifications(status);
CREATE INDEX idx_incentive_notification_entity ON hr.incentive_notifications(entity_type, entity_id);
CREATE INDEX idx_incentive_notification_created ON hr.incentive_notifications(created_at);

