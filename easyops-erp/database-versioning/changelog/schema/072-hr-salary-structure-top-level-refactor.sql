--liquibase formatted sql

--changeset easyops:072-hr-salary-structure-top-level-refactor
--comment: Refactor salary_structures to top-level only: add description; remove base_salary and position_id (moved to grade/band and position linkage). SS entity improvement.

SET search_path TO hr, admin, public;

-- Add description (SS-01: structure has name, description)
ALTER TABLE hr.salary_structures ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
COMMENT ON COLUMN hr.salary_structures.description IS 'Optional description of the salary structure.';

-- Update v_employee_salary_summary: join via position.default_salary_structure_id; remove base_salary (pay from grades/bands or employee details)
-- PostgreSQL does not allow CREATE OR REPLACE VIEW to drop columns; must DROP first.
DROP VIEW IF EXISTS hr.v_employee_salary_summary;
CREATE VIEW hr.v_employee_salary_summary AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    p.title AS position_title,
    ss.structure_name AS salary_structure,
    ss.currency,
    ss.pay_frequency,
    COUNT(DISTINCT esd.salary_detail_id) AS component_count,
    SUM(CASE WHEN sc.component_type = 'earning' THEN esd.amount ELSE 0 END) AS total_earnings,
    SUM(CASE WHEN sc.component_type = 'deduction' THEN esd.amount ELSE 0 END) AS total_deductions
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.positions p ON e.position_id = p.position_id
LEFT JOIN hr.salary_structures ss ON p.default_salary_structure_id = ss.salary_structure_id AND ss.is_active = true
LEFT JOIN hr.employee_salary_details esd ON e.employee_id = esd.employee_id AND esd.is_active = true
LEFT JOIN hr.salary_components sc ON esd.component_id = sc.component_id
WHERE e.employment_status = 'active'
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.name,
         d.name, p.title, ss.structure_name, ss.currency, ss.pay_frequency;

-- Drop index and columns no longer used on salary_structures
DROP INDEX IF EXISTS hr.idx_salary_structure_position;
ALTER TABLE hr.salary_structures DROP COLUMN IF EXISTS position_id;
ALTER TABLE hr.salary_structures DROP COLUMN IF EXISTS base_salary;
