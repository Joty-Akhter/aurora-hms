--liquibase formatted sql

--changeset easyops:110-prescribing-authority-add-hospital-view context:data
--comment: Grant HOSPITAL_VIEW to PRESCRIBING_AUTHORITY so prescribers can use hospital nav and hospital-pharmacy catalog reads (requireHospitalView) alongside Rx permissions
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_VIEW'
WHERE r.code = 'PRESCRIBING_AUTHORITY'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
