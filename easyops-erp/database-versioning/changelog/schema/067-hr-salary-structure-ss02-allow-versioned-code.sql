--liquibase formatted sql

--changeset easyops:067-hr-salary-structure-ss02-allow-versioned-code
--comment: SS-02 Allow multiple structures per (org, code) with non-overlapping effective dates. Drop unique on (organization_id, code); overlap enforced in application.

SET search_path TO hr, public;

-- Drop the unique index that allowed only one row per (organization_id, code).
-- SS-02: only one *active* structure per code per effective date (no overlapping periods).
-- Application validates overlap on create/update; DB no longer enforces single row per code.
DROP INDEX IF EXISTS hr.uk_salary_structures_org_code;
