--liquibase formatted sql

--changeset easyops:104-prescribing-authority-role context:data
--comment: Phase 1 — role template for prescribing (view + prescribe + transmit); assign in admin per user/org
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Prescribing authority', 'PRESCRIBING_AUTHORITY',
       'Create, validate, and transmit electronic prescriptions (org-scoped assignment)', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'PRESCRIBING_AUTHORITY');

--changeset easyops:105-prescribing-authority-role-permissions context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_PRESCRIPTION_VIEW',
    'HOSPITAL_PRESCRIPTION_PRESCRIBE',
    'HOSPITAL_PRESCRIPTION_TRANSMIT'
)
WHERE r.code = 'PRESCRIBING_AUTHORITY'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
