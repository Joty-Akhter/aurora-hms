--liquibase formatted sql

--changeset easyops:106-phase4-pharmacist-dispenser-role context:data
--comment: Phase 4 — pharmacist can dispense and read Rx for fulfillment; cannot prescribe or transmit (see RBAC_AND_PRESCRIBING_AUTHORITY_REQUIREMENTS §16.6)
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Pharmacist (dispense)', 'PHARMACIST_DISPENSER',
       'Hospital pharmacy: dispense and view prescriptions; does not include e-prescribe or transmit', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'PHARMACIST_DISPENSER');

--changeset easyops:107-phase4-pharmacist-dispenser-permissions context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_PHARMACY_DISPENSE',
    'HOSPITAL_PRESCRIPTION_VIEW',
    'HOSPITAL_VIEW'
)
WHERE r.code = 'PHARMACIST_DISPENSER'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:108-phase4-e-prescribing-transmitter-role context:data
--comment: Phase 4 — variant: transmit-only (e.g. delegate); no draft/create permission
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'E-prescribing transmitter', 'E_PRESCRIBING_TRANSMITTER',
       'View and transmit prescriptions only; cannot create or edit drafts (org-scoped assignment)', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'E_PRESCRIBING_TRANSMITTER');

--changeset easyops:109-phase4-e-prescribing-transmitter-permissions context:data
-- HOSPITAL_VIEW: required for coarse hospital navigation (MainLayout); user still lacks PRESCRIBE
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_VIEW',
    'HOSPITAL_PRESCRIPTION_VIEW',
    'HOSPITAL_PRESCRIPTION_TRANSMIT'
)
WHERE r.code = 'E_PRESCRIBING_TRANSMITTER'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
