--liquibase formatted sql

--changeset easyops:099-add-hospital-permissions context:data
--comment: Seed hospital/EHR module permissions for Permission Management and role assignment
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital View', 'HOSPITAL_VIEW', 'hospital', 'view', 'View hospital/EHR dashboards, patients, prescriptions, lab results, and clinical data'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital Manage', 'HOSPITAL_MANAGE', 'hospital', 'manage', 'Manage hospital/EHR configuration, patients, prescriptions, clinical notes, lab orders, and imaging studies'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_MANAGE');

--changeset easyops:100-assign-hospital-permissions-to-system-admin context:data
--comment: Grant hospital permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_VIEW', 'HOSPITAL_MANAGE')
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:101-assign-hospital-permissions-to-org-admin context:data
--comment: Grant hospital view and manage to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_VIEW', 'HOSPITAL_MANAGE')
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:102-add-hospital-prescription-pharmacy-permissions context:data
--comment: Fine-grained prescription and hospital pharmacy dispense permissions (same changelog as hospital-service)
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'View prescriptions', 'HOSPITAL_PRESCRIPTION_VIEW', 'hospital.prescription', 'view', 'Read prescription lists and details'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PRESCRIPTION_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Prescribe', 'HOSPITAL_PRESCRIPTION_PRESCRIBE', 'hospital.prescription', 'prescribe', 'Create and edit draft prescriptions, validate, cancel draft'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PRESCRIPTION_PRESCRIBE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Transmit prescription', 'HOSPITAL_PRESCRIPTION_TRANSMIT', 'hospital.prescription', 'transmit', 'Transmit prescription to pharmacy (e-prescribe)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PRESCRIPTION_TRANSMIT');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy dispense', 'HOSPITAL_PHARMACY_DISPENSE', 'hospital.pharmacy', 'dispense', 'Record dispensing and stock issue'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_DISPENSE');

--changeset easyops:103-assign-hospital-rx-pharmacy-to-system-admin context:data
--comment: Grant prescription/pharmacy dispense permissions to System Administrator
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_PRESCRIPTION_VIEW',
    'HOSPITAL_PRESCRIPTION_PRESCRIBE',
    'HOSPITAL_PRESCRIPTION_TRANSMIT',
    'HOSPITAL_PHARMACY_DISPENSE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
