--liquibase formatted sql

--changeset easyops:020-org-admin-permission-view context:data
--comment: ORG_ADMIN needs PERMISSION_VIEW to view permissions before managing them
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'PERMISSION_VIEW'
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );