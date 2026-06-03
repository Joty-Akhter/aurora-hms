--liquibase formatted sql

--changeset easyops:071-hr-position-default-structure-and-scope
--comment: SS-24–SS-27 Position default salary structure/grade/band; optional structure assignment by department/location.

SET search_path TO hr, admin, public;

-- Position: default salary structure (SS-26)
ALTER TABLE hr.positions ADD COLUMN IF NOT EXISTS default_salary_structure_id UUID REFERENCES hr.salary_structures(salary_structure_id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_positions_default_salary_structure ON hr.positions(default_salary_structure_id);
COMMENT ON COLUMN hr.positions.default_salary_structure_id IS 'Default salary structure for this position; new hires/transfers may inherit. SS-26.';

-- Position: default grade and band (SS-26; tables from 069)
ALTER TABLE hr.positions ADD COLUMN IF NOT EXISTS default_salary_grade_id UUID REFERENCES hr.salary_grades(salary_grade_id) ON DELETE SET NULL;
ALTER TABLE hr.positions ADD COLUMN IF NOT EXISTS default_salary_band_id UUID REFERENCES hr.salary_bands(salary_band_id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_positions_default_grade ON hr.positions(default_salary_grade_id);
CREATE INDEX IF NOT EXISTS idx_positions_default_band ON hr.positions(default_salary_band_id);
COMMENT ON COLUMN hr.positions.default_salary_grade_id IS 'Default salary grade for this position. SS-26. Must belong to default_salary_structure.';
COMMENT ON COLUMN hr.positions.default_salary_band_id IS 'Default salary band for this position. SS-26. Must belong to default grade.';

-- Optional structure assignment by department/location (SS-25). Both null = org-wide.
CREATE TABLE IF NOT EXISTS hr.salary_structure_scope (
    scope_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    salary_structure_id UUID NOT NULL REFERENCES hr.salary_structures(salary_structure_id) ON DELETE CASCADE,
    department_id UUID REFERENCES admin.departments(id) ON DELETE CASCADE,
    location_id UUID REFERENCES admin.locations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_salary_structure_scope_org_dept_loc
    ON hr.salary_structure_scope (organization_id, COALESCE(department_id, '00000000-0000-0000-0000-000000000000'::uuid), COALESCE(location_id, '00000000-0000-0000-0000-000000000000'::uuid));
CREATE INDEX IF NOT EXISTS idx_salary_structure_scope_org ON hr.salary_structure_scope(organization_id);
CREATE INDEX IF NOT EXISTS idx_salary_structure_scope_structure ON hr.salary_structure_scope(salary_structure_id);
CREATE INDEX IF NOT EXISTS idx_salary_structure_scope_dept ON hr.salary_structure_scope(department_id);
CREATE INDEX IF NOT EXISTS idx_salary_structure_scope_location ON hr.salary_structure_scope(location_id);
COMMENT ON TABLE hr.salary_structure_scope IS 'Optional structure assignment by org/department/location. SS-25.';
