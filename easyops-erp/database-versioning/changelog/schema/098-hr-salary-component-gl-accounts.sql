-- INT-20: Optional GL account codes on salary component for payroll journal mapping (expense vs liability/withholding).
--changeset easyops:098-hr-salary-component-gl-accounts splitStatements:false

ALTER TABLE hr.salary_components
    ADD COLUMN IF NOT EXISTS expense_account_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS liability_account_code VARCHAR(64);

COMMENT ON COLUMN hr.salary_components.expense_account_code IS 'INT-20: Chart of Accounts code for salary expense when this component is an earning (optional; used in payroll accounting export).';
COMMENT ON COLUMN hr.salary_components.liability_account_code IS 'INT-20: COA code for deduction/withholding line when this component is a deduction (optional).';
