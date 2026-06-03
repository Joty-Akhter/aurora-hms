--liquibase formatted sql

--changeset easyops:303-stock-transfers context:phase3 splitStatements:false
--comment: Enhance stock transfer tables with advanced tracking

-- Add missing columns to existing stock_transfers table
DO $$
BEGIN
    -- Add priority
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'priority') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN priority VARCHAR(20) DEFAULT 'NORMAL';
    END IF;
    
    -- Add transfer_type
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'transfer_type') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN transfer_type VARCHAR(50) DEFAULT 'STANDARD';
    END IF;
    
    -- Add shipped_by
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'shipped_by') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN shipped_by UUID;
    END IF;
    
    -- Add received_by
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'received_by') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN received_by UUID;
    END IF;
    
    -- Add expected_delivery_date
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'expected_delivery_date') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN expected_delivery_date DATE;
    END IF;
    
    -- Add actual_delivery_date
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'actual_delivery_date') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN actual_delivery_date DATE;
    END IF;
    
    -- Add reason
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfers' AND column_name = 'reason') THEN
        ALTER TABLE inventory.stock_transfers ADD COLUMN reason TEXT;
    END IF;
END $$;

-- Add missing columns to existing stock_transfer_lines table
DO $$
BEGIN
    -- Add variance_quantity
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfer_lines' AND column_name = 'variance_quantity') THEN
        ALTER TABLE inventory.stock_transfer_lines ADD COLUMN variance_quantity DECIMAL(19, 4) DEFAULT 0;
    END IF;
    
    -- Add status
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfer_lines' AND column_name = 'status') THEN
        ALTER TABLE inventory.stock_transfer_lines ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING';
    END IF;
    
    -- Add variance_reason
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'stock_transfer_lines' AND column_name = 'variance_reason') THEN
        ALTER TABLE inventory.stock_transfer_lines ADD COLUMN variance_reason TEXT;
    END IF;
END $$;

-- Grants
GRANT ALL PRIVILEGES ON inventory.stock_transfers TO easyops;
GRANT ALL PRIVILEGES ON inventory.stock_transfer_lines TO easyops;

-- Triggers
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_stock_transfers_updated_at') THEN
        CREATE TRIGGER update_stock_transfers_updated_at 
        BEFORE UPDATE ON inventory.stock_transfers 
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

