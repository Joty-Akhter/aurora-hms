--liquibase formatted sql

--changeset easyops:073-hr-salary-component-effective-display
--comment: SC-04, SC-05: Add effective_from, effective_to, display_order to salary_components.

SET search_path TO hr, public;

-- SC-04: effective from/to dates; effective_to null = open-ended
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS effective_from DATE;
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS effective_to DATE;
-- SC-05: display order for payslips and reports
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS display_order INTEGER;

-- Backfill existing rows: effective from creation date, open-ended, order 0
UPDATE hr.salary_components SET effective_from = (created_at AT TIME ZONE 'UTC')::date WHERE effective_from IS NULL;
UPDATE hr.salary_components SET display_order = 0 WHERE display_order IS NULL;

-- Default for new rows (application can set; DB default for NOT NULL if we make it required)
ALTER TABLE hr.salary_components ALTER COLUMN display_order SET DEFAULT 0;
COMMENT ON COLUMN hr.salary_components.effective_from IS 'SC-04: Date from which this component is effective.';
COMMENT ON COLUMN hr.salary_components.effective_to IS 'SC-04: Date until which this component is effective; NULL = open-ended.';
COMMENT ON COLUMN hr.salary_components.display_order IS 'SC-05: Display order for payslips and reports (integer).';

-- Index for payroll: list components effective on a given date
CREATE INDEX IF NOT EXISTS idx_salary_component_effective
ON hr.salary_components(organization_id, effective_from, effective_to);
