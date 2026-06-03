--liquibase formatted sql

--changeset easyops:201-fix-bank-transactions-columns context:fixes splitStatements:false
--comment: Add missing audit columns to bank_transactions table
DO $$
BEGIN
    -- Add columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'running_balance') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN running_balance DECIMAL(19, 4);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'status') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN status VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'reconciliation_id') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN reconciliation_id UUID;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'created_by') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN created_by UUID;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'updated_at') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'accounting' AND table_name = 'bank_transactions' AND column_name = 'updated_by') THEN
        ALTER TABLE accounting.bank_transactions ADD COLUMN updated_by UUID;
    END IF;
    
    -- Add trigger if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_bank_transactions_updated_at') THEN
        CREATE TRIGGER update_bank_transactions_updated_at 
        BEFORE UPDATE ON accounting.bank_transactions 
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

