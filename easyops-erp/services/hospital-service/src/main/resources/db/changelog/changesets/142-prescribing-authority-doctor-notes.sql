--liquibase formatted sql

--changeset easyops:142-prescribing-authority-doctor-notes context:data
--comment: Prescribing authority may view and manage hospital-wide doctor notes

INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_FEAT_DOCTOR_NOTES', 'HOSPITAL_DOCTOR_NOTES_MANAGE')
WHERE r.code = 'PRESCRIBING_AUTHORITY'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
