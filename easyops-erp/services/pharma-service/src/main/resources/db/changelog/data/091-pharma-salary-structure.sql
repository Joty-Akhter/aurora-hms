--liquibase formatted sql

--changeset easyops:091-pharma-salary-structure context:demo
--comment: Alien Pharma salary structure with 10 grades (12000-80000 BDT) and 5 components (Basic, HRA, Medical, Travel, PF).
--validCheckSum 1:any

SET search_path TO hr, public;

-- ============================================================================
-- 1. SALARY STRUCTURE
-- ============================================================================
INSERT INTO hr.salary_structures (
    salary_structure_id, organization_id, code, structure_name,
    currency, pay_frequency, effective_from, is_active, is_default, description
)
SELECT
    gen_random_uuid(),
    o.id,
    'PHARMA_GRADE_STRUCTURE',
    'Pharma Salary Structure',
    'BDT',
    'monthly',
    '2026-01-01'::date,
    true,
    true,
    'Alien Pharma salary structure with 10 grades from 12000 to 80000 BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_structures ss WHERE ss.organization_id = o.id AND ss.code = 'PHARMA_GRADE_STRUCTURE');

-- ============================================================================
-- 2. SALARY GRADES (10 grades: 12000 to 80000 BDT)
-- Linear: 12000, 19556, 27111, 34667, 42222, 49778, 57333, 64889, 72444, 80000
-- ============================================================================
INSERT INTO hr.salary_grades (
    salary_grade_id, salary_structure_id, code, name, display_order
)
SELECT
    gen_random_uuid(),
    ss.salary_structure_id,
    'G' || g.grade_num,
    'Grade ' || g.grade_num,
    g.grade_num - 1
FROM hr.salary_structures ss
JOIN admin.organizations o ON ss.organization_id = o.id AND o.code = 'ALIEN_PHARMA'
CROSS JOIN (
    SELECT 1 AS grade_num, 12000.00 AS base_salary UNION ALL SELECT 2, 19556.00 UNION ALL SELECT 3, 27111.00 UNION ALL
    SELECT 4, 34667.00 UNION ALL SELECT 5, 42222.00 UNION ALL SELECT 6, 49778.00 UNION ALL
    SELECT 7, 57333.00 UNION ALL SELECT 8, 64889.00 UNION ALL SELECT 9, 72444.00 UNION ALL SELECT 10, 80000.00
) g
WHERE ss.code = 'PHARMA_GRADE_STRUCTURE'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_grades sg WHERE sg.salary_structure_id = ss.salary_structure_id AND sg.code = 'G' || g.grade_num);

-- ============================================================================
-- 3. SALARY BANDS (one band per grade; min-max range with base as mid)
-- Constraint: minimum_amount < maximum_amount
-- ============================================================================
INSERT INTO hr.salary_bands (
    salary_band_id, salary_grade_id, minimum_amount, maximum_amount, mid_point, currency, code, display_order
)
SELECT
    gen_random_uuid(),
    sg.salary_grade_id,
    g.base_salary,
    g.base_salary + 1,
    g.base_salary,
    'BDT',
    'BAND_G' || g.grade_num,
    g.grade_num - 1
FROM hr.salary_grades sg
JOIN hr.salary_structures ss ON sg.salary_structure_id = ss.salary_structure_id
JOIN admin.organizations o ON ss.organization_id = o.id AND o.code = 'ALIEN_PHARMA'
CROSS JOIN (
    SELECT 1 AS grade_num, 12000.00 AS base_salary UNION ALL SELECT 2, 19556.00 UNION ALL SELECT 3, 27111.00 UNION ALL
    SELECT 4, 34667.00 UNION ALL SELECT 5, 42222.00 UNION ALL SELECT 6, 49778.00 UNION ALL
    SELECT 7, 57333.00 UNION ALL SELECT 8, 64889.00 UNION ALL SELECT 9, 72444.00 UNION ALL SELECT 10, 80000.00
) g
WHERE ss.code = 'PHARMA_GRADE_STRUCTURE'
  AND sg.code = 'G' || g.grade_num
  AND NOT EXISTS (SELECT 1 FROM hr.salary_bands sb WHERE sb.salary_grade_id = sg.salary_grade_id);

-- ============================================================================
-- 4. SALARY COMPONENTS (5 components)
-- 1. Basic - fixed taxable
-- 2. House rent - 70% of basic, non taxable
-- 3. Medical Allowance - 10% of basic, non taxable
-- 4. Travel Allowance - 20% of basic, non taxable
-- 5. Provident Fund (deduct) - 10% of basic
-- ============================================================================

-- 4.1 Basic (fixed, taxable)
INSERT INTO hr.salary_components (
    component_id, organization_id, code, component_name, component_type, calculation_type,
    category, is_taxable, taxability, is_active, display_order, effective_from, currency
)
SELECT gen_random_uuid(), o.id, 'BASIC', 'Basic Salary', 'earning', 'FIXED',
    'BASIC', true, 'TAXABLE', true, 1, '2026-01-01'::date, 'BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_components sc WHERE sc.organization_id = o.id AND sc.code = 'BASIC');

-- 4.2 House Rent (70% of basic, non taxable)
INSERT INTO hr.salary_components (
    component_id, organization_id, code, component_name, component_type, calculation_type,
    base_component_code, percentage_value, category, is_taxable, taxability, is_active, display_order, effective_from, currency
)
SELECT gen_random_uuid(), o.id, 'HOUSE_RENT', 'House Rent Allowance', 'earning', 'PERCENTAGE_OF_BASIC',
    'BASIC', 70.00, 'HRA', false, 'EXEMPT', true, 2, '2026-01-01'::date, 'BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_components sc WHERE sc.organization_id = o.id AND sc.code = 'HOUSE_RENT');

-- 4.3 Medical Allowance (10% of basic, non taxable)
INSERT INTO hr.salary_components (
    component_id, organization_id, code, component_name, component_type, calculation_type,
    base_component_code, percentage_value, category, is_taxable, taxability, is_active, display_order, effective_from, currency
)
SELECT gen_random_uuid(), o.id, 'MEDICAL', 'Medical Allowance', 'earning', 'PERCENTAGE_OF_BASIC',
    'BASIC', 10.00, 'MEDICAL', false, 'EXEMPT', true, 3, '2026-01-01'::date, 'BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_components sc WHERE sc.organization_id = o.id AND sc.code = 'MEDICAL');

-- 4.4 Travel Allowance (20% of basic, non taxable)
INSERT INTO hr.salary_components (
    component_id, organization_id, code, component_name, component_type, calculation_type,
    base_component_code, percentage_value, category, is_taxable, taxability, is_active, display_order, effective_from, currency
)
SELECT gen_random_uuid(), o.id, 'TRAVEL', 'Travel Allowance', 'earning', 'PERCENTAGE_OF_BASIC',
    'BASIC', 20.00, 'CONVEYANCE', false, 'EXEMPT', true, 4, '2026-01-01'::date, 'BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_components sc WHERE sc.organization_id = o.id AND sc.code = 'TRAVEL');

-- 4.5 Provident Fund - Employee contribution (10% of basic, deduction)
INSERT INTO hr.salary_components (
    component_id, organization_id, code, component_name, component_type, calculation_type,
    base_component_code, percentage_value, category, is_taxable, taxability, is_statutory, is_active, display_order, effective_from, currency
)
SELECT gen_random_uuid(), o.id, 'PF_EMPLOYEE', 'Provident Fund (Employee)', 'deduction', 'PERCENTAGE_OF_BASIC',
    'BASIC', 10.00, 'STATUTORY_DEDUCTION', false, 'EXEMPT', true, true, 5, '2026-01-01'::date, 'BDT'
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_components sc WHERE sc.organization_id = o.id AND sc.code = 'PF_EMPLOYEE');

-- 4.6 PF statutory tags (PF_WAGE for Basic, PF_EMPLOYEE for the deduction)
INSERT INTO hr.salary_component_statutory_tags (component_id, tag)
SELECT sc.component_id, 'PF_WAGE'
FROM hr.salary_components sc
JOIN admin.organizations o ON sc.organization_id = o.id
WHERE o.code = 'ALIEN_PHARMA' AND sc.code = 'BASIC'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_component_statutory_tags sct WHERE sct.component_id = sc.component_id AND sct.tag = 'PF_WAGE');

INSERT INTO hr.salary_component_statutory_tags (component_id, tag)
SELECT sc.component_id, 'PF_EMPLOYEE'
FROM hr.salary_components sc
JOIN admin.organizations o ON sc.organization_id = o.id
WHERE o.code = 'ALIEN_PHARMA' AND sc.code = 'PF_EMPLOYEE'
  AND NOT EXISTS (SELECT 1 FROM hr.salary_component_statutory_tags sct WHERE sct.component_id = sc.component_id AND sct.tag = 'PF_EMPLOYEE');
