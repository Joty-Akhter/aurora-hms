--liquibase formatted sql

--changeset easyops:101-hospital-appointment-update-status-permission context:data
--comment: Check-in / no-show / complete without full HOSPITAL_MANAGE (see HospitalSchedulingRbacService)
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Update appointment status (scheduling)', 'HOSPITAL_APPOINTMENT_UPDATE_STATUS', 'hospital.appointment', 'update_status',
       'Mark appointments checked-in, no-show, or completed (not reschedule/cancel/book)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_APPOINTMENT_UPDATE_STATUS');

--changeset easyops:102-assign-appt-update-status-to-system-org-admins context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_UPDATE_STATUS'
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:103-hospital-role-doctor-attendants context:data
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Doctor attendants', 'DOCTOR_ATTENDANTS',
       'OPD desk: view and print/export appointments; update status (check-in, no-show, complete); cannot book, reschedule, or cancel',
       true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'DOCTOR_ATTENDANTS');

--changeset easyops:104-hospital-role-doctor-attendants-permissions context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS',
    'HOSPITAL_APPOINTMENT_VIEW',
    'HOSPITAL_APPOINTMENT_UPDATE_STATUS'
)
WHERE r.code = 'DOCTOR_ATTENDANTS'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
