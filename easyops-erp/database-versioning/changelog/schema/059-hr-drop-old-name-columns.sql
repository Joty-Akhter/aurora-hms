--liquibase formatted sql

--changeset easyops:059-hr-drop-old-name-columns
--comment: Legacy placeholder – do NOT drop first_name/last_name because many views still depend on them

SET search_path TO hr, public;

-- No-op on purpose. Column constraint relaxation is handled in 060-hr-relax-legacy-name-columns.sql.

