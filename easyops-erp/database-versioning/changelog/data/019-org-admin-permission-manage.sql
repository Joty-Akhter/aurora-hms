--liquibase formatted sql

--changeset easyops:019-org-admin-permission-manage context:data
--comment: ORG_ADMIN needs PERMISSION_MANAGE to add/edit permissions (AuthorizationController PERMISSION_WRITE)
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'PERMISSION_MANAGE'
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );