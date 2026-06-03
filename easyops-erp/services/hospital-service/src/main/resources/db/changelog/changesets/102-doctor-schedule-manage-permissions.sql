--liquibase formatted sql

--changeset easyops:105-hospital-doctor-schedule-manage-permission context:data
--comment: Narrow permission for doctor weekly slots/off-days sync (working hours, blackouts, roster blocks, scheduling resource patch) without full hospital.manage
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Doctor appointment schedule (slots & leave)', 'HOSPITAL_DOCTOR_SCHEDULE_MANAGE', 'hospital.doctor_schedule', 'manage',
       'Edit doctor appointment_slots/off_days and sync scheduling (working hours, blackouts, roster blocks)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_DOCTOR_SCHEDULE_MANAGE');

--changeset easyops:106-assign-doctor-schedule-manage-to-system-org-admins context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_DOCTOR_SCHEDULE_MANAGE'
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:107-doctor-attendants-doctor-schedule-and-feature context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_DOCTOR_SCHEDULE_MANAGE',
    'HOSPITAL_FEAT_DOCTOR_SCHEDULE'
)
WHERE r.code = 'DOCTOR_ATTENDANTS'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:108-update-doctor-attendants-role-description context:data
UPDATE rbac.roles
SET description = 'OPD desk: appointments (view, print, status); doctor weekly slots & leave (off-days); cannot book/reschedule/cancel appointments or edit full doctor master'
WHERE code = 'DOCTOR_ATTENDANTS';
