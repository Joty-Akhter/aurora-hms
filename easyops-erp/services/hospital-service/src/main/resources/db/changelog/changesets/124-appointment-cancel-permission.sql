--liquibase formatted sql

--changeset easyops:124-hospital-appointment-cancel-permission context:data
--comment: Grant CALL_CENTER and DOCTOR_ATTENDANTS the ability to cancel appointments
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Cancel appointments (scheduling)', 'HOSPITAL_APPOINTMENT_CANCEL', 'hospital.appointment', 'cancel',
       'Cancel existing OPD appointments'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_APPOINTMENT_CANCEL');

--changeset easyops:125-assign-cancel-to-system-org-admins context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_CANCEL'
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:126-assign-cancel-to-call-center context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_CANCEL'
WHERE r.code = 'CALL_CENTER'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset easyops:127-assign-cancel-to-doctor-attendants context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code = 'HOSPITAL_APPOINTMENT_CANCEL'
WHERE r.code = 'DOCTOR_ATTENDANTS'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
