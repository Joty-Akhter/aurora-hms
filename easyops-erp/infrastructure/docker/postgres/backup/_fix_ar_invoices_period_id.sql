-- Make period_id nullable in ar_invoices table
-- This allows invoices to be created without a period initially

ALTER TABLE accounting.ar_invoices 
ALTER COLUMN period_id DROP NOT NULL;

COMMENT ON COLUMN accounting.ar_invoices.period_id IS 'Accounting period ID (optional - can be assigned later or auto-determined)';

