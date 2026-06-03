--liquibase formatted sql

--changeset easyops:076-hr-salary-component-calculation-basis
--comment: SC-09–SC-19 Calculation basis: default amount, percentage, base component, formula, statutory type, ceiling/floor, rounding rule.

SET search_path TO hr, public;

-- SC-09: calculation_type stores basis (FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS, FORMULA, STATUTORY, MANUAL). Existing 'fixed'/'percentage' normalized below.
-- SC-10: Default amount for Fixed (and optional for Manual)
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS default_amount DECIMAL(14,2);
COMMENT ON COLUMN hr.salary_components.default_amount IS 'SC-10: Default amount for FIXED/MANUAL calculation basis.';

-- SC-11: Percentage and base component for PercentageOfBasic / PercentageOfGross
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS percentage_value DECIMAL(8,2);
COMMENT ON COLUMN hr.salary_components.percentage_value IS 'SC-11: Percentage (0–100+) for PERCENTAGE_OF_BASIC/PERCENTAGE_OF_GROSS.';
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS base_component_code VARCHAR(100);
COMMENT ON COLUMN hr.salary_components.base_component_code IS 'SC-11: Base component code (e.g. BASIC) for percentage-based calculation. Must be earning in same org.';

-- SC-12: Formula expression
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS formula_expression VARCHAR(500);
COMMENT ON COLUMN hr.salary_components.formula_expression IS 'SC-12: Formula e.g. BASIC * 0.4. Operands: component codes, constants, + - * /. Validated for syntax and circular ref.';

-- SC-13: Statutory type reference
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS statutory_type VARCHAR(80);
COMMENT ON COLUMN hr.salary_components.statutory_type IS 'SC-13: Statutory type/reference e.g. PF_EMPLOYEE, INCOME_TAX for STATUTORY basis.';

-- SC-16: Ceiling and floor (min/max amount)
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS ceiling_amount DECIMAL(14,2);
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS floor_amount DECIMAL(14,2);
COMMENT ON COLUMN hr.salary_components.ceiling_amount IS 'SC-16: Max amount; when set with floor, ceiling >= floor.';
COMMENT ON COLUMN hr.salary_components.floor_amount IS 'SC-16: Min amount.';

-- SC-17: Rounding rule
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS rounding_rule VARCHAR(50);
COMMENT ON COLUMN hr.salary_components.rounding_rule IS 'SC-17: e.g. ROUND_NEAREST_INTEGER, ROUND_UP, TWO_DECIMALS.';

-- SC-18: Conditional applicability (optional; store as text/JSON for future rules)
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS applicability_rule VARCHAR(500);
COMMENT ON COLUMN hr.salary_components.applicability_rule IS 'SC-18 (optional): Rules e.g. employee type, grade, location for conditional applicability.';

-- Normalize existing calculation_type to enum values (FIXED, PERCENTAGE_OF_BASIC, etc.)
UPDATE hr.salary_components
SET calculation_type = CASE
    WHEN calculation_type IS NULL OR LOWER(TRIM(calculation_type)) IN ('', 'fixed') THEN 'FIXED'
    WHEN LOWER(calculation_type) = 'percentage' THEN 'PERCENTAGE_OF_BASIC'
    ELSE calculation_type
END
WHERE calculation_type IS NULL OR LOWER(TRIM(calculation_type)) IN ('', 'fixed', 'percentage');
