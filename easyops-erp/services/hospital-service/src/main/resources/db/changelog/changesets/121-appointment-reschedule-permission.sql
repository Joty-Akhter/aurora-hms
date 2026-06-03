--liquibase formatted sql

--changeset easyops:121-hospital-appointment-reschedule-permission context:data
--comment: Grant CALL_CENTER and DOCTOR_ATTENDANTS the ability to reschedule appointments
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Reschedule appointments (scheduling)', 'HOSPITAL_APPOINTMENT_RESCHEDULE', 'hospital.appointment', 'reschedule',
       'Move/reschedule existing OPD appointments to a new slot'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_APPOINTMENT_RESCHEDULE');

--changeset easyops:122-assign-reschedule-to-call-center context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_RESCHEDULE'
WHERE r.code = 'CALL_CENTER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:123-assign-reschedule-to-doctor-attendants context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_RESCHEDULE'
WHERE r.code = 'DOCTOR_ATTENDANTS'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
