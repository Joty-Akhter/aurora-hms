--liquibase formatted sql

--changeset easyops:075-hr-salary-component-code-description splitStatements:false
-- SC-01, SC-06: Add code (unique per org), description; code immutable after creation.

-- Add new columns (nullable initially for backfill)
ALTER TABLE hr.salary_components
    ADD COLUMN IF NOT EXISTS code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Backfill code from component_name for existing rows (currently unique per org)
UPDATE hr.salary_components
SET code = TRIM(component_name)
WHERE code IS NULL AND component_name IS NOT NULL;

-- For any remaining nulls (e.g. empty name), use a generated value to satisfy NOT NULL
UPDATE hr.salary_components
SET code = 'COMP_' || REPLACE(component_id::text, '-', '')
WHERE code IS NULL;

ALTER TABLE hr.salary_components
    ALTER COLUMN code SET NOT NULL;

-- Drop old unique constraint on (organization_id, component_name)
ALTER TABLE hr.salary_components
    DROP CONSTRAINT IF EXISTS salary_components_organization_id_component_name_key;

-- Add unique constraint on (organization_id, code)
ALTER TABLE hr.salary_components
    ADD CONSTRAINT salary_components_organization_id_code_key UNIQUE (organization_id, code);
