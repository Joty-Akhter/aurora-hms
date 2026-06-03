--liquibase formatted sql

--changeset easyops:073-hr-salary-component-category
--comment: SC-02 Add category to salary_components (Basic, HRA, Special Allowance, Statutory Deduction, Loan Repayment).

SET search_path TO hr, public;

ALTER TABLE hr.salary_components
    ADD COLUMN IF NOT EXISTS category VARCHAR(80) NULL;

COMMENT ON COLUMN hr.salary_components.category IS 'Sub-type: BASIC, HRA, SPECIAL_ALLOWANCE, etc. for earnings; STATUTORY_DEDUCTION, LOAN_REPAYMENT, etc. for deductions.';
