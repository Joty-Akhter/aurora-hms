--liquibase formatted sql

--changeset easyops:110-pharmacy-p2-stock-override-charge-permissions context:data
--comment: Pharmacy P2 — stock override and billing charge posting from pharmacy integration
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy stock override', 'HOSPITAL_PHARMACY_STOCK_OVERRIDE', 'hospital.pharmacy', 'stock_override',
       'Override recorded stock limits when policy allows (audit trail required)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_STOCK_OVERRIDE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy post charges', 'HOSPITAL_PHARMACY_CHARGE_POST', 'hospital.pharmacy', 'charge_post',
       'Post dispense charges to hospital billing from pharmacy workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_CHARGE_POST');

--changeset easyops:111-pharmacy-p2-grant-new-permissions-to-system-admin context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_PHARMACY_STOCK_OVERRIDE', 'HOSPITAL_PHARMACY_CHARGE_POST')
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:112-pharmacy-p2-grant-to-pharmacist-dispenser context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('HOSPITAL_PHARMACY_STOCK_OVERRIDE', 'HOSPITAL_PHARMACY_CHARGE_POST')
WHERE r.code = 'PHARMACIST_DISPENSER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
