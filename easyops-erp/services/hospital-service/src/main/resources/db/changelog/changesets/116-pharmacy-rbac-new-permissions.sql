--liquibase formatted sql

--changeset easyops:116a-pharmacy-p5-new-permissions context:data
--comment: Pharmacy P5 — RBAC permission codes for new workflow capabilities
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy requisition approve', 'HOSPITAL_PHARMACY_REQUISITION_APPROVE', 'hospital.pharmacy', 'requisition_approve',
       'Approve or reject inter-location stock requisitions in pharmacy workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_REQUISITION_APPROVE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy emergency purchase approve', 'HOSPITAL_PHARMACY_EMERGENCY_PURCHASE_APPROVE', 'hospital.pharmacy', 'emergency_purchase_approve',
       'Approve emergency purchase entries before stock is received into pharmacy locations'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_EMERGENCY_PURCHASE_APPROVE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy credit manage', 'HOSPITAL_PHARMACY_CREDIT_MANAGE', 'hospital.pharmacy', 'credit_manage',
       'Manage patient credit accounts and record payments in the pharmacy'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_CREDIT_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Pharmacy stock adjustment approve', 'HOSPITAL_PHARMACY_STOCK_ADJUSTMENT_APPROVE', 'hospital.pharmacy', 'stock_adjustment_approve',
       'Review and approve pending pharmacy stock adjustment requests (audit trail required)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PHARMACY_STOCK_ADJUSTMENT_APPROVE');

--changeset easyops:116b-pharmacy-p5-grant-to-system-admin context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_PHARMACY_REQUISITION_APPROVE',
    'HOSPITAL_PHARMACY_EMERGENCY_PURCHASE_APPROVE',
    'HOSPITAL_PHARMACY_CREDIT_MANAGE',
    'HOSPITAL_PHARMACY_STOCK_ADJUSTMENT_APPROVE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
