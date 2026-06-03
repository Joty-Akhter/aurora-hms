--liquibase formatted sql

--changeset easyops:096-add-pharma-permissions context:data
--comment: Seed pharma module permissions for Permission Management and role assignment
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharma View', 'PHARMA_VIEW', 'pharma', 'view', 'View pharma dashboards, territories, and field force data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PHARMA_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharma Manage', 'PHARMA_MANAGE', 'pharma', 'manage', 'Manage pharma configuration, territories, targets, and incentives'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PHARMA_MANAGE');

--changeset easyops:097-assign-pharma-permissions-to-system-admin context:data
--comment: Grant pharma permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('PHARMA_VIEW', 'PHARMA_MANAGE')
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:098-assign-pharma-permissions-to-org-admin context:data
--comment: Grant pharma view and manage to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('PHARMA_VIEW', 'PHARMA_MANAGE')
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

