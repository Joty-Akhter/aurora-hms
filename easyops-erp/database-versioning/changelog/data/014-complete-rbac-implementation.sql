--liquibase formatted sql

--changeset easyops:096-assign-guest-permissions context:data
--comment: Assign minimal read-only permissions to Guest role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'DASHBOARD_VIEW'
WHERE r.code = 'GUEST'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:097-create-business-roles context:data
--comment: Create all documented business/functional roles
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active) VALUES
('Manager', 'MANAGER', 'Management-level access with oversight and approval capabilities', false, true),
('Salesperson', 'SALESPERSON', 'Sales team member with access to sales and CRM modules', false, true),
('Accountant', 'ACCOUNTANT', 'Financial professional with access to accounting modules', false, true),
('Finance Manager', 'FINANCE_MANAGER', 'Senior financial role with approval authority and oversight', false, true),
('Auditor', 'AUDITOR', 'Read-only access role for internal or external auditors', false, true),
('Branch Accountant', 'BRANCH_ACCOUNTANT', 'Accountant with access scoped to a specific branch or location', false, true),
('Warehouse Staff', 'WAREHOUSE_STAFF', 'Inventory and warehouse operations personnel', false, true),
('Sales Support', 'SALES_SUPPORT', 'Customer service and support personnel with CRM access', false, true)
ON CONFLICT (code) DO NOTHING;

--changeset easyops:098-assign-permissions-to-manager context:data
--comment: Assign permissions to Manager role - approval workflows, reporting, data access
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ACCOUNTING_VIEW', 'ACCOUNTING_MANAGE',
    'SALES_VIEW', 'SALES_MANAGE',
    'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    'PURCHASE_VIEW', 'PURCHASE_MANAGE',
    'HR_VIEW', 'HR_MANAGE',
    'CRM_VIEW', 'CRM_MANAGE',
    'MANUFACTURING_VIEW', 'MANUFACTURING_MANAGE',
    'AUDIT_VIEW'
)
WHERE r.code = 'MANAGER'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:099-assign-permissions-to-salesperson context:data
--comment: Assign permissions to Salesperson role - customer management, sales transactions, CRM access
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'SALES_VIEW', 'SALES_MANAGE',
    'CRM_VIEW', 'CRM_MANAGE',
    'INVENTORY_VIEW'
)
WHERE r.code = 'SALESPERSON'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:100-assign-permissions-to-accountant context:data
--comment: Assign permissions to Accountant role - journal entries, general ledger, financial reports
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ACCOUNTING_VIEW', 'ACCOUNTING_MANAGE',
    'AUDIT_VIEW'
)
WHERE r.code = 'ACCOUNTANT'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:101-assign-permissions-to-finance-manager context:data
--comment: Assign permissions to Finance Manager role - all accountant permissions plus approval authority
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ACCOUNTING_VIEW', 'ACCOUNTING_MANAGE',
    'AUDIT_VIEW',
    'SYSTEM_VIEW'
)
WHERE r.code = 'FINANCE_MANAGER'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:102-assign-permissions-to-auditor context:data
--comment: Assign read-only permissions to Auditor role - view all data, audit logs, reports
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ACCOUNTING_VIEW',
    'SALES_VIEW',
    'INVENTORY_VIEW',
    'PURCHASE_VIEW',
    'HR_VIEW',
    'CRM_VIEW',
    'MANUFACTURING_VIEW',
    'AUDIT_VIEW',
    'SYSTEM_VIEW'
)
WHERE r.code = 'AUDITOR'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:103-assign-permissions-to-branch-accountant context:data
--comment: Assign scoped permissions to Branch Accountant role - accounting permissions limited to branch
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ACCOUNTING_VIEW', 'ACCOUNTING_MANAGE',
    'INVENTORY_VIEW',
    'AUDIT_VIEW'
)
WHERE r.code = 'BRANCH_ACCOUNTANT'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:104-assign-permissions-to-warehouse-staff context:data
--comment: Assign permissions to Warehouse Staff role - inventory transactions, stock management
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    'PURCHASE_VIEW'
)
WHERE r.code = 'WAREHOUSE_STAFF'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:105-assign-permissions-to-sales-support context:data
--comment: Assign permissions to Sales Support role - contact management, CRM access
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'CRM_VIEW', 'CRM_MANAGE',
    'SALES_VIEW'
)
WHERE r.code = 'SALES_SUPPORT'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
