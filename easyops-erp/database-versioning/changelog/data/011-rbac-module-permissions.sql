--liquibase formatted sql

--changeset easyops:091-add-module-permissions context:data
--comment: Seed module-level permissions to support granular RBAC controls
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Dashboard View', 'DASHBOARD_VIEW', 'dashboard', 'view', 'Access to the main dashboard'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'DASHBOARD_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Organization View', 'ORG_VIEW', 'organizations', 'view', 'View organization information'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ORG_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Permission View', 'PERMISSION_VIEW', 'permissions', 'view', 'View permissions catalog'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PERMISSION_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Permission Manage', 'PERMISSION_MANAGE', 'permissions', 'manage', 'Manage permissions catalog'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PERMISSION_MANAGE');

-- Accounting
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Accounting View', 'ACCOUNTING_VIEW', 'accounting', 'view', 'View accounting dashboards and reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ACCOUNTING_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Accounting Manage', 'ACCOUNTING_MANAGE', 'accounting', 'manage', 'Manage accounting configuration and entries'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'ACCOUNTING_MANAGE');

-- Sales
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Sales View', 'SALES_VIEW', 'sales', 'view', 'View sales dashboards and data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'SALES_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Sales Manage', 'SALES_MANAGE', 'sales', 'manage', 'Manage sales configuration and transactions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'SALES_MANAGE');

-- Inventory
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Inventory View', 'INVENTORY_VIEW', 'inventory', 'view', 'View inventory dashboards and stock data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'INVENTORY_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Inventory Manage', 'INVENTORY_MANAGE', 'inventory', 'manage', 'Manage inventory configuration and transactions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'INVENTORY_MANAGE');

-- Purchase
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Purchase View', 'PURCHASE_VIEW', 'purchase', 'view', 'View purchase dashboards and reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PURCHASE_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Purchase Manage', 'PURCHASE_MANAGE', 'purchase', 'manage', 'Manage purchase orders and procurement workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PURCHASE_MANAGE');

-- HR
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR View', 'HR_VIEW', 'hr', 'view', 'View HR dashboards and employee data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Manage', 'HR_MANAGE', 'hr', 'manage', 'Manage HR configuration and employee records'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_MANAGE');

-- CRM
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'CRM View', 'CRM_VIEW', 'crm', 'view', 'View CRM dashboards and data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'CRM_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'CRM Manage', 'CRM_MANAGE', 'crm', 'manage', 'Manage CRM configuration and records'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'CRM_MANAGE');

-- Manufacturing
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Manufacturing View', 'MANUFACTURING_VIEW', 'manufacturing', 'view', 'View manufacturing dashboards and data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'MANUFACTURING_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Manufacturing Manage', 'MANUFACTURING_MANAGE', 'manufacturing', 'manage', 'Manage manufacturing configuration and workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'MANUFACTURING_MANAGE');

--changeset easyops:092-assign-module-permissions-to-system-admin context:data
--comment: Grant module-level permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ORG_VIEW',
    'PERMISSION_VIEW',
    'PERMISSION_MANAGE',
    'ACCOUNTING_VIEW', 'ACCOUNTING_MANAGE',
    'SALES_VIEW', 'SALES_MANAGE',
    'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    'PURCHASE_VIEW', 'PURCHASE_MANAGE',
    'HR_VIEW', 'HR_MANAGE',
    'CRM_VIEW', 'CRM_MANAGE',
    'MANUFACTURING_VIEW', 'MANUFACTURING_MANAGE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:093-assign-module-view-to-org-admin context:data
--comment: Grant view permissions to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'ORG_VIEW',
    'PERMISSION_VIEW',
    'ACCOUNTING_VIEW',
    'SALES_VIEW',
    'INVENTORY_VIEW',
    'PURCHASE_VIEW',
    'HR_VIEW',
    'CRM_VIEW',
    'MANUFACTURING_VIEW'
)
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:095-assign-module-manage-to-org-admin context:data
--comment: Grant manage permissions to Organization Administrator role for full module access within organization scope
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'ACCOUNTING_MANAGE',
    'SALES_MANAGE',
    'INVENTORY_MANAGE',
    'PURCHASE_MANAGE',
    'HR_MANAGE',
    'CRM_MANAGE',
    'MANUFACTURING_MANAGE'
)
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:094-assign-dashboard-to-user context:data
--comment: Grant dashboard permission to standard User role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'DASHBOARD_VIEW'
WHERE r.code = 'USER'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

