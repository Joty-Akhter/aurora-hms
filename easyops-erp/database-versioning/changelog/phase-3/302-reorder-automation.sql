--liquibase formatted sql

--changeset easyops:302-reorder-automation context:phase3 splitStatements:false
--comment: Enhance reorder rules and create reorder alerts

-- Add missing columns to existing reorder_rules table
DO $$
BEGIN
    -- Rename reorder_level to reorder_point for consistency
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'reorder_level') THEN
        ALTER TABLE inventory.reorder_rules RENAME COLUMN reorder_level TO reorder_point;
    END IF;
    
    -- Add safety_stock
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'safety_stock') THEN
        ALTER TABLE inventory.reorder_rules ADD COLUMN safety_stock DECIMAL(19, 4) DEFAULT 0;
    END IF;
    
    -- Add preferred_supplier_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'preferred_supplier_id') THEN
        ALTER TABLE inventory.reorder_rules ADD COLUMN preferred_supplier_id UUID;
    END IF;
    
    -- Rename supplier_id to preferred_supplier_id if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'supplier_id') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'preferred_supplier_id') THEN
            ALTER TABLE inventory.reorder_rules RENAME COLUMN supplier_id TO preferred_supplier_id;
        END IF;
    END IF;
    
    -- Add last_triggered_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'last_triggered_at') THEN
        ALTER TABLE inventory.reorder_rules ADD COLUMN last_triggered_at TIMESTAMP WITH TIME ZONE;
    END IF;
    
    -- Add trigger_count
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'trigger_count') THEN
        ALTER TABLE inventory.reorder_rules ADD COLUMN trigger_count INTEGER DEFAULT 0;
    END IF;
    
    -- Add notes
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'reorder_rules' AND column_name = 'notes') THEN
        ALTER TABLE inventory.reorder_rules ADD COLUMN notes TEXT;
    END IF;
END $$;

-- Reorder Alerts Table
CREATE TABLE IF NOT EXISTS inventory.reorder_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    reorder_rule_id UUID NOT NULL REFERENCES inventory.reorder_rules(id),
    current_quantity DECIMAL(19, 4),
    reorder_point DECIMAL(19, 4),
    suggested_order_qty DECIMAL(19, 4),
    alert_status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, PO_CREATED, CLOSED
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    suggested_po_id UUID,
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    notification_sent BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reorder_alerts_org ON inventory.reorder_alerts(organization_id);
CREATE INDEX IF NOT EXISTS idx_reorder_alerts_product ON inventory.reorder_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_reorder_alerts_status ON inventory.reorder_alerts(alert_status);
CREATE INDEX IF NOT EXISTS idx_reorder_alerts_priority ON inventory.reorder_alerts(priority);
CREATE INDEX IF NOT EXISTS idx_reorder_alerts_open ON inventory.reorder_alerts(alert_status) WHERE alert_status = 'OPEN';

-- Grants
GRANT ALL PRIVILEGES ON inventory.reorder_rules TO easyops;
GRANT ALL PRIVILEGES ON inventory.reorder_alerts TO easyops;

-- Trigger for updated_at
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_reorder_rules_updated_at') THEN
        CREATE TRIGGER update_reorder_rules_updated_at 
        BEFORE UPDATE ON inventory.reorder_rules 
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

