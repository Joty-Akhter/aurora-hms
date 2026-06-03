--liquibase formatted sql

--changeset easyops:128-hospital-patient-ehr-permissions context:data
--comment: Granular permissions for Patient EHR data management (add, edit, delete)
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Add patient EHR data', 'HOSPITAL_PATIENT_EHR_ADD', 'hospital.patient_ehr', 'add',
       'Create new patient EHR records (clinical notes, vitals, encounter data)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PATIENT_EHR_ADD');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Edit patient EHR data', 'HOSPITAL_PATIENT_EHR_EDIT', 'hospital.patient_ehr', 'edit',
       'Edit existing patient EHR records'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PATIENT_EHR_EDIT');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Delete patient EHR data', 'HOSPITAL_PATIENT_EHR_DELETE', 'hospital.patient_ehr', 'delete',
       'Delete patient EHR records'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_PATIENT_EHR_DELETE');

--changeset easyops:129-hospital-role-ehr-opd context:data
--comment: Create EHR-OPD role for staff who manage patient EHR data in OPD setting
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'EHR OPD', 'EHR_OPD',
       'OPD staff: add, edit, and delete patient EHR data (clinical notes, vitals, encounter records)',
       false, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'EHR_OPD');

--changeset easyops:130-assign-ehr-opd-permissions context:data
--comment: Grant EHR-OPD role hospital view access + patient EHR CRUD permissions
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_VIEW',
    'HOSPITAL_FEAT_PATIENTS',
    'HOSPITAL_PATIENT_EHR_ADD',
    'HOSPITAL_PATIENT_EHR_EDIT',
    'HOSPITAL_PATIENT_EHR_DELETE'
)
WHERE r.code = 'EHR_OPD'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
