--liquibase formatted sql

--changeset easyops:230-insert-communication-permissions context:data
--comment: Seed communication module RBAC permissions (menu, templates, operations)
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication View', 'COMMUNICATION_VIEW', 'communication', 'view', 'View communication foundation and workbench screens'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication Manage', 'COMMUNICATION_MANAGE', 'communication', 'manage', 'Manage communication configuration and operations UI'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication Template View', 'COMMUNICATION_TEMPLATE_VIEW', 'communication_template', 'view', 'View communication templates and provider health'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_TEMPLATE_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication Template Manage', 'COMMUNICATION_TEMPLATE_MANAGE', 'communication_template', 'manage', 'Create and manage communication templates'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_TEMPLATE_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication Operations View', 'COMMUNICATION_OPERATIONS_VIEW', 'communication_operations', 'view', 'View delivery drill-down and provider operations'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_OPERATIONS_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Communication Operations Manage', 'COMMUNICATION_OPERATIONS_MANAGE', 'communication_operations', 'manage', 'Resend failed deliveries and run communication ops actions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'COMMUNICATION_OPERATIONS_MANAGE');

--changeset easyops:231-assign-communication-permissions-to-system-admin context:data
--comment: Grant communication permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'COMMUNICATION_VIEW', 'COMMUNICATION_MANAGE',
    'COMMUNICATION_TEMPLATE_VIEW', 'COMMUNICATION_TEMPLATE_MANAGE',
    'COMMUNICATION_OPERATIONS_VIEW', 'COMMUNICATION_OPERATIONS_MANAGE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:232-assign-communication-permissions-to-org-admin context:data
--comment: Grant communication permissions to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'COMMUNICATION_VIEW', 'COMMUNICATION_MANAGE',
    'COMMUNICATION_TEMPLATE_VIEW', 'COMMUNICATION_TEMPLATE_MANAGE',
    'COMMUNICATION_OPERATIONS_VIEW', 'COMMUNICATION_OPERATIONS_MANAGE'
)
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
