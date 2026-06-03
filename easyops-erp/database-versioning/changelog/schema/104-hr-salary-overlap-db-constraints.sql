--liquibase formatted sql

--changeset easyops:104-hr-salary-overlap-db-constraints
--comment: NF-05/ES-33 DB-level overlap protection for salary assignments/details to avoid race-condition duplicates.

SET search_path TO hr, admin, public;

-- Needed so UUID equality can participate in GiST exclusion constraints.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- One salary assignment period per employee (per org) must not overlap.
ALTER TABLE hr.employee_salary_assignments
    DROP CONSTRAINT IF EXISTS esa_no_overlap_per_employee_period;

ALTER TABLE hr.employee_salary_assignments
    ADD CONSTRAINT esa_no_overlap_per_employee_period
    EXCLUDE USING gist (
        organization_id WITH =,
        employee_id WITH =,
        daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]') WITH &&
    );

-- One salary detail period per employee+component (per org) must not overlap.
ALTER TABLE hr.employee_salary_details
    DROP CONSTRAINT IF EXISTS esd_no_overlap_per_employee_component_period;

ALTER TABLE hr.employee_salary_details
    ADD CONSTRAINT esd_no_overlap_per_employee_component_period
    EXCLUDE USING gist (
        organization_id WITH =,
        employee_id WITH =,
        component_id WITH =,
        daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]') WITH &&
    );
