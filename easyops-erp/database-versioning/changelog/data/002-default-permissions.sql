--liquibase formatted sql

--changeset easyops:087-insert-default-permissions context:data
--comment: Insert default permissions
INSERT INTO rbac.permissions (name, code, resource, action, description) VALUES
('User Management', 'USER_MANAGE', 'users', 'manage', 'Manage user accounts'),
('User View', 'USER_VIEW', 'users', 'view', 'View user information'),
('Role Management', 'ROLE_MANAGE', 'roles', 'manage', 'Manage roles and permissions'),
('Role View', 'ROLE_VIEW', 'roles', 'view', 'View roles and permissions'),
('System Configuration', 'SYSTEM_CONFIG', 'system', 'configure', 'Configure system settings'),
('System View', 'SYSTEM_VIEW', 'system', 'view', 'View system information'),
('Audit Logs', 'AUDIT_VIEW', 'audit', 'view', 'View audit logs'),
('Organization Management', 'ORG_MANAGE', 'organizations', 'manage', 'Manage organizations');

--changeset easyops:088-assign-permissions-to-system-admin context:data
--comment: Assign permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'SYSTEM_ADMIN';

--changeset easyops:089-assign-permissions-to-org-admin context:data
--comment: Assign basic permissions to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'ORG_ADMIN'
AND p.code IN ('USER_MANAGE', 'USER_VIEW', 'ROLE_VIEW', 'ROLE_MANAGE', 'SYSTEM_VIEW', 'AUDIT_VIEW', 'ORG_MANAGE');

--changeset easyops:090-assign-permissions-to-user context:data
--comment: Assign view permissions to User role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'USER' 
AND p.code IN ('USER_VIEW', 'SYSTEM_VIEW');
