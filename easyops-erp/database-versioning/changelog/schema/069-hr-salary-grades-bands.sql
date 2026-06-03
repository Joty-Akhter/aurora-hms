--liquibase formatted sql

--changeset easyops:069-hr-salary-grades-bands splitStatements:false
--comment: Add salary_grades and salary_bands tables (SS-08–SS-19).

SET search_path TO hr, public;

-- Salary Grades: named levels within a structure (SS-08–SS-12)
CREATE TABLE hr.salary_grades (
    salary_grade_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salary_structure_id UUID NOT NULL REFERENCES hr.salary_structures(salary_structure_id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500),
    effective_from DATE,
    effective_to DATE,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_salary_grades_structure_code UNIQUE (salary_structure_id, code)
);

CREATE INDEX idx_salary_grades_structure ON hr.salary_grades(salary_structure_id);
CREATE INDEX idx_salary_grades_display_order ON hr.salary_grades(salary_structure_id, display_order);

COMMENT ON TABLE hr.salary_grades IS 'Salary grades within a structure; code unique per structure, immutable (SS-08–SS-12)';

-- Salary Bands: pay ranges within a grade (SS-13–SS-19)
CREATE TABLE hr.salary_bands (
    salary_band_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salary_grade_id UUID NOT NULL REFERENCES hr.salary_grades(salary_grade_id) ON DELETE CASCADE,
    minimum_amount DECIMAL(12,2) NOT NULL,
    maximum_amount DECIMAL(12,2) NOT NULL,
    mid_point DECIMAL(12,2),
    currency VARCHAR(3),
    name VARCHAR(200),
    code VARCHAR(50),
    display_order INTEGER DEFAULT 0,
    effective_from DATE,
    effective_to DATE,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_salary_band_min_max CHECK (minimum_amount < maximum_amount),
    CONSTRAINT chk_salary_band_mid CHECK (
        mid_point IS NULL OR (mid_point >= minimum_amount AND mid_point <= maximum_amount)
    )
);

CREATE INDEX idx_salary_bands_grade ON hr.salary_bands(salary_grade_id);
CREATE INDEX idx_salary_bands_display_order ON hr.salary_bands(salary_grade_id, display_order);

COMMENT ON TABLE hr.salary_bands IS 'Pay ranges within a grade: min, max, optional mid (SS-13–SS-19)';

-- Trigger for salary_grades updated_at (align with existing pattern)
CREATE OR REPLACE FUNCTION hr.update_salary_grades_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_salary_grades_updated_at ON hr.salary_grades;
CREATE TRIGGER trg_salary_grades_updated_at
    BEFORE UPDATE ON hr.salary_grades
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_salary_grades_updated_at();

-- Trigger for salary_bands updated_at
CREATE OR REPLACE FUNCTION hr.update_salary_bands_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_salary_bands_updated_at ON hr.salary_bands;
CREATE TRIGGER trg_salary_bands_updated_at
    BEFORE UPDATE ON hr.salary_bands
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_salary_bands_updated_at();
