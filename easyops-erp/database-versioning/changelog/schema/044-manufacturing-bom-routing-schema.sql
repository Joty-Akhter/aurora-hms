--liquibase formatted sql

--changeset easyops:044-create-manufacturing-bom-routing-schema splitStatements:false

-- Create Manufacturing schema
CREATE SCHEMA IF NOT EXISTS manufacturing;

-- =====================================================
-- BOM Headers Table
-- =====================================================
CREATE TABLE manufacturing.bom_headers (
    bom_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    bom_number VARCHAR(50) NOT NULL,
    
    -- Product Reference
    product_id UUID NOT NULL, -- References inventory.products
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    
    -- BOM Details
    bom_type VARCHAR(50) DEFAULT 'MANUFACTURING', -- MANUFACTURING, ENGINEERING, SALES, SERVICE, PHANTOM
    version_number INTEGER DEFAULT 1,
    revision VARCHAR(20),
    
    -- Status
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED, ACTIVE, OBSOLETE
    
    -- Effective Dates
    effective_from DATE,
    effective_to DATE,
    
    -- Quantities
    base_quantity DECIMAL(15,3) DEFAULT 1, -- Quantity this BOM produces
    uom VARCHAR(20),
    
    -- Costs (calculated)
    material_cost DECIMAL(15,2),
    labor_cost DECIMAL(15,2),
    overhead_cost DECIMAL(15,2),
    total_cost DECIMAL(15,2),
    
    -- Additional Info
    description TEXT,
    notes TEXT,
    
    -- Approval
    approved_by UUID,
    approved_date TIMESTAMP,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_bom_number UNIQUE (organization_id, bom_number),
    CONSTRAINT uk_product_bom UNIQUE (organization_id, product_id, version_number)
);

CREATE INDEX idx_bom_headers_org ON manufacturing.bom_headers(organization_id);
CREATE INDEX idx_bom_headers_product ON manufacturing.bom_headers(product_id);
CREATE INDEX idx_bom_headers_status ON manufacturing.bom_headers(status);
CREATE INDEX idx_bom_headers_type ON manufacturing.bom_headers(bom_type);

-- =====================================================
-- BOM Lines Table
-- =====================================================
CREATE TABLE manufacturing.bom_lines (
    bom_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_id UUID NOT NULL REFERENCES manufacturing.bom_headers(bom_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Component Details
    component_id UUID NOT NULL, -- References inventory.products
    component_code VARCHAR(100),
    component_name VARCHAR(255),
    
    -- Hierarchy
    parent_line_id UUID REFERENCES manufacturing.bom_lines(bom_line_id),
    level_number INTEGER DEFAULT 1, -- 1 = top level, 2 = sub-level, etc.
    sequence_number INTEGER,
    
    -- Quantity Requirements
    quantity_per_unit DECIMAL(15,4) NOT NULL,
    uom VARCHAR(20),
    
    -- Component Classification
    component_type VARCHAR(50), -- RAW_MATERIAL, SUB_ASSEMBLY, FINISHED_GOOD, PACKAGING
    
    -- Characteristics
    is_optional BOOLEAN DEFAULT false,
    is_phantom BOOLEAN DEFAULT false, -- Phantom BOM (not stocked)
    can_substitute BOOLEAN DEFAULT false,
    
    -- Planning
    scrap_percentage DECIMAL(5,2) DEFAULT 0,
    lead_time_days INTEGER,
    
    -- Costs
    unit_cost DECIMAL(15,2),
    extended_cost DECIMAL(15,2) GENERATED ALWAYS AS (quantity_per_unit * COALESCE(unit_cost, 0)) STORED,
    
    -- Additional Info
    notes TEXT,
    reference_designator VARCHAR(100), -- For electronics/PCBs
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    effective_from DATE,
    effective_to DATE,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bom_lines_bom ON manufacturing.bom_lines(bom_id);
CREATE INDEX idx_bom_lines_component ON manufacturing.bom_lines(component_id);
CREATE INDEX idx_bom_lines_parent ON manufacturing.bom_lines(parent_line_id);
CREATE INDEX idx_bom_lines_level ON manufacturing.bom_lines(level_number);
CREATE INDEX idx_bom_lines_type ON manufacturing.bom_lines(component_type);

-- =====================================================
-- BOM Versions Table
-- =====================================================
CREATE TABLE manufacturing.bom_versions (
    version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_id UUID NOT NULL REFERENCES manufacturing.bom_headers(bom_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Version Info
    version_number INTEGER NOT NULL,
    revision VARCHAR(20),
    
    -- Change Management
    change_type VARCHAR(50), -- MINOR, MAJOR, ECN, ECO
    change_reason TEXT,
    change_description TEXT,
    
    -- Approval
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    approved_by UUID,
    approved_date TIMESTAMP,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_bom_version UNIQUE (bom_id, version_number)
);

CREATE INDEX idx_bom_versions_bom ON manufacturing.bom_versions(bom_id);
CREATE INDEX idx_bom_versions_status ON manufacturing.bom_versions(status);

-- =====================================================
-- Product Routings Table
-- =====================================================
CREATE TABLE manufacturing.product_routings (
    routing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    routing_number VARCHAR(50) NOT NULL,
    
    -- Product Reference
    product_id UUID NOT NULL,
    bom_id UUID REFERENCES manufacturing.bom_headers(bom_id),
    
    -- Routing Details
    routing_name VARCHAR(255),
    operation_sequence INTEGER NOT NULL,
    operation_code VARCHAR(50),
    operation_name VARCHAR(255) NOT NULL,
    
    -- Work Center
    work_center_code VARCHAR(50),
    work_center_name VARCHAR(255),
    
    -- Time Standards (in minutes)
    setup_time DECIMAL(10,2) DEFAULT 0,
    run_time_per_unit DECIMAL(10,2) NOT NULL,
    teardown_time DECIMAL(10,2) DEFAULT 0,
    
    -- Costs
    cost_per_hour DECIMAL(10,2),
    setup_cost DECIMAL(10,2),
    
    -- Operation Details
    description TEXT,
    instructions TEXT,
    quality_check_required BOOLEAN DEFAULT false,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    effective_from DATE,
    effective_to DATE,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_routing_number UNIQUE (organization_id, routing_number),
    CONSTRAINT uk_product_routing_seq UNIQUE (product_id, operation_sequence)
);

CREATE INDEX idx_product_routings_org ON manufacturing.product_routings(organization_id);
CREATE INDEX idx_product_routings_product ON manufacturing.product_routings(product_id);
CREATE INDEX idx_product_routings_bom ON manufacturing.product_routings(bom_id);
CREATE INDEX idx_product_routings_sequence ON manufacturing.product_routings(operation_sequence);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_bom_headers_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_bom_headers_timestamp
BEFORE UPDATE ON manufacturing.bom_headers
FOR EACH ROW
EXECUTE FUNCTION update_bom_headers_timestamp();

CREATE OR REPLACE FUNCTION update_bom_lines_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_bom_lines_timestamp
BEFORE UPDATE ON manufacturing.bom_lines
FOR EACH ROW
EXECUTE FUNCTION update_bom_lines_timestamp();

CREATE OR REPLACE FUNCTION update_product_routings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_product_routings_timestamp
BEFORE UPDATE ON manufacturing.product_routings
FOR EACH ROW
EXECUTE FUNCTION update_product_routings_timestamp();

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE manufacturing.bom_headers IS 'Bill of Materials master records';
COMMENT ON TABLE manufacturing.bom_lines IS 'BOM component lines with hierarchy';
COMMENT ON TABLE manufacturing.bom_versions IS 'BOM version history and change tracking';
COMMENT ON TABLE manufacturing.product_routings IS 'Manufacturing operations/routing for products';


