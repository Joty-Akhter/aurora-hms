--liquibase formatted sql

--changeset easyops:060-hr-relax-legacy-name-columns
--comment: Make legacy first_name/last_name nullable so inserts using name-only model no longer fail

SET search_path TO hr, public;

-- Only relax NOT NULL; keep columns for dependent legacy views
ALTER TABLE hr.employees ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE hr.employees ALTER COLUMN last_name DROP NOT NULL;

--rollback ALTER TABLE hr.employees ALTER COLUMN first_name SET NOT NULL;
--rollback ALTER TABLE hr.employees ALTER COLUMN last_name SET NOT NULL;

