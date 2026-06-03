-- Fix customers table - Add missing current_balance column
-- This script can be run multiple times safely

-- Add current_balance column if it doesn't exist
ALTER TABLE accounting.customers 
ADD COLUMN IF NOT EXISTS current_balance DECIMAL(19, 4) DEFAULT 0;

-- Comment on the column
COMMENT ON COLUMN accounting.customers.current_balance IS 'Running balance of customer outstanding invoices';

