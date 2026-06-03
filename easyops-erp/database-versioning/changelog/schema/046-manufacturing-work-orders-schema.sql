--liquibase formatted sql

--changeset easyops:046-create-manufacturing-work-orders-schema splitStatements:false

-- =====================================================
-- Work Orders Table
-- =====================================================
CREATE TABLE manufacturing.work_orders (
    work_order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    work_order_number VARCHAR(50) NOT NULL,
    
    -- Product & BOM Reference
    product_id UUID NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    bom_id UUID REFERENCES manufacturing.bom_headers(bom_id),
    routing_id UUID, -- Reference to first routing operation
    
    -- Order Type & Source
    order_type VARCHAR(50) DEFAULT 'PRODUCTION', -- PRODUCTION, REWORK, ASSEMBLY, DISASSEMBLY
    source_type VARCHAR(50), -- SALES_ORDER, STOCK_REPLENISHMENT, MRP, MANUAL
    source_reference VARCHAR(100), -- Reference to source document
    
    -- Quantities
    quantity_planned DECIMAL(15,3) NOT NULL,
    quantity_completed DECIMAL(15,3) DEFAULT 0,
    quantity_scrapped DECIMAL(15,3) DEFAULT 0,
    quantity_reworked DECIMAL(15,3) DEFAULT 0,
    quantity_in_process DECIMAL(15,3) DEFAULT 0,
    
    -- Status & Priority
    status VARCHAR(50) DEFAULT 'CREATED', -- CREATED, RELEASED, IN_PROGRESS, PAUSED, COMPLETED, CLOSED, CANCELLED
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    
    -- Dates
    planned_start_date TIMESTAMP,
    planned_end_date TIMESTAMP,
    actual_start_date TIMESTAMP,
    actual_end_date TIMESTAMP,
    release_date TIMESTAMP,
    
    -- Location & Warehouse
    source_warehouse_id UUID, -- Where materials come from
    target_warehouse_id UUID, -- Where finished goods go
    production_line VARCHAR(100),
    work_center_id UUID,
    
    -- Costs (to be calculated)
    material_cost DECIMAL(18,2) DEFAULT 0,
    labor_cost DECIMAL(18,2) DEFAULT 0,
    overhead_cost DECIMAL(18,2) DEFAULT 0,
    total_cost DECIMAL(18,2) DEFAULT 0,
    
    -- Progress Tracking
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    operations_completed INTEGER DEFAULT 0,
    total_operations INTEGER DEFAULT 0,
    
    -- Additional Info
    notes TEXT,
    special_instructions TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_by UUID,
    completed_by UUID,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_work_order_number UNIQUE (organization_id, work_order_number)
);

CREATE INDEX idx_work_orders_org ON manufacturing.work_orders(organization_id);
CREATE INDEX idx_work_orders_product ON manufacturing.work_orders(product_id);
CREATE INDEX idx_work_orders_bom ON manufacturing.work_orders(bom_id);
CREATE INDEX idx_work_orders_status ON manufacturing.work_orders(status);
CREATE INDEX idx_work_orders_priority ON manufacturing.work_orders(priority);
CREATE INDEX idx_work_orders_dates ON manufacturing.work_orders(planned_start_date, planned_end_date);
CREATE INDEX idx_work_orders_source ON manufacturing.work_orders(source_type, source_reference);

-- =====================================================
-- Work Order Operations Table
-- =====================================================
CREATE TABLE manufacturing.work_order_operations (
    operation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES manufacturing.work_orders(work_order_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Routing Reference
    routing_operation_id UUID REFERENCES manufacturing.product_routings(routing_id),
    operation_sequence INTEGER NOT NULL,
    operation_code VARCHAR(50),
    operation_name VARCHAR(255) NOT NULL,
    
    -- Work Center & Assignment
    work_center_id UUID,
    work_center_code VARCHAR(50),
    work_center_name VARCHAR(255),
    assigned_to UUID, -- References admin.users(id)
    assigned_date TIMESTAMP,
    
    -- Status & Progress
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, PAUSED, SKIPPED, FAILED
    
    -- Time Standards
    setup_time_planned DECIMAL(10,2), -- minutes
    run_time_planned DECIMAL(10,2), -- minutes per unit
    teardown_time_planned DECIMAL(10,2), -- minutes
    total_time_planned DECIMAL(10,2), -- total minutes
    
    -- Actual Time Tracking
    setup_time_actual DECIMAL(10,2),
    run_time_actual DECIMAL(10,2),
    teardown_time_actual DECIMAL(10,2),
    total_time_actual DECIMAL(10,2),
    
    -- Dates & Times
    planned_start TIMESTAMP,
    planned_end TIMESTAMP,
    actual_start TIMESTAMP,
    actual_end TIMESTAMP,
    
    -- Production Tracking
    quantity_completed DECIMAL(15,3) DEFAULT 0,
    quantity_rejected DECIMAL(15,3) DEFAULT 0,
    quantity_reworked DECIMAL(15,3) DEFAULT 0,
    
    -- Quality Check
    quality_check_required BOOLEAN DEFAULT false,
    quality_check_completed BOOLEAN DEFAULT false,
    quality_inspector UUID,
    quality_check_date TIMESTAMP,
    quality_result VARCHAR(50), -- PASS, FAIL, CONDITIONAL_PASS
    
    -- Cost
    labor_cost DECIMAL(10,2),
    overhead_cost DECIMAL(10,2),
    
    -- Instructions & Notes
    instructions TEXT,
    notes TEXT,
    failure_reason TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_by UUID,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_work_order_operation_seq UNIQUE (work_order_id, operation_sequence)
);

CREATE INDEX idx_work_order_operations_wo ON manufacturing.work_order_operations(work_order_id);
CREATE INDEX idx_work_order_operations_status ON manufacturing.work_order_operations(status);
CREATE INDEX idx_work_order_operations_wc ON manufacturing.work_order_operations(work_center_id);
CREATE INDEX idx_work_order_operations_assigned ON manufacturing.work_order_operations(assigned_to);
CREATE INDEX idx_work_order_operations_dates ON manufacturing.work_order_operations(planned_start, planned_end);

-- =====================================================
-- Work Order Materials Table
-- =====================================================
CREATE TABLE manufacturing.work_order_materials (
    material_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES manufacturing.work_orders(work_order_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Material Reference
    component_id UUID NOT NULL, -- References inventory.products
    component_code VARCHAR(100),
    component_name VARCHAR(255),
    
    -- BOM Reference
    bom_line_id UUID REFERENCES manufacturing.bom_lines(bom_line_id),
    
    -- Operation Assignment
    operation_id UUID REFERENCES manufacturing.work_order_operations(operation_id),
    operation_sequence INTEGER,
    
    -- Quantities
    quantity_required DECIMAL(15,4) NOT NULL,
    quantity_reserved DECIMAL(15,4) DEFAULT 0,
    quantity_issued DECIMAL(15,4) DEFAULT 0,
    quantity_consumed DECIMAL(15,4) DEFAULT 0,
    quantity_returned DECIMAL(15,4) DEFAULT 0,
    quantity_scrapped DECIMAL(15,4) DEFAULT 0,
    
    -- Unit of Measure
    uom VARCHAR(20),
    
    -- Status
    status VARCHAR(50) DEFAULT 'PLANNED', -- PLANNED, RESERVED, ISSUED, CONSUMED, RETURNED
    
    -- Location
    warehouse_id UUID,
    location VARCHAR(100),
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    lot_number VARCHAR(100),
    
    -- Costs
    unit_cost DECIMAL(15,2),
    total_cost DECIMAL(15,2),
    
    -- Dates
    required_date TIMESTAMP,
    reserved_date TIMESTAMP,
    issued_date TIMESTAMP,
    consumed_date TIMESTAMP,
    
    -- Backflush
    backflush BOOLEAN DEFAULT false, -- Auto-consume when WO completes
    
    -- Additional Info
    notes TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    issued_by UUID,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_work_order_materials_wo ON manufacturing.work_order_materials(work_order_id);
CREATE INDEX idx_work_order_materials_component ON manufacturing.work_order_materials(component_id);
CREATE INDEX idx_work_order_materials_operation ON manufacturing.work_order_materials(operation_id);
CREATE INDEX idx_work_order_materials_status ON manufacturing.work_order_materials(status);
CREATE INDEX idx_work_order_materials_warehouse ON manufacturing.work_order_materials(warehouse_id);
CREATE INDEX idx_work_order_materials_batch ON manufacturing.work_order_materials(batch_number);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_work_orders_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_work_orders_timestamp
BEFORE UPDATE ON manufacturing.work_orders
FOR EACH ROW
EXECUTE FUNCTION update_work_orders_timestamp();

CREATE OR REPLACE FUNCTION update_work_order_operations_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_work_order_operations_timestamp
BEFORE UPDATE ON manufacturing.work_order_operations
FOR EACH ROW
EXECUTE FUNCTION update_work_order_operations_timestamp();

CREATE OR REPLACE FUNCTION update_work_order_materials_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_work_order_materials_timestamp
BEFORE UPDATE ON manufacturing.work_order_materials
FOR EACH ROW
EXECUTE FUNCTION update_work_order_materials_timestamp();

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE manufacturing.work_orders IS 'Production work orders for manufacturing';
COMMENT ON TABLE manufacturing.work_order_operations IS 'Operations/steps in work orders';
COMMENT ON TABLE manufacturing.work_order_materials IS 'Material requirements and issuance for work orders';


