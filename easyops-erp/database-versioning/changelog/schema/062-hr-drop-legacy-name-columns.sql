--liquibase formatted sql

--changeset easyops:062-hr-drop-legacy-name-columns
--comment: After all views use employees.name, drop legacy first_name/last_name columns

SET search_path TO hr, public;

ALTER TABLE hr.employees DROP COLUMN IF EXISTS first_name;
ALTER TABLE hr.employees DROP COLUMN IF EXISTS last_name;

