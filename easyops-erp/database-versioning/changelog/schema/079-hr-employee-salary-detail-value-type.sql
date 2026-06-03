--liquibase formatted sql

--changeset easyops:079-hr-employee-salary-detail-value-type
--comment: ES-07–ES-15 Employee salary component: valueType (Amount|Percentage|UseMasterDefault), nullable amount; one active per employee per component per date.

SET search_path TO hr, admin, public;

-- ES-07: value_type (AMOUNT, PERCENTAGE, USE_MASTER_DEFAULT)
ALTER TABLE hr.employee_salary_details
    ADD COLUMN IF NOT EXISTS value_type VARCHAR(30) NOT NULL DEFAULT 'AMOUNT';

-- When value_type is PERCENTAGE or USE_MASTER_DEFAULT, amount can be null; when AMOUNT, amount required (enforced in app)
ALTER TABLE hr.employee_salary_details
    ALTER COLUMN amount DROP NOT NULL;

COMMENT ON COLUMN hr.employee_salary_details.value_type IS 'ES-07: AMOUNT | PERCENTAGE | USE_MASTER_DEFAULT.';
