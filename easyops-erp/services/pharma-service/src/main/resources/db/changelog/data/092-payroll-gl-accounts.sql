--liquibase formatted sql

--changeset easyops:092-payroll-gl-accounts
--comment: Add payroll GL accounts (6110 Salaries, 2020 Accrued Expenses, 1030 Bank) for payroll posting to accounting
-- Run after Alien Pharma seed; ensures Post to Accounting works for payroll runs.
-- Add 6110 Salaries (EXPENSE)
INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry, description)
SELECT o.id, '6110', 'Salaries', 'EXPENSE', 'Operating Expenses', 2, false, true, true, 'Salary and wage expense for payroll posting'
FROM admin.organizations o
WHERE NOT EXISTS (SELECT 1 FROM accounting.chart_of_accounts c WHERE c.organization_id = o.id AND c.account_code = '6110');

-- Add 2020 Accrued Expenses (LIABILITY)
INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry, description)
SELECT o.id, '2020', 'Accrued Expenses', 'LIABILITY', 'Current Liabilities', 2, false, true, true, 'Accrued liabilities including payroll deductions'
FROM admin.organizations o
WHERE NOT EXISTS (SELECT 1 FROM accounting.chart_of_accounts c WHERE c.organization_id = o.id AND c.account_code = '2020');

-- Add 1030 Bank (ASSET)
INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry, description)
SELECT o.id, '1030', 'Bank - Operating Account', 'ASSET', 'Current Assets', 2, false, true, true, 'Main operating bank account for payroll disbursement'
FROM admin.organizations o
WHERE NOT EXISTS (SELECT 1 FROM accounting.chart_of_accounts c WHERE c.organization_id = o.id AND c.account_code = '1030');

-- Add EPF_PAYABLE (LIABILITY) and CASH (ASSET) for EPF posting
INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry, description)
SELECT o.id, 'EPF_PAYABLE', 'EPF Payable', 'LIABILITY', 'Current Liabilities', 2, false, true, true, 'Provident Fund liability'
FROM admin.organizations o
WHERE NOT EXISTS (SELECT 1 FROM accounting.chart_of_accounts c WHERE c.organization_id = o.id AND c.account_code = 'EPF_PAYABLE');

INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry, description)
SELECT o.id, 'CASH', 'Cash', 'ASSET', 'Current Assets', 2, false, true, true, 'Cash for EPF and general payments'
FROM admin.organizations o
WHERE NOT EXISTS (SELECT 1 FROM accounting.chart_of_accounts c WHERE c.organization_id = o.id AND c.account_code = 'CASH');
