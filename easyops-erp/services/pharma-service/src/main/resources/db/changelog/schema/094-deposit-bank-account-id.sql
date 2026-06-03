--liquibase formatted sql

--changeset easyops:094-deposit-bank-account-id context:pharma
--comment: Add bank_account_id to deposits for linking to configured bank accounts
ALTER TABLE pharma.deposits
ADD COLUMN IF NOT EXISTS bank_account_id UUID REFERENCES accounting.bank_accounts(id);

CREATE INDEX IF NOT EXISTS idx_deposits_bank_account_id ON pharma.deposits(bank_account_id);
