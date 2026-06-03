--liquibase formatted sql

--changeset easyops:200-insert-hospital-permissions context:data
--comment: Seed hospital and hospital-pharmacy RBAC permissions
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital View', 'HOSPITAL_VIEW', 'hospital', 'view', 'View hospital / EHR module'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital Manage', 'HOSPITAL_MANAGE', 'hospital', 'manage', 'Manage hospital configuration, master data and clinical records'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital Pharmacy Dispense', 'HOSPITAL_PHARMACY_DISPENSE', 'hospital.pharmacy', 'dispense', 'Dispense drugs, manage pharmacy queues, post receipts, adjustments and transfers'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_DISPENSE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital Pharmacy Stock Override', 'HOSPITAL_PHARMACY_STOCK_OVERRIDE', 'hospital.pharmacy', 'stock_override', 'Override stock levels when dispensing beyond recorded on-hand quantity'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_STOCK_OVERRIDE');

--changeset easyops:201-assign-hospital-permissions-to-system-admin context:data
--comment: Grant all hospital permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_VIEW', 'HOSPITAL_MANAGE',
    'HOSPITAL_PHARMACY_DISPENSE', 'HOSPITAL_PHARMACY_STOCK_OVERRIDE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:202-assign-hospital-permissions-to-org-admin context:data
--comment: Grant hospital view and manage permissions to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_VIEW', 'HOSPITAL_MANAGE',
    'HOSPITAL_PHARMACY_DISPENSE', 'HOSPITAL_PHARMACY_STOCK_OVERRIDE'
)
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
