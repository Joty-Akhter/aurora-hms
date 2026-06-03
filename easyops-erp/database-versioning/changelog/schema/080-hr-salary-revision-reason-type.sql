--liquibase formatted sql

--changeset easyops:080-hr-salary-revision-reason-type
--comment: ES-16–ES-17 Revision reason and type on assignment and detail; revision by new effective-dated rows; history queryable.

SET search_path TO hr, admin, public;

-- Assignments: revision reason/type for revision history
ALTER TABLE hr.employee_salary_assignments ADD COLUMN IF NOT EXISTS revision_reason VARCHAR(500);
ALTER TABLE hr.employee_salary_assignments ADD COLUMN IF NOT EXISTS revision_type VARCHAR(50);
COMMENT ON COLUMN hr.employee_salary_assignments.revision_reason IS 'ES-17: Reason for this revision (e.g. Annual increment 2024).';
COMMENT ON COLUMN hr.employee_salary_assignments.revision_type IS 'ES-17: Type of revision (e.g. ANNUAL_INCREMENT, PROMOTION).';

-- Employee salary details: revision reason/type
ALTER TABLE hr.employee_salary_details ADD COLUMN IF NOT EXISTS revision_reason VARCHAR(500);
ALTER TABLE hr.employee_salary_details ADD COLUMN IF NOT EXISTS revision_type VARCHAR(50);
COMMENT ON COLUMN hr.employee_salary_details.revision_reason IS 'ES-17: Reason for this revision.';
COMMENT ON COLUMN hr.employee_salary_details.revision_type IS 'ES-17: Type of revision (e.g. BULK_PERCENTAGE).';
