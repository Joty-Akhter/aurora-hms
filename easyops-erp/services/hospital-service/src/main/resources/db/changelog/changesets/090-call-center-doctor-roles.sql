--liquibase formatted sql

--changeset easyops:090-hospital-appointment-clerk-permissions context:data
--comment: Appointment desk — view/list/slots without coarse HOSPITAL_VIEW; book without HOSPITAL_MANAGE (see HospitalSchedulingRbacService)
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'View appointments (scheduling)', 'HOSPITAL_APPOINTMENT_VIEW', 'hospital.appointment', 'view',
       'List and read OPD appointments, queues, resources, and availability for booking'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_APPOINTMENT_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Book appointments (scheduling)', 'HOSPITAL_APPOINTMENT_BOOK', 'hospital.appointment', 'book',
       'Create new OPD appointments (not reschedule/cancel/check-in; those remain hospital manage)'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_APPOINTMENT_BOOK');

--changeset easyops:091-hospital-role-call-center context:data
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Call Center', 'CALL_CENTER',
       'Front desk: create and view OPD appointments only (narrow sidebar)', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'CALL_CENTER');

--changeset easyops:092-hospital-role-call-center-permissions context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS',
    'HOSPITAL_APPOINTMENT_VIEW',
    'HOSPITAL_APPOINTMENT_BOOK'
)
WHERE r.code = 'CALL_CENTER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:093-hospital-role-hospital-doctor context:data
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Doctor', 'HOSPITAL_DOCTOR',
       'Clinical: doctor dashboard, doctor master data, prescribing, read appointments for queue', true, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'HOSPITAL_DOCTOR');

--changeset easyops:094-hospital-role-hospital-doctor-permissions context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HOSPITAL_FEAT_DOCTOR_DASHBOARD',
    'HOSPITAL_FEAT_DOCTORS',
    'HOSPITAL_FEAT_DOCTOR_SCHEDULE',
    'HOSPITAL_FEAT_DOCTOR_DEPARTMENTS',
    'HOSPITAL_PRESCRIPTION_VIEW',
    'HOSPITAL_PRESCRIPTION_PRESCRIBE',
    'HOSPITAL_PRESCRIPTION_TRANSMIT',
    'HOSPITAL_APPOINTMENT_VIEW'
)
WHERE r.code = 'HOSPITAL_DOCTOR'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
