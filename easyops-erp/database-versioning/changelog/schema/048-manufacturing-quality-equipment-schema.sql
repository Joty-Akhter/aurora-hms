--liquibase formatted sql

--changeset easyops:048-create-manufacturing-quality-equipment-schema splitStatements:false

-- =====================================================
-- Quality Inspections Table
-- =====================================================
CREATE TABLE manufacturing.quality_inspections (
    inspection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    inspection_number VARCHAR(50) NOT NULL,
    
    -- References
    work_order_id UUID REFERENCES manufacturing.work_orders(work_order_id),
    operation_id UUID REFERENCES manufacturing.work_order_operations(operation_id),
    product_id UUID NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    
    -- Inspection Type
    inspection_type VARCHAR(50) NOT NULL, -- RECEIVING, IN_PROCESS, FINAL, AUDIT, FIRST_ARTICLE
    inspection_stage VARCHAR(50), -- PRE_PRODUCTION, DURING_PRODUCTION, POST_PRODUCTION
    
    -- Inspection Details
    inspection_date TIMESTAMP NOT NULL,
    inspector_id UUID, -- References admin.users(id)
    inspector_name VARCHAR(255),
    
    -- Status & Results
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED
    overall_result VARCHAR(50), -- PASS, FAIL, CONDITIONAL_PASS, PENDING_REVIEW
    
    -- Sample Information
    lot_number VARCHAR(100),
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    sample_size INTEGER,
    sample_method VARCHAR(50), -- RANDOM, SEQUENTIAL, STATISTICAL
    
    -- Quantities
    quantity_inspected DECIMAL(15,3),
    quantity_passed DECIMAL(15,3),
    quantity_failed DECIMAL(15,3),
    quantity_reworked DECIMAL(15,3),
    
    -- Defect Analysis
    defects_found INTEGER DEFAULT 0,
    critical_defects INTEGER DEFAULT 0,
    major_defects INTEGER DEFAULT 0,
    minor_defects INTEGER DEFAULT 0,
    
    -- Quality Metrics
    pass_rate DECIMAL(5,2), -- Percentage
    defect_rate DECIMAL(5,2), -- Defects per unit
    
    -- Documentation
    inspection_criteria TEXT,
    notes TEXT,
    corrective_actions TEXT,
    attachments TEXT[], -- Array of file URLs
    
    -- Approval
    approved_by UUID,
    approved_date TIMESTAMP,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_inspection_number UNIQUE (organization_id, inspection_number)
);

CREATE INDEX idx_quality_inspections_org ON manufacturing.quality_inspections(organization_id);
CREATE INDEX idx_quality_inspections_wo ON manufacturing.quality_inspections(work_order_id);
CREATE INDEX idx_quality_inspections_product ON manufacturing.quality_inspections(product_id);
CREATE INDEX idx_quality_inspections_type ON manufacturing.quality_inspections(inspection_type);
CREATE INDEX idx_quality_inspections_status ON manufacturing.quality_inspections(status);
CREATE INDEX idx_quality_inspections_result ON manufacturing.quality_inspections(overall_result);
CREATE INDEX idx_quality_inspections_date ON manufacturing.quality_inspections(inspection_date);

-- =====================================================
-- Quality Inspection Items Table
-- =====================================================
CREATE TABLE manufacturing.quality_inspection_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES manufacturing.quality_inspections(inspection_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Parameter Details
    sequence_number INTEGER,
    parameter_code VARCHAR(50),
    parameter_name VARCHAR(255) NOT NULL,
    parameter_type VARCHAR(50), -- DIMENSION, VISUAL, FUNCTIONAL, CHEMICAL, MECHANICAL
    
    -- Specification
    specification_min DECIMAL(15,4),
    specification_max DECIMAL(15,4),
    specification_target DECIMAL(15,4),
    specification_unit VARCHAR(20),
    specification_text TEXT, -- For non-numeric specs
    
    -- Measured Values
    measured_value DECIMAL(15,4),
    measured_text VARCHAR(255), -- For text-based results
    
    -- Pass/Fail
    pass_fail VARCHAR(10), -- PASS, FAIL, NA
    is_critical BOOLEAN DEFAULT false,
    deviation DECIMAL(15,4), -- Difference from target
    deviation_percentage DECIMAL(5,2),
    
    -- Notes
    notes TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quality_inspection_items_inspection ON manufacturing.quality_inspection_items(inspection_id);
CREATE INDEX idx_quality_inspection_items_pass_fail ON manufacturing.quality_inspection_items(pass_fail);

-- =====================================================
-- Non-Conformances Table
-- =====================================================
CREATE TABLE manufacturing.non_conformances (
    nc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    nc_number VARCHAR(50) NOT NULL,
    
    -- References
    work_order_id UUID REFERENCES manufacturing.work_orders(work_order_id),
    operation_id UUID REFERENCES manufacturing.work_order_operations(operation_id),
    inspection_id UUID REFERENCES manufacturing.quality_inspections(inspection_id),
    product_id UUID,
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    
    -- NC Type & Severity
    nc_type VARCHAR(50) NOT NULL, -- DEFECT, DEVIATION, NON_COMPLIANCE, PROCESS_FAILURE
    severity VARCHAR(50) DEFAULT 'MINOR', -- CRITICAL, MAJOR, MINOR
    category VARCHAR(50), -- MATERIAL, PROCESS, EQUIPMENT, HUMAN_ERROR, DESIGN
    
    -- Status
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, IN_REVIEW, REWORK, SCRAP, APPROVED, CLOSED
    
    -- Description
    description TEXT NOT NULL,
    location VARCHAR(255),
    
    -- Quantities
    quantity_affected DECIMAL(15,3),
    quantity_reworked DECIMAL(15,3),
    quantity_scrapped DECIMAL(15,3),
    
    -- Root Cause Analysis
    root_cause VARCHAR(50), -- MATERIAL_DEFECT, PROCESS_ERROR, EQUIPMENT_FAILURE, OPERATOR_ERROR, DESIGN_ISSUE
    root_cause_description TEXT,
    
    -- Corrective Actions
    immediate_action TEXT,
    corrective_action TEXT,
    preventive_action TEXT,
    
    -- Responsibility
    reported_by UUID, -- References admin.users(id)
    reported_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_to UUID,
    
    -- Resolution
    disposition VARCHAR(50), -- USE_AS_IS, REWORK, SCRAP, RETURN_TO_VENDOR, MRB_REVIEW
    resolution_notes TEXT,
    resolved_by UUID,
    resolved_date TIMESTAMP,
    
    -- Verification
    verified_by UUID,
    verified_date TIMESTAMP,
    effectiveness_check BOOLEAN,
    
    -- Cost Impact
    cost_impact DECIMAL(15,2),
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_nc_number UNIQUE (organization_id, nc_number)
);

CREATE INDEX idx_non_conformances_org ON manufacturing.non_conformances(organization_id);
CREATE INDEX idx_non_conformances_wo ON manufacturing.non_conformances(work_order_id);
CREATE INDEX idx_non_conformances_status ON manufacturing.non_conformances(status);
CREATE INDEX idx_non_conformances_severity ON manufacturing.non_conformances(severity);
CREATE INDEX idx_non_conformances_type ON manufacturing.non_conformances(nc_type);
CREATE INDEX idx_non_conformances_date ON manufacturing.non_conformances(reported_date);

-- =====================================================
-- Work Centers Table
-- =====================================================
CREATE TABLE manufacturing.work_centers (
    work_center_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    work_center_code VARCHAR(50) NOT NULL,
    work_center_name VARCHAR(255) NOT NULL,
    
    -- Type & Classification
    work_center_type VARCHAR(50), -- MACHINE, ASSEMBLY_LINE, WORK_STATION, TESTING, PACKAGING
    category VARCHAR(50), -- PRODUCTION, QUALITY, MAINTENANCE, SUPPORT
    
    -- Location
    location VARCHAR(255),
    department VARCHAR(100),
    
    -- Capacity
    capacity_per_hour DECIMAL(10,2),
    capacity_uom VARCHAR(20),
    number_of_machines INTEGER DEFAULT 1,
    number_of_operators INTEGER,
    
    -- Operating Hours
    shifts_per_day INTEGER DEFAULT 1,
    hours_per_shift DECIMAL(5,2) DEFAULT 8,
    working_days_per_week INTEGER DEFAULT 5,
    
    -- Efficiency & Performance
    efficiency_percentage DECIMAL(5,2) DEFAULT 100, -- Target efficiency
    utilization_percentage DECIMAL(5,2), -- Actual utilization
    oee_target DECIMAL(5,2), -- OEE target percentage
    
    -- Status
    status VARCHAR(50) DEFAULT 'AVAILABLE', -- AVAILABLE, IN_USE, MAINTENANCE, DOWN, INACTIVE
    is_active BOOLEAN DEFAULT true,
    
    -- Costs
    cost_per_hour DECIMAL(10,2),
    setup_cost DECIMAL(10,2),
    overhead_rate DECIMAL(5,2), -- Percentage
    
    -- Maintenance
    last_maintenance_date TIMESTAMP,
    next_maintenance_date TIMESTAMP,
    maintenance_frequency_days INTEGER,
    
    -- Additional Info
    description TEXT,
    notes TEXT,
    specifications TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_work_center_code UNIQUE (organization_id, work_center_code)
);

CREATE INDEX idx_work_centers_org ON manufacturing.work_centers(organization_id);
CREATE INDEX idx_work_centers_code ON manufacturing.work_centers(work_center_code);
CREATE INDEX idx_work_centers_type ON manufacturing.work_centers(work_center_type);
CREATE INDEX idx_work_centers_status ON manufacturing.work_centers(status);

-- =====================================================
-- Equipment Maintenance Table
-- =====================================================
CREATE TABLE manufacturing.equipment_maintenance (
    maintenance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    maintenance_number VARCHAR(50) NOT NULL,
    
    -- Work Center Reference
    work_center_id UUID NOT NULL REFERENCES manufacturing.work_centers(work_center_id),
    
    -- Maintenance Type
    maintenance_type VARCHAR(50) NOT NULL, -- PREVENTIVE, CORRECTIVE, PREDICTIVE, BREAKDOWN, CALIBRATION
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    
    -- Status
    status VARCHAR(50) DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE
    
    -- Schedule
    scheduled_date TIMESTAMP NOT NULL,
    scheduled_duration_hours DECIMAL(5,2),
    
    -- Actual
    actual_start_date TIMESTAMP,
    actual_end_date TIMESTAMP,
    actual_duration_hours DECIMAL(5,2),
    
    -- Work Details
    description TEXT NOT NULL,
    work_performed TEXT,
    parts_replaced TEXT,
    findings TEXT,
    recommendations TEXT,
    
    -- Assignment
    assigned_to UUID, -- References admin.users(id)
    technician_id UUID,
    technician_name VARCHAR(255),
    
    -- Cost
    labor_cost DECIMAL(10,2),
    parts_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    
    -- Downtime Impact
    downtime_hours DECIMAL(10,2),
    production_loss DECIMAL(15,2), -- Estimated production loss
    
    -- Follow-up
    follow_up_required BOOLEAN DEFAULT false,
    follow_up_date TIMESTAMP,
    follow_up_notes TEXT,
    
    -- Completion
    completed_by UUID,
    completion_notes TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_maintenance_number UNIQUE (organization_id, maintenance_number)
);

CREATE INDEX idx_equipment_maintenance_org ON manufacturing.equipment_maintenance(organization_id);
CREATE INDEX idx_equipment_maintenance_wc ON manufacturing.equipment_maintenance(work_center_id);
CREATE INDEX idx_equipment_maintenance_type ON manufacturing.equipment_maintenance(maintenance_type);
CREATE INDEX idx_equipment_maintenance_status ON manufacturing.equipment_maintenance(status);
CREATE INDEX idx_equipment_maintenance_date ON manufacturing.equipment_maintenance(scheduled_date);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_quality_inspections_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_quality_inspections_timestamp
BEFORE UPDATE ON manufacturing.quality_inspections
FOR EACH ROW
EXECUTE FUNCTION update_quality_inspections_timestamp();

CREATE OR REPLACE FUNCTION update_quality_inspection_items_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_quality_inspection_items_timestamp
BEFORE UPDATE ON manufacturing.quality_inspection_items
FOR EACH ROW
EXECUTE FUNCTION update_quality_inspection_items_timestamp();

CREATE OR REPLACE FUNCTION update_non_conformances_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_non_conformances_timestamp
BEFORE UPDATE ON manufacturing.non_conformances
FOR EACH ROW
EXECUTE FUNCTION update_non_conformances_timestamp();

CREATE OR REPLACE FUNCTION update_work_centers_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_work_centers_timestamp
BEFORE UPDATE ON manufacturing.work_centers
FOR EACH ROW
EXECUTE FUNCTION update_work_centers_timestamp();

CREATE OR REPLACE FUNCTION update_equipment_maintenance_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_equipment_maintenance_timestamp
BEFORE UPDATE ON manufacturing.equipment_maintenance
FOR EACH ROW
EXECUTE FUNCTION update_equipment_maintenance_timestamp();

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE manufacturing.quality_inspections IS 'Quality inspection records';
COMMENT ON TABLE manufacturing.quality_inspection_items IS 'Individual inspection parameters and measurements';
COMMENT ON TABLE manufacturing.non_conformances IS 'Non-conformance and defect tracking';
COMMENT ON TABLE manufacturing.work_centers IS 'Manufacturing work center master data';
COMMENT ON TABLE manufacturing.equipment_maintenance IS 'Equipment maintenance records';


