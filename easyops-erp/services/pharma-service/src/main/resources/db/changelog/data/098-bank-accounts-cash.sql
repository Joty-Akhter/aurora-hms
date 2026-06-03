--liquibase formatted sql

--changeset easyops:098-bank-accounts-cash
--comment: Add Cash account for cash deposits (alongside bank deposits)
-- Deposit can be by bank or cash - both appear in the same dropdown

-- Alien Pharma - Cash
INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CASH-001', 'Cash', 'Cash', NULL, 'CASH', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CASH-001');

-- DEMO_ORG - Cash
INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CASH-001', 'Cash', 'Cash', NULL, 'CASH', 'USD', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'DEMO_ORG'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CASH-001');
