--liquibase formatted sql

--changeset easyops:305-add-warehouse-id-to-areas context:pharma
--comment: Add warehouse_id column to pharma.areas table to link each area with its corresponding warehouse in inventory system

-- First, ensure pharma schema exists
CREATE SCHEMA IF NOT EXISTS pharma;

-- Add warehouse_id column to areas table
ALTER TABLE pharma.areas
ADD COLUMN IF NOT EXISTS warehouse_id UUID;

-- Add index for warehouse_id for better query performance
CREATE INDEX IF NOT EXISTS idx_areas_warehouse_id ON pharma.areas(warehouse_id);

-- Add comment to column
COMMENT ON COLUMN pharma.areas.warehouse_id IS 'Reference to inventory.warehouses.id - Auto-created when area is created';

