--liquibase formatted sql

--changeset hospital-service:097-doctor-portal-users-seed
--comment: Create users.users portal accounts for all active system-imported doctors that do not yet have a linked_user_id. Username = doctor_code (padded to ≥3 chars with 'DR' prefix for legacy 1-2-char codes). Password hash is BCrypt(ChangeMeDoc1!) at strength 10. Assigns PRESCRIBING_AUTHORITY and USER roles in rbac.user_roles scoped to the Aurora organisation. Updates hospital.doctors.linked_user_id on success.

-- Step 1: insert portal users for all active system-imported doctors without a linked user.
--         • Skip if a user with the same username already exists (idempotent re-run).
--         • Skip if the doctor's email is already registered to another user (use NULL instead).
--         • Doctor codes < 3 chars are prefixed with 'DR' to satisfy the username min-length rule.
INSERT INTO users.users (
    id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    is_active,
    is_verified
)
SELECT
    gen_random_uuid(),
    CASE WHEN LENGTH(d.doctor_code) < 3 THEN 'DR' || d.doctor_code ELSE d.doctor_code END,
    CASE
        WHEN d.email IS NOT NULL
             AND TRIM(d.email) <> ''
             AND NOT EXISTS (
                 SELECT 1 FROM users.users eu
                 WHERE eu.email = TRIM(d.email)
             )
        THEN TRIM(d.email)
        ELSE NULL
    END,
    '$2b$10$x8RPlhYzOZNVclTzXd7aWuQS7go4k2GNXU.n6KFfb39UaEXnQ2jCW',  -- BCrypt(ChangeMeDoc1!, strength=10)
    split_part(TRIM(d.doctor_name), ' ', 1),
    CASE
        WHEN POSITION(' ' IN TRIM(d.doctor_name)) > 0
        THEN TRIM(SUBSTRING(TRIM(d.doctor_name) FROM POSITION(' ' IN TRIM(d.doctor_name)) + 1))
        ELSE ''
    END,
    d.phone_number,
    TRUE,
    FALSE
FROM hospital.doctors d
WHERE (d.created_by = 'system-import' OR d.doctor_code LIKE 'TEST-DOC-%')
  AND d.is_active = TRUE
  AND d.linked_user_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users.users eu
      WHERE eu.username = CASE WHEN LENGTH(d.doctor_code) < 3 THEN 'DR' || d.doctor_code ELSE d.doctor_code END
  );

-- Step 2: assign PRESCRIBING_AUTHORITY and USER roles to all newly created doctor portal accounts.
--         Scoped to the Aurora Specialized Hospital organisation (stable seed UUID from changeset 033).
INSERT INTO rbac.user_roles (id, user_id, role_id, organization_id, granted_by)
SELECT
    gen_random_uuid(),
    u.id,
    r.id,
    'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid,  -- Aurora Specialized Hospital org (changeset 033)
    (SELECT au.id FROM users.users au WHERE au.username = 'admin' LIMIT 1)
FROM hospital.doctors d
JOIN users.users u
    ON u.username = CASE WHEN LENGTH(d.doctor_code) < 3 THEN 'DR' || d.doctor_code ELSE d.doctor_code END
CROSS JOIN (
    SELECT id FROM rbac.roles
    WHERE code IN ('PRESCRIBING_AUTHORITY', 'USER')
      AND is_active = TRUE
) r
WHERE (d.created_by = 'system-import' OR d.doctor_code LIKE 'TEST-DOC-%')
  AND d.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM rbac.user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

-- Step 3: link the new users back to their doctor rows.
UPDATE hospital.doctors d
SET linked_user_id = u.id,
    updated_at     = CURRENT_TIMESTAMP,
    updated_by     = 'system-import'
FROM users.users u
WHERE u.username = CASE WHEN LENGTH(d.doctor_code) < 3 THEN 'DR' || d.doctor_code ELSE d.doctor_code END
  AND (d.created_by = 'system-import' OR d.doctor_code LIKE 'TEST-DOC-%')
  AND d.linked_user_id IS NULL;
