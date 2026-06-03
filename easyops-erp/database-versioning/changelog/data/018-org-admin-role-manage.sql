--liquibase formatted sql

--changeset easyops:018-org-admin-role-manage context:data
--comment: ORG_ADMIN needs ROLE_MANAGE to assign roles to users and attach permissions to roles (rbac AuthorizationController ASSIGN_ROLES / RoleController ROLE_WRITE)
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'ROLE_MANAGE'
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
