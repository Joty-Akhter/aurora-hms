--liquibase formatted sql

--changeset easyops:097-bank-accounts-seed
--comment: Add bank accounts for organizations that have none (for deposit dropdown)
-- Ensures ALIEN_PHARMA and DEMO_ORG have bank accounts even if 090/004 seeds didn't run

-- Alien Pharma bank accounts
INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CHK-OP-001', 'Main Operating Account', 'Trust Bank', 'Gulshan Branch', 'CHECKING', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CHK-OP-001');

INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'CHK-COLL-001', 'Collection Account', 'Eastern Bank', 'Dhanmondi Branch', 'CHECKING', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'CHK-COLL-001');

INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active)
SELECT o.id, 'SAV-001', 'Business Savings', 'Trust Bank', 'Gulshan Branch', 'SAVINGS', 'BDT', 0.00, 0.00, true
FROM admin.organizations o
WHERE o.code = 'ALIEN_PHARMA'
  AND NOT EXISTS (SELECT 1 FROM accounting.bank_accounts ba WHERE ba.organization_id = o.id AND ba.account_number = 'SAV-001');


