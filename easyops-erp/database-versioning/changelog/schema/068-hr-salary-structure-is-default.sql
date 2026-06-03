--liquibase formatted sql

--changeset easyops:068-hr-salary-structure-is-default
--comment: Add is_default to salary_structures (default structure per org). SS-05, SS-06.

SET search_path TO hr, public;

ALTER TABLE hr.salary_structures ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT false;

COMMENT ON COLUMN hr.salary_structures.is_default IS 'Default structure for the organization (or org + pay frequency). Only one per org should be true. SS-06.';
