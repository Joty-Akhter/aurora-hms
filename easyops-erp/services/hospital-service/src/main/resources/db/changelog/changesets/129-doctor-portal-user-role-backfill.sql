--liquibase formatted sql

--changeset hospital-service:129-doctor-portal-user-role-backfill context:data
--comment: Backfill USER role for doctor portal accounts that have PRESCRIBING_AUTHORITY but were created without USER (fixes Access Denied on login/dashboard).

INSERT INTO rbac.user_roles (id, user_id, role_id, organization_id, granted_by)
SELECT
    gen_random_uuid(),
    d.linked_user_id,
    user_role.id,
    ur_pa.organization_id,
    (SELECT au.id FROM users.users au WHERE au.username = 'admin' LIMIT 1)
FROM hospital.doctors d
JOIN rbac.user_roles ur_pa ON ur_pa.user_id = d.linked_user_id
JOIN rbac.roles pres_role ON pres_role.id = ur_pa.role_id AND pres_role.code = 'PRESCRIBING_AUTHORITY'
JOIN rbac.roles user_role ON user_role.code = 'USER' AND user_role.is_active = TRUE
WHERE d.linked_user_id IS NOT NULL
  AND d.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM rbac.user_roles ur_user
      JOIN rbac.roles r_user ON r_user.id = ur_user.role_id
      WHERE ur_user.user_id = d.linked_user_id
        AND ur_user.organization_id = ur_pa.organization_id
        AND r_user.code = 'USER'
  );
