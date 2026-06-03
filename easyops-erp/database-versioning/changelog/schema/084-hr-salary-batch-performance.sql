--liquibase formatted sql

--changeset easyops:084-hr-salary-batch-performance splitStatements:false
--comment: NF-03/NF-05 Batch performance and integrity indexes for employee salary assignment and details.

SET search_path TO hr, admin, public;

-- NF-03: Optimize batch retrieval of employee salary for payroll (by organization, employee, and effective period).
CREATE INDEX IF NOT EXISTS idx_employee_salary_org_employee_effective
    ON hr.employee_salary_details(organization_id, employee_id, effective_from, effective_to);

-- NF-03: Optimize batch lookup of active assignments for many employees in an organization.
CREATE INDEX IF NOT EXISTS idx_esa_org_employee_effective
    ON hr.employee_salary_assignments(organization_id, employee_id, effective_from, effective_to);

-- NF-05: These composite indexes also support efficient overlap checks per employee/component/assignment period.
