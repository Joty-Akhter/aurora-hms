--liquibase formatted sql

--changeset easyops:101-create-sales-manager-role context:data
--comment: Create a sample custom Sales Manager role for RBAC testing
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Sales Manager', 'SALES_MANAGER', 'Manages sales operations with elevated permissions', false, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'SALES_MANAGER');

--changeset easyops:102-create-hr-viewer-role context:data
--comment: Create a sample custom HR Viewer role for RBAC testing
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'HR Viewer', 'HR_VIEWER', 'Read-only access to HR dashboards', false, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'HR_VIEWER');

--changeset easyops:103-assign-permissions-to-sales-manager-role context:data
--comment: Assign module permissions to the Sales Manager role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('SALES_VIEW', 'SALES_MANAGE', 'INVENTORY_VIEW')
WHERE r.code = 'SALES_MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:104-assign-permissions-to-hr-viewer-role context:data
--comment: Assign module permissions to the HR Viewer role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HR_VIEW', 'DASHBOARD_VIEW')
WHERE r.code = 'HR_VIEWER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:105-insert-sample-users context:data
--comment: Insert predefined demo users for RBAC testing (password: Admin123!)
INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'orgadmin', 'orgadmin@easyops.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Olivia', 'Admin', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'orgadmin');

INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'sales.manager', 'sales.manager@easyops.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Sam', 'Manager', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'sales.manager');

INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'hr.viewer', 'hr.viewer@easyops.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Harper', 'Viewer', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'hr.viewer');

INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'limited.user', 'limited.user@easyops.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Liam', 'Limited', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'limited.user');

--changeset easyops:106-assign-roles-to-sample-users context:data
--comment: Assign roles to predefined demo users
INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u
JOIN rbac.roles r ON r.code = 'ORG_ADMIN'
WHERE u.username = 'orgadmin'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.user_roles rp WHERE rp.user_id = u.id AND rp.role_id = r.id
  );

INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u
JOIN rbac.roles r ON r.code = 'SALES_MANAGER'
WHERE u.username = 'sales.manager'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.user_roles rp WHERE rp.user_id = u.id AND rp.role_id = r.id
  );

INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u
JOIN rbac.roles r ON r.code = 'HR_VIEWER'
WHERE u.username = 'hr.viewer'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.user_roles rp WHERE rp.user_id = u.id AND rp.role_id = r.id
  );

INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u
JOIN rbac.roles r ON r.code = 'USER'
WHERE u.username = 'limited.user'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.user_roles rp WHERE rp.user_id = u.id AND rp.role_id = r.id
  );

--changeset easyops:107-assign-sample-users-to-default-organization context:data
--comment: Attach demo users to the default organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'ADMIN', true
FROM users.users u
JOIN admin.organizations o ON o.code = 'DEMO_ORG'
WHERE u.username = 'orgadmin'
  AND NOT EXISTS (
    SELECT 1 FROM admin.user_organizations uo WHERE uo.user_id = u.id AND uo.organization_id = o.id
  );

INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'MEMBER', true
FROM users.users u
JOIN admin.organizations o ON o.code = 'DEMO_ORG'
WHERE u.username IN ('sales.manager', 'hr.viewer', 'limited.user')
  AND NOT EXISTS (
    SELECT 1 FROM admin.user_organizations uo WHERE uo.user_id = u.id AND uo.organization_id = o.id
  );

--changeset easyops:108-insert-notification-preferences-for-sample-users context:data
--comment: Ensure notification preferences exist for demo users
INSERT INTO notifications.notification_preferences (user_id, email_enabled, in_app_enabled)
SELECT u.id, true, true
FROM users.users u
WHERE u.username IN ('orgadmin', 'sales.manager', 'hr.viewer', 'limited.user')
ON CONFLICT (user_id) DO NOTHING;

