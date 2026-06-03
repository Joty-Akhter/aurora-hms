--liquibase formatted sql

--changeset easyops:099-hospital-menu-clinical-chart-permission
--comment: Hospital sidebar — Clinical Chart catalog under Configurations

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical Chart', 'HOSPITAL_FEAT_CLINICAL_CHART', 'hospital.menu', 'feat_clinical_chart', 'Navigate to clinical chart (charge items) catalog'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_CHART');

INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_FEAT_CLINICAL_CHART'
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
