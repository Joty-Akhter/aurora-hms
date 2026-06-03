--liquibase formatted sql

--changeset easyops:091-assign-hospital-appointment-perms-to-system-org-admins context:data
--comment: So SYSTEM_ADMIN / ORG_ADMIN inherit appointment desk API permissions (same pattern as coarse hospital perms) and can assign roles consistently
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_APPOINTMENT_VIEW', 'HOSPITAL_APPOINTMENT_BOOK')
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
