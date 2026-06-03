--liquibase formatted sql

--changeset easyops:090-alien-pharma-seed context:demo
--comment: Alien Pharma full initialization. Destructive - run only with context demo.

-- ============================================================================
-- ALIEN PHARMA INITIALIZATION SCRIPT
-- ScriptUtils-safe version (no DO $$ blocks)
-- This script does everything in one go:
-- 1. Deletes all existing organizations and related data
-- 2. Creates Alien Pharma organization
-- 3. Creates roles and permissions
-- 4. Links users to organization
-- 5. Creates departments (admin and hr schemas)
-- 6. Creates product categories
-- 7. Creates HR departments and positions
-- 8. Creates employee record for admin user
-- 9. Creates sample pharmaceutical products
-- 10. Inserts UOMs
-- ============================================================================

-- ============================================================================
-- STEP 1: DELETE ALL EXISTING ORGANIZATIONS AND RELATED DATA
-- ============================================================================
-- Delete in dependency order to avoid foreign key constraint violations

-- Sales module (delete in dependency order)
DELETE FROM sales.sales_order_lines;
DELETE FROM sales.sales_orders;
DELETE FROM sales.quotation_lines;
DELETE FROM sales.quotations;
DELETE FROM sales.products;
DELETE FROM sales.customers;

-- Inventory module (delete in dependency order - stock_movements references products and warehouses)
DELETE FROM inventory.stock_movements;
DELETE FROM inventory.stock_adjustments;
DELETE FROM inventory.stock_transfers;
DELETE FROM inventory.stock;
DELETE FROM inventory.products;
DELETE FROM inventory.product_categories;
DELETE FROM inventory.warehouses;

-- HR module (delete in dependency order)
-- Performance & Goals
DELETE FROM hr.one_on_one_meetings;
DELETE FROM hr.training_certifications;
DELETE FROM hr.development_plans;
DELETE FROM hr.feedback_360;
DELETE FROM hr.review_ratings;
DELETE FROM hr.competencies;
DELETE FROM hr.performance_reviews;
DELETE FROM hr.goal_updates;
DELETE FROM hr.goals;
DELETE FROM hr.performance_cycles;

-- Time & Attendance
DELETE FROM hr.shift_schedules;
DELETE FROM hr.holidays;
DELETE FROM hr.leave_balances;
DELETE FROM hr.leave_requests;
DELETE FROM hr.leave_types;
DELETE FROM hr.timesheet_lines;
DELETE FROM hr.timesheets;
DELETE FROM hr.attendance_records;

-- Payroll & Benefits
DELETE FROM hr.bonuses;
DELETE FROM hr.reimbursements;
DELETE FROM hr.employee_benefits;
DELETE FROM hr.benefits;
DELETE FROM hr.tax_slabs;
DELETE FROM hr.payroll_components;
DELETE FROM hr.payroll_details;
DELETE FROM hr.payroll_runs;
DELETE FROM hr.employee_salary_details;
DELETE FROM hr.salary_components;
DELETE FROM hr.salary_structures;

-- Provident Fund & Incentives
DELETE FROM hr.incentive_audit_trail;
DELETE FROM hr.incentive_payouts;
DELETE FROM hr.incentive_calculations;
DELETE FROM hr.sales_achievements;
DELETE FROM hr.sales_targets;
DELETE FROM hr.employee_incentive_eligibility;
DELETE FROM hr.incentive_plans;
DELETE FROM hr.epf_compliance_records;
DELETE FROM hr.epf_nominations;
DELETE FROM hr.epf_transfers;
DELETE FROM hr.epf_withdrawals;
DELETE FROM hr.epf_interest_calculations;
DELETE FROM hr.epf_contributions;
DELETE FROM hr.epf_accounts;

-- Advanced Incentives
DELETE FROM hr.incentive_notifications;
DELETE FROM hr.incentive_disputes;
DELETE FROM hr.referral_incentives;
DELETE FROM hr.retention_bonuses;
DELETE FROM hr.project_incentives;

-- Sales Target Configuration
DELETE FROM hr.sales_target_configurations;

-- Reporting
DELETE FROM hr.scheduled_reports;

-- Employee Management
DELETE FROM hr.onboarding_checklists;
DELETE FROM hr.employee_documents;
DELETE FROM hr.employees;
DELETE FROM hr.positions;
-- Pharma module
DELETE FROM pharma.sold_product_entries;
DELETE FROM pharma.deposits;
DELETE FROM pharma.expenses;
DELETE FROM pharma.adjustments;
DELETE FROM pharma.product_receipts;
DELETE FROM pharma.product_disbursements;
DELETE FROM pharma.targets;
DELETE FROM pharma.target_coverage;
DELETE FROM pharma.incentive_distributions;
DELETE FROM pharma.incentive_calculations;
DELETE FROM pharma.territory_incentive_rules;
DELETE FROM pharma.employee_territory_assignments;
DELETE FROM pharma.sold_product_entries;
DELETE FROM pharma.territories;
DELETE FROM pharma.areas;
DELETE FROM pharma.regions;
DELETE FROM pharma.divisions;

-- CRM module (delete in dependency order - contacts reference accounts and organizations)
DELETE FROM crm.case_comments;
DELETE FROM crm.cases;
DELETE FROM crm.knowledge_base;
DELETE FROM crm.events;
DELETE FROM crm.tasks;
DELETE FROM crm.campaign_members;
DELETE FROM crm.campaigns;
DELETE FROM crm.opportunity_products;
DELETE FROM crm.opportunity_activities;
DELETE FROM crm.opportunities;
DELETE FROM crm.lead_activities;
DELETE FROM crm.contacts;
DELETE FROM crm.accounts;
DELETE FROM crm.leads;

-- Manufacturing module
DELETE FROM manufacturing.work_order_materials;
DELETE FROM manufacturing.work_order_operations;
DELETE FROM manufacturing.work_orders;
DELETE FROM manufacturing.bom_versions;
DELETE FROM manufacturing.bom_lines;
DELETE FROM manufacturing.bom_headers;
DELETE FROM manufacturing.product_routings;
DELETE FROM manufacturing.quality_inspections;
DELETE FROM manufacturing.quality_inspection_items;
DELETE FROM manufacturing.non_conformances;
DELETE FROM manufacturing.work_centers;
DELETE FROM manufacturing.equipment_maintenance;

-- Accounting module
DELETE FROM accounting.account_balance_summary;
DELETE FROM accounting.account_balances;
DELETE FROM accounting.journal_template_lines;
DELETE FROM accounting.journal_templates;
DELETE FROM accounting.journal_entries;
DELETE FROM accounting.journal_lines;
DELETE FROM accounting.chart_of_accounts;
DELETE FROM accounting.coa_templates;
DELETE FROM accounting.ap_payment_allocations;
DELETE FROM accounting.ap_payments;
DELETE FROM accounting.ap_bill_lines;
DELETE FROM accounting.ap_bills;
DELETE FROM accounting.ar_receipt_allocations;
DELETE FROM accounting.ar_receipts;
DELETE FROM accounting.ar_credit_note_lines;
DELETE FROM accounting.ar_credit_notes;
DELETE FROM accounting.ar_invoice_lines;
DELETE FROM accounting.ar_invoices;
DELETE FROM accounting.reminder_history;
DELETE FROM accounting.reminder_config;
DELETE FROM accounting.periods;
DELETE FROM accounting.fiscal_years;
DELETE FROM accounting.bank_reconciliations;
DELETE FROM accounting.reconciliation_items;
DELETE FROM accounting.bank_accounts;
DELETE FROM accounting.bank_transactions;

-- Organization app data
DELETE FROM admin.organization_app_data;

-- User organizations (links users to organizations)
DELETE FROM admin.user_organizations;

-- RBAC user roles (organization-specific role assignments)
DELETE FROM rbac.user_roles;

-- Organization settings
DELETE FROM admin.organization_settings;

-- Departments and locations
DELETE FROM admin.locations;
DELETE FROM admin.departments;

-- Invitations
DELETE FROM admin.invitations;

-- Finally, delete all organizations (contacts must be deleted first)
DELETE FROM admin.organizations;

-- ============================================================================
-- STEP 2: CREATE ALIEN PHARMA ORGANIZATION
-- ============================================================================
INSERT INTO admin.organizations (
    code, name, legal_name, description, industry, business_type,
    currency, timezone, locale, status, is_active
)
SELECT 'ALIEN_PHARMA', 'Alien Pharma', 'Alien Pharma Limited',
    'Pharmaceutical company specializing in healthcare products',
    'Pharmaceuticals', 'Manufacturing', 'BDT', 'Asia/Dhaka', 'en-US',
    'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM admin.organizations WHERE code = 'ALIEN_PHARMA');

-- ============================================================================
-- STEP 3: CREATE ROLES AND PERMISSIONS
-- ============================================================================
-- Insert default roles if they don't exist
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'System Administrator', 'SYSTEM_ADMIN', 'Full system access', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'SYSTEM_ADMIN');

INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Organization Administrator', 'ORG_ADMIN', 'Organization-level administration', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'ORG_ADMIN');

INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'User', 'USER', 'Standard user access', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'USER');

INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Guest', 'GUEST', 'Limited guest access', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'GUEST');

-- Insert essential permissions if they don't exist
INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Dashboard View', 'DASHBOARD_VIEW', 'dashboard', 'view', 'Access to the main dashboard', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'DASHBOARD_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Organization View', 'ORG_VIEW', 'organizations', 'view', 'View organization information', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ORG_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Organization Management', 'ORG_MANAGE', 'organizations', 'manage', 'Manage organizations', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ORG_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'User View', 'USER_VIEW', 'users', 'view', 'View user information', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'USER_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'User Management', 'USER_MANAGE', 'users', 'manage', 'Manage user accounts', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'USER_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Role View', 'ROLE_VIEW', 'roles', 'view', 'View roles and permissions', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ROLE_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Role Management', 'ROLE_MANAGE', 'roles', 'manage', 'Manage roles and permissions', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ROLE_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'System View', 'SYSTEM_VIEW', 'system', 'view', 'View system information', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'SYSTEM_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'System Configuration', 'SYSTEM_CONFIG', 'system', 'configure', 'Configure system settings', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'SYSTEM_CONFIG');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Inventory View', 'INVENTORY_VIEW', 'inventory', 'view', 'View inventory dashboards and stock data', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'INVENTORY_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description, is_active)
SELECT 'Inventory Manage', 'INVENTORY_MANAGE', 'inventory', 'manage', 'Manage inventory configuration and transactions', true
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'INVENTORY_MANAGE');

-- Assign ALL permissions to SYSTEM_ADMIN role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
CROSS JOIN rbac.permissions p
WHERE r.code = 'SYSTEM_ADMIN'
  AND p.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM rbac.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign essential permissions to ORG_ADMIN role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ORG_VIEW',
    'ORG_MANAGE',
    'USER_VIEW',
    'USER_MANAGE',
    'ROLE_VIEW',
    'ROLE_MANAGE',
    'SYSTEM_VIEW',
    'INVENTORY_VIEW',
    'INVENTORY_MANAGE'
)
WHERE r.code = 'ORG_ADMIN'
  AND p.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM rbac.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign basic view permissions to USER role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ORG_VIEW',
    'USER_VIEW',
    'SYSTEM_VIEW',
    'INVENTORY_VIEW'
)
WHERE r.code = 'USER'
  AND p.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM rbac.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ============================================================================
-- STEP 4: CREATE OR LINK ADMIN USER AND ASSIGN ROLES
-- ============================================================================
-- Create admin user if it doesn't exist (Password: Admin123!)
INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'admin', 'admin@alienpharma.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'System', 'Administrator', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'admin');

-- Assign SYSTEM_ADMIN role to admin user (global role, organization_id = NULL)
INSERT INTO rbac.user_roles (user_id, role_id, organization_id)
SELECT u.id, r.id, NULL
FROM users.users u
CROSS JOIN rbac.roles r
WHERE u.username = 'admin'
  AND r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM rbac.user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.organization_id IS NULL
  );

-- Assign ORG_ADMIN role to admin user for Alien Pharma organization
INSERT INTO rbac.user_roles (user_id, role_id, organization_id)
SELECT u.id, r.id, o.id
FROM users.users u
CROSS JOIN rbac.roles r
CROSS JOIN admin.organizations o
WHERE u.username = 'admin'
  AND r.code = 'ORG_ADMIN'
  AND o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM rbac.user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.organization_id = o.id
  );

-- Link admin user to Alien Pharma as OWNER
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary, status)
SELECT u.id, o.id, 'OWNER', true, 'ACTIVE'
FROM users.users u
CROSS JOIN admin.organizations o
WHERE u.username = 'admin'
  AND o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM admin.user_organizations uo
      WHERE uo.user_id = u.id AND uo.organization_id = o.id
  )
ON CONFLICT (user_id, organization_id) DO UPDATE
SET role = 'OWNER', is_primary = true, status = 'ACTIVE';

-- Link all other existing users to Alien Pharma as MEMBER
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary, status)
SELECT u.id, o.id, 'MEMBER', false, 'ACTIVE'
FROM users.users u
CROSS JOIN admin.organizations o
WHERE u.username != 'admin'
  AND u.is_active = true
  AND o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM admin.user_organizations uo
      WHERE uo.user_id = u.id AND uo.organization_id = o.id
  );

-- Assign USER role to all other active users for Alien Pharma organization
INSERT INTO rbac.user_roles (user_id, role_id, organization_id)
SELECT u.id, r.id, o.id
FROM users.users u
CROSS JOIN rbac.roles r
CROSS JOIN admin.organizations o
WHERE u.username != 'admin'
  AND u.is_active = true
  AND r.code = 'USER'
  AND o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM rbac.user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.organization_id = o.id
  );

-- ============================================================================
-- STEP 4b: CREATE BANK ACCOUNTS (for deposit dropdown)
-- ============================================================================
INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CHK-OP-001', 'Main Operating Account', 'Trust Bank', 'Gulshan Branch', 'CHECKING', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CHK-OP-001');

INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CHK-COLL-001', 'Collection Account', 'Eastern Bank', 'Dhanmondi Branch', 'CHECKING', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CHK-COLL-001');

INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'SAV-001', 'Business Savings', 'Trust Bank', 'Gulshan Branch', 'SAVINGS', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'SAV-001');

-- ============================================================================
-- STEP 5: CREATE DEPARTMENTS
-- ============================================================================
-- Create Sales Department
INSERT INTO admin.departments (organization_id, code, name, description, type, status)
SELECT o.id, 'DEPT-SALES', 'Sales', 'Sales department responsible for product sales and customer relationships', 'DEPARTMENT', 'ACTIVE'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM admin.departments d
      WHERE d.organization_id = o.id AND d.code = 'DEPT-SALES'
  )
ON CONFLICT (organization_id, code) DO UPDATE
SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';

-- Create HR Department
INSERT INTO admin.departments (organization_id, code, name, description, type, status)
SELECT o.id, 'DEPT-HR', 'Human Resources', 'Human Resources department responsible for employee management and development', 'DEPARTMENT', 'ACTIVE'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM admin.departments d
      WHERE d.organization_id = o.id AND d.code = 'DEPT-HR'
  )
ON CONFLICT (organization_id, code) DO UPDATE
SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';

-- ============================================================================
-- STEP 6: CREATE PHARMA PRODUCT CATEGORY
-- ============================================================================
INSERT INTO inventory.product_categories (organization_id, code, name, description, is_active, display_order)
SELECT o.id, 'CAT-PHARMA', 'Pharmaceuticals', 'Pharmaceutical products and medicines', true, 1
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM inventory.product_categories pc
      WHERE pc.organization_id = o.id AND pc.code = 'CAT-PHARMA'
  );

-- ============================================================================
-- STEP 7: CREATE POSITIONS
-- ============================================================================
-- Positions reference admin.departments (organization-service)
INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'MPO',
    'Medical Promotion Officer - Responsible for promoting pharmaceutical products to healthcare professionals',
    d.id,
    'JUNIOR',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'MPO'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'SR',
    'Sales Representative - Responsible for direct sales and customer relationship management',
    d.id,
    'JUNIOR',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'SR'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'Asst. Area Manager',
    'Assistant Area Manager - Assists in managing sales operations within a specific area',
    d.id,
    'MID',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'Asst. Area Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'Area Manager',
    'Area Manager - Manages sales operations and team within a specific geographical area',
    d.id,
    'MID',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'Area Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'Territory Manager',
    'Territory Manager - Manages sales operations across multiple areas within a territory',
    d.id,
    'SENIOR',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'Territory Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'Regional Sales Manager',
    'Regional Sales Manager - Manages sales operations across multiple territories within a region',
    d.id,
    'SENIOR',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'Regional Sales Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'Division Sales Manager',
    'Division Sales Manager - Manages sales operations across multiple regions within a division',
    d.id,
    'EXECUTIVE',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'Division Sales Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT 
    o.id,
    'National Sales Manager',
    'National Sales Manager - Manages all sales operations at the national level',
    d.id,
    'EXECUTIVE',
    'BDT',
    true
FROM admin.organizations o
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = 'National Sales Manager'
  )
ON CONFLICT (organization_id, title) DO UPDATE
SET description = EXCLUDED.description, department_id = EXCLUDED.department_id, is_active = true;

-- ============================================================================
-- STEP 9: CREATE EMPLOYEE FOR ADMIN USER
-- ============================================================================
-- Create employee record for admin user linked to HR department (admin.departments)
INSERT INTO hr.employees (
    organization_id, user_id, employee_number, name, email, phone,
    hire_date, department_id, position_id, employment_type, employment_status, is_active
)
SELECT 
    o.id,
    u.id,
    'EMP-ADMIN-001',
    'System Administrator',
    u.email,
    NULL,
    CURRENT_DATE,
    d.id,
    NULL,
    'FULL_TIME',
    'ACTIVE',
    true
FROM admin.organizations o
CROSS JOIN users.users u
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-HR'
WHERE o.code = 'ALIEN_PHARMA'
  AND u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM hr.employees e
      WHERE e.organization_id = o.id AND e.user_id = u.id
  )
ON CONFLICT (organization_id, employee_number) DO UPDATE
SET name = EXCLUDED.name, email = EXCLUDED.email, is_active = true;

-- Create one sample employee for each Sales position (MPO, SR, etc.)
INSERT INTO hr.employees (
    organization_id, user_id, employee_number, name, email, phone,
    hire_date, department_id, position_id, employment_type, employment_status, is_active
)
SELECT
    o.id,
    NULL, -- no linked user account
    'EMP-' || replace(replace(upper(p.title), ' ', '_'), '.', '') || '-001',
    p.title || ' - Sample Employee',
    NULL,
    NULL,
    CURRENT_DATE,
    p.department_id,
    p.position_id,
    'FULL_TIME',
    'ACTIVE',
    true
FROM admin.organizations o
JOIN hr.positions p ON p.organization_id = o.id
JOIN admin.departments d ON d.id = p.department_id AND d.organization_id = o.id AND d.code = 'DEPT-SALES'
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (
      SELECT 1
      FROM hr.employees e
      WHERE e.organization_id = o.id
        AND e.position_id = p.position_id
  );

-- ============================================================================
-- STEP 10: INSERT PHARMA PRODUCTS
-- ============================================================================
-- Insert pharma products with pack_size, TP (wholesale_price), and MRP (retail_price)
INSERT INTO inventory.products (
    organization_id, 
    sku, 
    name, 
    description, 
    short_description,
    category_id, 
    product_type, 
    item_type, 
    cost_price, 
    selling_price,
    wholesale_price, 
    retail_price, 
    currency,
    uom, 
    pack_size,
    reorder_level, 
    min_stock_level, 
    safety_stock, 
    track_inventory, 
    track_batch,
    is_active, 
    status
) VALUES

-- Active Plus (Soap) - PackSize=100gram, TP=300, MRP=350
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-ACTIVE-PLUS-SOAP', 
 'Active Plus (Soap)', 
 'Active Plus Soap - 100 gram pack', 
 'Active Plus Soap 100g',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 270.00,  -- cost_price (90% of TP)
 350.00,  -- selling_price (MRP)
 300.00,  -- wholesale_price (TP)
 350.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'GRAM',   -- uom
 100.00,   -- pack_size (100 gram)
 50,       -- reorder_level
 20,       -- min_stock_level
 10,       -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Angel (Body wash) - PS=250ml, TP=450, MRP=525
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-ANGEL-BODYWASH', 
 'Angel (Body wash)', 
 'Angel Body wash - 250ml bottle', 
 'Angel Body wash 250ml',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 405.00,  -- cost_price (90% of TP)
 525.00,  -- selling_price (MRP)
 450.00,  -- wholesale_price (TP)
 525.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'ML',     -- uom
 250.00,   -- pack_size (250ml)
 40,       -- reorder_level
 15,       -- min_stock_level
 8,        -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Angel (Handwash) - 200ml, TP=125, MRP=145
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-ANGEL-HANDWASH', 
 'Angel (Handwash)', 
 'Angel Handwash - 200ml bottle', 
 'Angel Handwash 200ml',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 112.50,  -- cost_price (90% of TP)
 145.00,  -- selling_price (MRP)
 125.00,  -- wholesale_price (TP)
 145.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'ML',     -- uom
 200.00,   -- pack_size (200ml)
 60,       -- reorder_level
 25,       -- min_stock_level
 12,       -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Arnipain Plus (Ointment) - 1pcs, TP=300, MRP=350
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-ARNIPAIN-PLUS-OINT', 
 'Arnipain Plus (Ointment)', 
 'Arnipain Plus Ointment - 1 piece', 
 'Arnipain Plus Ointment 1pcs',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 270.00,  -- cost_price (90% of TP)
 350.00,  -- selling_price (MRP)
 300.00,  -- wholesale_price (TP)
 350.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'PCS',    -- uom
 1.00,     -- pack_size (1 piece)
 50,       -- reorder_level
 20,       -- min_stock_level
 10,       -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Bio Plus (Capsule) - 30pcs, TP=750, MRP=870
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-BIO-PLUS-CAP', 
 'Bio Plus (Capsule)', 
 'Bio Plus Capsule - 30 pieces per pack', 
 'Bio Plus Capsule 30pcs',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 675.00,  -- cost_price (90% of TP)
 870.00,  -- selling_price (MRP)
 750.00,  -- wholesale_price (TP)
 870.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'PCS',    -- uom
 30.00,    -- pack_size (30 pieces)
 30,       -- reorder_level
 12,       -- min_stock_level
 6,        -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Bilogin 60mg (Capsule) - 30pcs, TP=525, MRP=600
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-BILOGIN-60MG-CAP', 
 'Bilogin 60mg (Capsule)', 
 'Bilogin 60mg Capsule - 30 pieces per pack', 
 'Bilogin 60mg Capsule 30pcs',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 472.50,  -- cost_price (90% of TP)
 600.00,  -- selling_price (MRP)
 525.00,  -- wholesale_price (TP)
 600.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'PCS',    -- uom
 30.00,    -- pack_size (30 pieces)
 35,       -- reorder_level 
 15,       -- min_stock_level
 8,        -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE'),

-- Bone Fuel Plus (tablet) - 30pcs, TP=550, MRP=640
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 
 'PHARMA-BONE-FUEL-PLUS-TAB', 
 'Bone Fuel Plus (tablet)', 
 'Bone Fuel Plus Tablet - 30 pieces per pack', 
 'Bone Fuel Plus Tablet 30pcs',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')), 
 'GOODS', 
 'STOCKABLE', 
 495.00,  -- cost_price (90% of TP)
 640.00,  -- selling_price (MRP)
 550.00,  -- wholesale_price (TP)
 640.00,  -- retail_price (MRP)
 'BDT',    -- currency
 'PCS',    -- uom
 30.00,    -- pack_size (30 pieces)
 35,       -- reorder_level
 15,       -- min_stock_level
 8,        -- safety_stock
 true,     -- track_inventory
 true,     -- track_batch
 true,     -- is_active
 'ACTIVE')
ON CONFLICT (organization_id, sku) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    short_description = EXCLUDED.short_description,
    category_id = EXCLUDED.category_id,
    cost_price = EXCLUDED.cost_price,
    selling_price = EXCLUDED.selling_price,
    wholesale_price = EXCLUDED.wholesale_price,
    retail_price = EXCLUDED.retail_price,
    uom = EXCLUDED.uom,
    pack_size = EXCLUDED.pack_size,
    reorder_level = EXCLUDED.reorder_level,
    min_stock_level = EXCLUDED.min_stock_level,
    safety_stock = EXCLUDED.safety_stock,
    track_inventory = EXCLUDED.track_inventory,
    track_batch = EXCLUDED.track_batch,
    is_active = true,
    status = 'ACTIVE';

-- ============================================================================
-- STEP 11: INSERT UOMs (Units of Measure) and GENDERs
-- ============================================================================
-- UOM values
INSERT INTO admin.organization_app_data (
    id, organization_id, type, code, name, description, extra_attributes,
    is_active, display_order, created_at, updated_at, created_by, updated_by
)
SELECT
    gen_random_uuid(),
    o.id,
    'UOM' AS type,
    u.code,
    u.name,
    u.description,
    u.extra_attributes::jsonb,
    TRUE AS is_active,
    u.display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
FROM admin.organizations o
CROSS JOIN (
    VALUES
        ('GRAM', 'Gram', 'Gram (g) unit of mass', '{"category":"MASS"}', 1),
        ('ML',   'Milliliter', 'Milliliter (ml) unit of volume', '{"category":"VOLUME"}', 2),
        ('PCS',  'Pieces', 'Pieces (pcs) unit of count', '{"category":"COUNT"}', 3)
) AS u(code, name, description, extra_attributes, display_order)
WHERE o.code = 'ALIEN_PHARMA'
ON CONFLICT (organization_id, type, code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    extra_attributes = EXCLUDED.extra_attributes,
    is_active = EXCLUDED.is_active,
    display_order = EXCLUDED.display_order,
    updated_at = CURRENT_TIMESTAMP;

-- Gender values
INSERT INTO admin.organization_app_data (
    id, organization_id, type, code, name, description, extra_attributes,
    is_active, display_order, created_at, updated_at, created_by, updated_by
)
SELECT
    gen_random_uuid(),
    o.id,
    'GENDER' AS type,
    g.code,
    g.name,
    g.description,
    NULL::jsonb AS extra_attributes,
    TRUE AS is_active,
    g.display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
FROM admin.organizations o
CROSS JOIN (
    VALUES
        ('MALE', 'Male', 'Male gender', 1),
        ('FEMALE', 'Female', 'Female gender', 2),
        ('OTHER', 'Other', 'Other / non-binary gender', 3),
        ('PREFER_NOT_TO_SAY', 'Prefer not to say', 'Prefer not to disclose gender', 4)
) AS g(code, name, description, display_order)
WHERE o.code = 'ALIEN_PHARMA'
ON CONFLICT (organization_id, type, code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    extra_attributes = EXCLUDED.extra_attributes,
    is_active = EXCLUDED.is_active,
    display_order = EXCLUDED.display_order,
    updated_at = CURRENT_TIMESTAMP;
	
	
DELETE FROM inventory.products;





-- ============================================================================
-- STEP 12: INSERT ALIEN PHARMA PRODUCTS
-- ============================================================================
-- Insert Alien pharma products with pack_size, TP (wholesale_price), and MRP (retail_price)


INSERT INTO inventory.products
(
    organization_id,
    sku,
    name,
    description,
    short_description,
    category_id,
    product_type,
    currency,
    wholesale_price,
    retail_price,
    uom,
    pack_size,
    is_active,
    status
)
VALUES
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ACTIVE-PLUS-SOAP',
 'Active Plus (Soap)',
 'Active Plus (Soap)',
 'Active Plus (Soap)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'GRAM',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ANGEL-BODY-WASH',
 'Angel (Body Wash)',
 'Angel (Body Wash)',
 'Angel (Body Wash)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',450,525,'ML',250,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ANGEL-HAND-WASH',
 'Angel (Hand Wash)',
 'Angel (Hand Wash)',
 'Angel (Hand Wash)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',125,145,'ML',200,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ARNIPAIN-PLUS-OINTMENT',
 'Arnipain Plus (Ointment)',
 'Arnipain Plus (Ointment)',
 'Arnipain Plus (Ointment)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BIO-PLUS-CAPSULE',
 'Bio Plus (Capsule)',
 'Bio Plus (Capsule)',
 'Bio Plus (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',750,870,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BILOGIN-60MG-CAPSULE',
 'Bilogin 60 mg (Capsule)',
 'Bilogin 60 mg (Capsule)',
 'Bilogin 60 mg (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',525,600,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BONE-FUEL-PLUS-TABLET',
 'Bone Fuel Plus (Tablet)',
 'Bone Fuel Plus (Tablet)',
 'Bone Fuel Plus (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',550,640,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BRIGHT-KISS-TOOTHPASTE',
 'Bright Kiss (Toothpaste)',
 'Bright Kiss (Toothpaste)',
 'Bright Kiss (Toothpaste)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',125,150,'GRAM',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BRONGEL-ACNE-GEL',
 'Brongel (Acne gel)',
 'Brongel (Acne gel)',
 'Brongel (Acne gel)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',600,695,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'BRON-WASH-FACE-WASH',
 'Bron Wash (Face wash)',
 'Bron Wash (Face wash)',
 'Bron Wash (Face wash)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',400,465,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'CM-CARE-CAPSULE',
 'CM Care (Capsule)',
 'CM Care (Capsule)',
 'CM Care (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1050,1210,'PCS',20,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'CRACK-CURE-FOOT-CREAM',
 'Crack Cure (Foot Cream)',
 'Crack Cure (Foot Cream)',
 'Crack Cure (Foot Cream)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'DILASTIN-STRETCH-MARK-SCAR-GEL',
 'Dilastin Strech Mark (Scar) gel',
 'Dilastin Strech Mark (Scar) gel',
 'Dilastin Strech Mark (Scar) gel',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',500,580,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ENQ-TABLET',
 'ENQ (Tablet)',
 'ENQ (Tablet)',
 'ENQ (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1040,1200,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ENZY-FORTE-TABLET',
 'Enzy Forte (Tablet)',
 'Enzy Forte (Tablet)',
 'Enzy Forte (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',750,870,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'FERTILITY-TABLET',
 'Fertility (Tablet)',
 'Fertility (Tablet)',
 'Fertility (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1260,1450,'PCS',42,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'FRESHDENT-TOOTHPASTE',
 'Freshdent (Toothpaste)',
 'Freshdent (Toothpaste)',
 'Freshdent (Toothpaste)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',110,130,'GRAM',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'GARCICAM-CAPSULE',
 'Garcicam (Capsule)',
 'Garcicam (Capsule)',
 'Garcicam (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',500,580,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'GASTOFORTE-TABLET',
 'Gastoforte (Tablet)',
 'Gastoforte (Tablet)',
 'Gastoforte (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',380,450,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'GELTAR-SHAMPOO',
 'Geltar (Shampoo)',
 'Geltar (Shampoo)',
 'Geltar (Shampoo)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',425,500,'ML',200,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'GELTAR-OINTMENT',
 'Geltar (Ointment)',
 'Geltar (Ointment)',
 'Geltar (Ointment)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'GLUTALIGHT-CAPSULE',
 'Glutalight (Capsule)',
 'Glutalight (Capsule)',
 'Glutalight (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1040,1200,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'HER-BOOSTER-TABLET',
 'Her Booster (Tablet)',
 'Her Booster (Tablet)',
 'Her Booster (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',775,900,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'IMMUNA-CAPSULE',
 'Immuna (Capsule)',
 'Immuna (Capsule)',
 'Immuna (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',400,465,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'IMMUNA-PLUS-TABLET',
 'Immuna Plus (Tablet)',
 'Immuna Plus (Tablet)',
 'Immuna Plus (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',550,640,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'JOINT-RELIEF-PLUS-TABLET',
 'Joint Relief Plus (Tablet)',
 'Joint Relief Plus (Tablet)',
 'Joint Relief Plus (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',950,1100,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'KETO-GOLD-SOAP',
 'Keto Gold (Soap)',
 'Keto Gold (Soap)',
 'Keto Gold (Soap)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',475,550,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'KIDS-DK-DROPS',
 'Kids DK (Drops)',
 'Kids DK (Drops)',
 'Kids DK (Drops)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'ML',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'LACTOFORTE-TABLET',
 'Lactoforte (Tablet)',
 'Lactoforte (Tablet)',
 'Lactoforte (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',650,750,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'LACTOCALM-DROP',
 'Lactocalm (Drop)',
 'Lactocalm (Drop)',
 'Lactocalm (Drop)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'ML',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'LAXOCARE-TABLET',
 'Laxocare (Tablet)',
 'Laxocare (Tablet)',
 'Laxocare (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',550,640,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'LICE-CURE-SHAMPOO',
 'Lice Cure (Shampoo)',
 'Lice Cure (Shampoo)',
 'Lice Cure (Shampoo)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',130,150,'ML',60,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'MYOSITOL-PLUS-TABLET',
 'Myositol Plus (Tablet)',
 'Myositol Plus (Tablet)',
 'Myositol Plus (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1260,1450,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'NEEMORAL-SOAP',
 'Neemoral (Soap)',
 'Neemoral (Soap)',
 'Neemoral (Soap)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',300,350,'GRAM',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'OVULATION-INDUCER-CAPSULE',
 'Ovulation Inducer (Capsule)',
 'Ovulation Inducer (Capsule)',
 'Ovulation Inducer (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1180,1370,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'ORALCARE-TOOTHPASTE',
 'Oralcare (Toothpaste)',
 'Oralcare (Toothpaste)',
 'Oralcare (Toothpaste)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',100,120,'GRAM',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'PEARLCAL-TABLET',
 'Pearlcal (Tablet)',
 'Pearlcal (Tablet)',
 'Pearlcal (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',400,465,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'PHENOCID-SOAP',
 'Phenocid (Soap)',
 'Phenocid (Soap)',
 'Phenocid (Soap)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',200,235,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'PREGNIFORTE-TABLET',
 'Pregniforte (Tablet)',
 'Pregniforte (Tablet)',
 'Pregniforte (Tablet)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1380,1600,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'PROTOLACTIN-CAPSULE',
 'Protolactin (Capsule)',
 'Protolactin (Capsule)',
 'Protolactin (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',700,810,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'RETAINAGE-A-ANTI-AGING-SERUM',
 'Retainage-A (Anti-Aging Serum)',
 'Retainage-A (Anti-Aging Serum)',
 'Retainage-A (Anti-Aging Serum)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',500,580,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SAFE-WASH-MOUTH-WASH',
 'Safe Wash (Mouth wash)',
 'Safe Wash (Mouth wash)',
 'Safe Wash (Mouth wash)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',160,190,'ML',250,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SELDAN-PLUS-SHAMPOO',
 'Seldan Plus (Shampoo)',
 'Seldan Plus (Shampoo)',
 'Seldan Plus (Shampoo)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',525,610,'ML',250,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SELDAN-PLUS-LOTION',
 'Seldan Plus (Lotion)',
 'Seldan Plus (Lotion)',
 'Seldan Plus (Lotion)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',400,465,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SILYFORTE-87-50MG-CAPSULE',
 'Silyforte 87.50 mg (Capsule)',
 'Silyforte 87.50 mg (Capsule)',
 'Silyforte 87.50 mg (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',225,260,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SKILIGHTER-SOAP',
 'Skilighter (Soap)',
 'Skilighter (Soap)',
 'Skilighter (Soap)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',500,570,'PCS',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SPERM-CARE-CAPSULE',
 'Sperm Care (Capsule)',
 'Sperm Care (Capsule)',
 'Sperm Care (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',1330,1540,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'STIMUGIN-500MG-CAPSULE',
 'Stimugin 500 mg (Capsule)',
 'Stimugin 500 mg (Capsule)',
 'Stimugin 500 mg (Capsule)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',625,750,'PCS',30,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'SUPER-KIDS-DROPS',
 'Super Kids (Drops)',
 'Super Kids (Drops)',
 'Super Kids (Drops)',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',500,580,'ML',1,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'VASIKA-SYRUP-100ML',
 'Vasika (Syrup) 100 ml',
 'Vasika (Syrup) 100 ml',
 'Vasika (Syrup) 100 ml',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',60,75,'ML',100,true,'ACTIVE'
),
(
 (SELECT id FROM admin.organizations WHERE code='ALIEN_PHARMA'),
 'VASIKA-SYRUP-200ML',
 'Vasika (Syrup) 200 ml',
 'Vasika (Syrup) 200 ml',
 'Vasika (Syrup) 200 ml',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PHARMA' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA')),
 'GOODS','BDT',100,120,'ML',200,true,'ACTIVE'
);



-- ============================================================================
-- STEP 12: INSERT ALIEN PHARMA Terrirory Management (DIVISION)
-- ============================================================================
insert into pharma.divisions
(organization_id, name, code, description, is_active,   status)
VALUES (
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Dhaka','Dhaka-001','Dhaka Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Mymensingh','Mymensingh-001','Mymensingh Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Rangpur ','Rangpur-001','Rangpur  Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Rajshahi','Rajshahi-001','Rajshahi Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Sylhet','Sylhet-001','Sylhet Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Chattogram','Chattogram-001','Chattogram Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Khulna','Khulna-001','Khulna Division',true,'ACTIVE'),
((SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'), 'Barishal','Barishal-001','Barishal Division',true,'ACTIVE');


-- ============================================================================
-- STEP 13: INSERT ALIEN PHARMA Terrirory Management (REGION)
-- ============================================================================

INSERT INTO pharma.regions
(
	organization_id,
	division_id,
	name,
	code,
	description,
	is_active,
	status
)
VALUES
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
	'Dhaka',
	'Dhaka-002',
	'Dhaka Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
	'Mymensingh',
	'Mymensingh-002',
	'Mymensingh Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Rangpur-001'),
	'Rangpur',
	'Rangpur-002',
	'Rangpur Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Rajshahi-001'),
	'Rajshahi',
	'Rajshahi-002',
	'Rajshahi Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Sylhet-001'),
	'Sylhet',
	'Sylhet-002',
	'Sylhet Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	'Chattogram',
	'Chattogram-002',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	'Khulna',
	'Khulna-002',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Barishal-001'),
	'Barishal',
	'Barishal-002',
	'Barishal Division',
	true,
	'ACTIVE'
);



-- ============================================================================
-- STEP 14: INSERT ALIEN PHARMA Areas (middle tier: Division -> Region -> Area -> Territory)
-- ============================================================================

INSERT INTO pharma.areas
(
	organization_id,
	division_id,
	region_id,
	name,
	code,
	description,
	is_active,
	status
)
VALUES
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
	'Dhaka',
	'Dhaka-004',
	'Dhaka Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
	'Dhaka-2',
	'Dhaka-005',
	'Dhaka Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
	'Dhaka-1',
	'Dhaka-006',
	'Dhaka Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
	'Mymensingh',
	'Mymensingh-003',
	'Mymensingh Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
	'Gazipur',
	'Mymensingh-004',
	'Mymensingh Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
	'Kishoregonj',
	'Mymensingh-005',
	'Mymensingh Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Rangpur-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Rangpur-002'),
	'Rangpur',
	'Rangpur-003',
	'Rangpur Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Rajshahi-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Rajshahi-002'),
	'Rajshahi',
	'Rajshahi-003',
	'Rajshahi Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Rajshahi-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Rajshahi-002'),
	'TANGAIL',
	'Rajshahi-004',
	'Rajshahi Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Sylhet-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Sylhet-002'),
	'Sylhet',
	'Sylhet-003',
	'Sylhet Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Chattogram-002'),
	'Chattogram',
	'Chattogram-003',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Chattogram-002'),
	'Comilla',
	'Chattogram-004',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Chattogram-002'),
	'Chandpur',
	'Chattogram-005',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Chattogram-002'),
	'Noakhali',
	'Chattogram-006',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Chattogram-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Chattogram-002'),
	'Cox Bazar',
	'Chattogram-007',
	'Chattogram Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Khulna',
	'Khulna-003',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Jessore',
	'Khulna-004',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Kushtia',
	'Khulna-005',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Kushtia',
	'Khulna-006',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Magura',
	'Khulna-007',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Khulna-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Khulna-002'),
	'Jhenaidhah',
	'Khulna-008',
	'Khulna Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Barishal-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Barishal-002'),
	'Barishal',
	'Barishal-003',
	'Barishal Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Barishal-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Barishal-002'),
	'Faridpur',
	'Barishal-004',
	'Barishal Division',
	true,
	'ACTIVE'
),
(
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Barishal-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Barishal-002'),
	'Madaripur',
	'Barishal-005',
	'Barishal Division',
	true,
	'ACTIVE'
);


-- ============================================================================
-- STEP 15: INSERT ALIEN PHARMA Territories (leaf tier, under areas)
-- ============================================================================

-- ==================== DHAKA ============================
INSERT INTO pharma.territories
(
	organization_id,
	division_id,
	region_id,
	area_id,
	name,
	code,
	description,
	is_active,
	status
)
SELECT
	(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
	(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
	(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
	(SELECT id FROM pharma.areas WHERE name = 'Dhaka'),
	territory_name,
	territory_name || '-001',
	'Dhaka Division',
	true,
	'ACTIVE'
FROM unnest(ARRAY[
	'Dhanmondi',
	'Dhaka Medical',
	'College gate',
	'Azimpur',
	'Saver',
	'Hemyetpur',
	'Baipail',
	'Ashulia',
	'Dhamrai',
	'Manikgonj',
	'Ghior',
	'Shingair',
	'Shaturia'
]) AS t(territory_name);


INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
(SELECT id FROM pharma.areas WHERE name = 'Dhaka-2'),
territory_name,
territory_name || '-001',
'Dhaka Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Narayongonj',
'Bondor',
'Manshigonj',
'Tongibari',
'Sreenagor',
'Nowabgonj',
'Rohitpur',
'Shantinagor',
'Sadorghat-Tu',
'Jattrabari-San'
]) AS t(territory_name);


INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Dhaka-001'),
(SELECT id FROM pharma.regions WHERE code = 'Dhaka-002'),
(SELECT id FROM pharma.areas WHERE name = 'Dhaka-1'),
territory_name,
territory_name || '-001',
'Dhaka Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Gulshan',
'Mirpur-10',
'Mirpur-1',
'Uttara',
'Tongi'
]) AS t(territory_name);


-- ==================== Mymensingh ============================

INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
(SELECT id FROM pharma.areas WHERE name = 'Mymensingh'),
territory_name,
territory_name || '-001',
'Mymensingh Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Mymensingh',
'Bhaluca',
'Gaffargoan',
'Phulpur',
'Haluaghat',
'Sherpur',
'Nolitabari',
'Bockshigonj',
'Roumari',
'Jamalpur',
'Sorisabari',
'Islampur',
'Madhargonj'
]) AS t(territory_name);


INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
(SELECT id FROM pharma.areas WHERE name = 'Gazipur'),
territory_name,
territory_name || '-001',
'Mymensingh Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Gazipur',
'Kapasia',
'Samir',
'Konabari'
]) AS t(territory_name);


INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Mymensingh-001'),
(SELECT id FROM pharma.regions WHERE code = 'Mymensingh-002'),
(SELECT id FROM pharma.areas WHERE name = 'Kishoregonj'),
territory_name,
territory_name || '-001',
'Mymensingh Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Kishoregonj',
'Hoshenpur',
'Pakundia',
'Tarial',
'Nandail',
'Bhagolpur',
'Katiadi',
'Netrokona',
'Durgapur',
'Mohongonj',
'Modan',
'Kendua'
]) AS t(territory_name);


-- ==================== Rangpur ============================

INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Rangpur-001'),
(SELECT id FROM pharma.regions WHERE code = 'Rangpur-002'),
(SELECT id FROM pharma.areas WHERE name = 'Rangpur'),
territory_name,
territory_name || '-001',
'Rangpur Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Rangpur',
'Mithapukur',
'Bodorgonj',
'Pirganj',
'Pirgacha',
'Gaibandha',
'Gobindoganj',
'Palashbari',
'Sundorgonj',
'Lalmonir hat',
'Patgram',
'Hatibandha',
'Kaliganj',
'Kurigram',
'Ulipur',
'Burangomari',
'Nagerswary',
'Nilfamary',
'Demla',
'Domar',
'Saidpur',
'Dinajpur',
'Fulbaria',
'Birgonj',
'Birampur',
'Parbotipur',
'Chirirbandar',
'Thakurgaon',
'Ranisangkor',
'Bodha',
'Panchagor'
]) AS t(territory_name);


-- ==================== Sylhet ============================

INSERT INTO pharma.territories
(organization_id, division_id, region_id, area_id, name, code, description, is_active, status)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
(SELECT id FROM pharma.divisions WHERE code = 'Sylhet-001'),
(SELECT id FROM pharma.regions WHERE code = 'Sylhet-002'),
(SELECT id FROM pharma.areas WHERE name = 'Sylhet'),
territory_name,
territory_name || '-001',
'Sylhet Division',true,'ACTIVE'
FROM unnest(ARRAY[
'Norsindi',
'Polash',
'Madhabdi',
'Monohordi',
'Raypura',
'Belabo',
'Kaligonj',
'Shibpur',
'Bhulta',
'Murapara',
'Sonargaon',
'Araihazer',
'B.Baria',
'Asugonj',
'Akhura',
'Kasba',
'Bhoirob',
'Nabinagor',
'Nasirnogar',
'Bacharampur',
'Hobigonj',
'Baniachong',
'Chunarughat',
'Madhoppur',
'Nobigonj',
'M.Bazar',
'Sreemongal',
'Kulaura',
'Sylhet',
'Bishawnath', 
'Gobindagonj',
'Borolekha',
'Golapgonj',
'Jokigonj',
'Bianibazer',
'Sunamgong',
'Derai'
]) AS t(territory_name);


-- ============================================================================
-- STEP 16: INSERT ALIEN PHARMA Warehouses (one per territory)
-- ============================================================================

INSERT INTO inventory.warehouses
(
organization_id,
name,
code,
warehouse_type,
is_active,
status,
capacity_unit
)
SELECT
(SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA'),
t.name || '-Warehouse' AS name,
t.code,
'DISTRIBUTION' AS warehouse_type,
true AS is_active,
'OPERATIONAL' AS status,
'M3' AS capacity_unit
FROM pharma.territories t;

UPDATE pharma.territories t
SET warehouse_id = w.id
FROM inventory.warehouses w
WHERE t.code = w.code
AND t.warehouse_id IS NULL;