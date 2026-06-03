--liquibase formatted sql

--changeset hospital-service:138-aurora-scheduling-api-org-link
--comment: Link scheduling.api service account to Aurora (ASHK) for public web booking integration

INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'MEMBER', false
FROM users.users u
JOIN admin.organizations o ON o.code = 'ASHK'
WHERE u.username = 'scheduling.api'
  AND NOT EXISTS (
    SELECT 1 FROM admin.user_organizations uo
    WHERE uo.user_id = u.id AND uo.organization_id = o.id
  );

INSERT INTO users.api_keys (user_id, organization_id, key_hash, name, is_active)
SELECT u.id, o.id,
       encode(sha256('esk_test_scheduling_aurora_web_key'::bytea), 'hex'),
       'Aurora Web Booking Dev Key', true
FROM users.users u
JOIN admin.organizations o ON o.code = 'ASHK'
WHERE u.username = 'scheduling.api'
  AND NOT EXISTS (
    SELECT 1 FROM users.api_keys ak
    WHERE ak.key_hash = encode(sha256('esk_test_scheduling_aurora_web_key'::bytea), 'hex')
  );

--rollback DELETE FROM users.api_keys WHERE name = 'Aurora Web Booking Dev Key';
--rollback DELETE FROM admin.user_organizations uo USING users.users u, admin.organizations o WHERE uo.user_id = u.id AND uo.organization_id = o.id AND u.username = 'scheduling.api' AND o.code = 'ASHK' AND uo.is_primary = false;
