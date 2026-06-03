--liquibase formatted sql

--changeset easyops:078-hr-employee-salary-assignment
--comment: ES-01–ES-06 EmployeeSalaryAssignment: employeeId, structureId, gradeId, bandId (optional), effectiveFrom, effectiveTo, source (Position/Override). One active per employee per date.

SET search_path TO hr, admin, public;

CREATE TABLE IF NOT EXISTS hr.employee_salary_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    salary_structure_id UUID NOT NULL REFERENCES hr.salary_structures(salary_structure_id) ON DELETE RESTRICT,
    salary_grade_id UUID NOT NULL REFERENCES hr.salary_grades(salary_grade_id) ON DELETE RESTRICT,
    salary_band_id UUID REFERENCES hr.salary_bands(salary_band_id) ON DELETE SET NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    source VARCHAR(20) NOT NULL DEFAULT 'OVERRIDE',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_esa_employee ON hr.employee_salary_assignments(employee_id);
CREATE INDEX IF NOT EXISTS idx_esa_organization ON hr.employee_salary_assignments(organization_id);
CREATE INDEX IF NOT EXISTS idx_esa_structure ON hr.employee_salary_assignments(salary_structure_id);
CREATE INDEX IF NOT EXISTS idx_esa_grade ON hr.employee_salary_assignments(salary_grade_id);
CREATE INDEX IF NOT EXISTS idx_esa_effective ON hr.employee_salary_assignments(employee_id, effective_from, effective_to);

COMMENT ON TABLE hr.employee_salary_assignments IS 'ES-01–ES-06: One active assignment per employee per date; structure/grade in same org; source Position or Override.';
