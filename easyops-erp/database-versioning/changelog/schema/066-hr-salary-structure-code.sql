--liquibase formatted sql

--changeset easyops:066-hr-salary-structure-code
--comment: Add code to salary_structures (unique per organization, immutable). SS-01, SS-07.

SET search_path TO hr, public;

-- Add code column (nullable for existing rows; new structures must supply code via application)
ALTER TABLE hr.salary_structures ADD COLUMN IF NOT EXISTS code VARCHAR(50);

-- Unique constraint: one structure per (organization_id, code). Multiple NULL codes allowed for legacy rows.
CREATE UNIQUE INDEX IF NOT EXISTS uk_salary_structures_org_code
    ON hr.salary_structures (organization_id, code)
    WHERE code IS NOT NULL;

COMMENT ON COLUMN hr.salary_structures.code IS 'Unique per organization; immutable after creation. Required for new structures.';
