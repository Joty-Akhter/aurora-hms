--liquibase formatted sql

--changeset easyops:052-create-hr-sales-target-configuration-schema splitStatements:false

-- =====================================================
-- SALES TARGET CONFIGURATION SCHEMA
-- Phase 4: Sales Targets & Achievement-Based Incentives
-- =====================================================

-- Sales Target Configurations Table
CREATE TABLE hr.sales_target_configurations (
    configuration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    configuration_name VARCHAR(200) NOT NULL,
    configuration_type VARCHAR(50) NOT NULL, -- department, role, product, geographic
    target_setting_rules JSONB, -- JSON structure for flexible rules
    incentive_calculation_formula TEXT, -- Formula or script for calculation
    tier_structure JSONB, -- JSON structure for tier definitions
    minimum_achievement_threshold DECIMAL(5,2),
    maximum_incentive_cap DECIMAL(15,2),
    accelerator_rate DECIMAL(5,2), -- Bonus rate for exceeding 100%
    penalty_rate DECIMAL(5,2), -- Penalty rate if applicable
    department_id UUID REFERENCES admin.departments(id),
    role_name VARCHAR(100), -- Sales Rep, Sales Manager, etc.
    product_category VARCHAR(100),
    geographic_territory VARCHAR(100),
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_target_config_organization ON hr.sales_target_configurations(organization_id);
CREATE INDEX idx_sales_target_config_type ON hr.sales_target_configurations(configuration_type);
CREATE INDEX idx_sales_target_config_department ON hr.sales_target_configurations(department_id);
CREATE INDEX idx_sales_target_config_role ON hr.sales_target_configurations(role_name);
CREATE INDEX idx_sales_target_config_active ON hr.sales_target_configurations(is_active);

