--liquibase formatted sql

--changeset easyops:022-set-non-system-roles-as-custom context:data
--comment: Only SYSTEM_ADMIN should be a system-type role; all other roles should be custom type (is_system_role = false)
UPDATE rbac.roles
SET is_system_role = false
WHERE code != 'SYSTEM_ADMIN'
  AND is_system_role = true;
