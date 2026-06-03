--liquibase formatted sql

--changeset easyops:101-fix-reminder-tables context:fixes
--comment: Fix reminder tables structure
-- This changeset applies fixes from _fix_reminder_tables.sql

-- Fix reminder_config table if needed
ALTER TABLE accounting.reminder_config ALTER COLUMN email_template_before_due TYPE VARCHAR(2000);
ALTER TABLE accounting.reminder_config ALTER COLUMN email_template_level1 TYPE VARCHAR(2000);
ALTER TABLE accounting.reminder_config ALTER COLUMN email_template_level2 TYPE VARCHAR(2000);
ALTER TABLE accounting.reminder_config ALTER COLUMN email_template_level3 TYPE VARCHAR(2000);

--changeset easyops:102-fix-customers-table context:fixes splitStatements:false
--comment: Fix customers table structure
-- This changeset applies fixes from _fix_customers_table.sql

-- Add missing columns to customers table if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'accounting' 
                   AND table_name = 'customers' 
                   AND column_name = 'contact_person') THEN
        ALTER TABLE accounting.customers ADD COLUMN contact_person VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'accounting' 
                   AND table_name = 'customers' 
                   AND column_name = 'billing_address') THEN
        ALTER TABLE accounting.customers ADD COLUMN billing_address TEXT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'accounting' 
                   AND table_name = 'customers' 
                   AND column_name = 'shipping_address') THEN
        ALTER TABLE accounting.customers ADD COLUMN shipping_address TEXT;
    END IF;
END $$;

--changeset easyops:103-fix-ar-invoices-period-id context:fixes
--comment: Fix AR invoices period_id column
-- This changeset applies fixes from _fix_ar_invoices_period_id.sql

-- Update period_id column to be NOT NULL if it's currently nullable
ALTER TABLE accounting.ar_invoices ALTER COLUMN period_id SET NOT NULL;

--changeset easyops:104-fix-ar-credit-notes context:fixes splitStatements:false
--comment: Fix AR credit notes table structure
-- This changeset applies fixes from _fix_ar_credit_notes.sql

-- Fix credit notes table structure
ALTER TABLE accounting.ar_credit_notes ALTER COLUMN created_by TYPE UUID USING created_by::UUID;
ALTER TABLE accounting.ar_credit_notes ALTER COLUMN updated_by TYPE UUID USING updated_by::UUID;

-- Add foreign key constraints if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_ar_credit_notes_created_by' 
                   AND table_schema = 'accounting') THEN
        ALTER TABLE accounting.ar_credit_notes 
        ADD CONSTRAINT fk_ar_credit_notes_created_by 
        FOREIGN KEY (created_by) REFERENCES users.users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_ar_credit_notes_updated_by' 
                   AND table_schema = 'accounting') THEN
        ALTER TABLE accounting.ar_credit_notes 
        ADD CONSTRAINT fk_ar_credit_notes_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users.users(id);
    END IF;
END $$;
