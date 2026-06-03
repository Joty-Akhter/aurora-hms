--liquibase formatted sql

-- ─────────────────────────────────────────────────────────────────────────────
-- Appointment-level RBAC permissions (narrower than HOSPITAL_VIEW/MANAGE)
-- Used by the third-party scheduling API role so it gets minimum required access.
-- ─────────────────────────────────────────────────────────────────────────────

--changeset easyops:300-insert-appointment-view-permission context:data
--comment: Narrow read permission for hospital appointment resources
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Appointment View', 'APPOINTMENT_VIEW', 'hospital.appointment', 'view',
       'Read doctor schedules, availability, and appointment lists'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'APPOINTMENT_VIEW');

--changeset easyops:301-insert-appointment-book-permission context:data
--comment: Permission to create (book) doctor appointments
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Appointment Book', 'APPOINTMENT_BOOK', 'hospital.appointment', 'book',
       'Book new doctor appointments on behalf of patients'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'APPOINTMENT_BOOK');

-- ─────────────────────────────────────────────────────────────────────────────
-- Role: SCHEDULING_API — minimum role for third-party appointment integration
-- ─────────────────────────────────────────────────────────────────────────────

--changeset easyops:302-create-scheduling-api-role context:data
--comment: Service role for third-party systems that need to book and view doctor appointments
INSERT INTO rbac.roles (name, code, description, is_system_role, is_active)
SELECT 'Scheduling API', 'SCHEDULING_API',
       'Minimum-privilege role for third-party appointment booking integrations',
       false, true
WHERE NOT EXISTS (SELECT 1 FROM rbac.roles WHERE code = 'SCHEDULING_API');

--changeset easyops:303-assign-permissions-to-scheduling-api-role context:data
--comment: Grant APPOINTMENT_VIEW and APPOINTMENT_BOOK to the SCHEDULING_API role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN ('APPOINTMENT_VIEW', 'APPOINTMENT_BOOK')
WHERE r.code = 'SCHEDULING_API'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- Service account user: scheduling.api
-- This account is used exclusively by third-party API key holders.
-- Password is locked (unusable bcrypt hash) — login is via API key only.
-- ─────────────────────────────────────────────────────────────────────────────

--changeset easyops:304-create-scheduling-api-service-account context:data
--comment: Service account for third-party scheduling integrations
INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
SELECT 'scheduling.api', NULL,
       '$2a$10$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA',
       'Scheduling', 'API', true, true
WHERE NOT EXISTS (SELECT 1 FROM users.users WHERE username = 'scheduling.api');

--changeset easyops:305-assign-scheduling-api-role-to-service-account context:data
--comment: Assign SCHEDULING_API role to the scheduling.api service account
INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u
JOIN rbac.roles r ON r.code = 'SCHEDULING_API'
WHERE u.username = 'scheduling.api'
  AND NOT EXISTS (
    SELECT 1 FROM rbac.user_roles rp WHERE rp.user_id = u.id AND rp.role_id = r.id
  );

--changeset easyops:306-assign-service-account-to-default-org context:data
--comment: Attach scheduling.api service account to the default organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'MEMBER', true
FROM users.users u
JOIN admin.organizations o ON o.code = 'DEMO_ORG'
WHERE u.username = 'scheduling.api'
  AND NOT EXISTS (
    SELECT 1 FROM admin.user_organizations uo
    WHERE uo.user_id = u.id AND uo.organization_id = o.id
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- Default development API key for the scheduling.api service account.
--
-- RAW KEY  :  esk_test_scheduling_default_key_for_dev_only
-- ALGORITHM:  SHA-256 of the raw key (hex)
-- ⚠️  Replace this key before going to production.
-- ─────────────────────────────────────────────────────────────────────────────

--changeset easyops:307-seed-default-scheduling-api-key context:data
--comment: Seed a default dev API key for the scheduling.api service account
INSERT INTO users.api_keys (user_id, organization_id, key_hash, name, is_active)
SELECT u.id, o.id,
       encode(sha256('esk_test_scheduling_default_key_for_dev_only'::bytea), 'hex'),
       'Default Dev Key', true
FROM users.users u
JOIN admin.organizations o ON o.code = 'DEMO_ORG'
WHERE u.username = 'scheduling.api'
  AND NOT EXISTS (
    SELECT 1 FROM users.api_keys ak
    WHERE ak.key_hash = encode(sha256('esk_test_scheduling_default_key_for_dev_only'::bytea), 'hex')
  );
