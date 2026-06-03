--liquibase formatted sql

--changeset hospital-service:132-doctor-hospital-notes-menu-permission context:data
--comment: Menu/route access for hospital-wide doctor broadcast notes
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctor notes', 'HOSPITAL_FEAT_DOCTOR_NOTES', 'hospital.menu', 'feat_doctor_notes',
       'View hospital-wide doctor broadcast notes (chamber closed, running late, etc.)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_DOCTOR_NOTES');

--changeset hospital-service:132-doctor-hospital-notes-manage-permission context:data
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Doctor notes: create and manage', 'HOSPITAL_DOCTOR_NOTES_MANAGE', 'hospital.doctor_notes', 'manage',
       'Post, edit, and delete doctor broadcast notes (creator or named doctor)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_DOCTOR_NOTES_MANAGE');

--changeset hospital-service:132-grant-doctor-notes-view context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_FEAT_DOCTOR_NOTES'
WHERE r.code IN ('CALL_CENTER', 'HOSPITAL_DOCTOR', 'DOCTOR_ATTENDANTS')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset hospital-service:132-grant-doctor-notes-manage context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_DOCTOR_NOTES_MANAGE'
WHERE r.code IN ('HOSPITAL_DOCTOR', 'DOCTOR_ATTENDANTS', 'SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
