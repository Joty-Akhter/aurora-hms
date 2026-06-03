--liquibase formatted sql

--changeset easyops:301-create-inventory-schema context:inventory
--comment: Create inventory schema
CREATE SCHEMA IF NOT EXISTS inventory;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA inventory TO easyops;

--changeset easyops:302-create-product-categories-table context:inventory
--comment: Create product categories table
CREATE TABLE inventory.product_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_category_id UUID REFERENCES inventory.product_categories(id) ON DELETE SET NULL,
    description TEXT,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    UNIQUE(organization_id, code)
);

CREATE INDEX idx_product_categories_org ON inventory.product_categories(organization_id);
CREATE INDEX idx_product_categories_parent ON inventory.product_categories(parent_category_id);
CREATE INDEX idx_product_categories_active ON inventory.product_categories(is_active);

--changeset easyops:303-create-product-master-table context:inventory
--comment: Create comprehensive product master table
CREATE TABLE inventory.products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    
    -- Basic Information
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    category_id UUID REFERENCES inventory.product_categories(id),
    brand VARCHAR(100),
    manufacturer VARCHAR(100),
    model_number VARCHAR(100),
    
    -- Product Type
    product_type VARCHAR(50) DEFAULT 'GOODS', -- GOODS, SERVICE, BUNDLE
    item_type VARCHAR(50) DEFAULT 'STOCKABLE', -- STOCKABLE, NON_STOCKABLE, SERVICE
    
    -- Pricing
    cost_price DECIMAL(19, 4) DEFAULT 0,
    selling_price DECIMAL(19, 4) DEFAULT 0,
    wholesale_price DECIMAL(19, 4),
    retail_price DECIMAL(19, 4),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Stock Information
    uom VARCHAR(50) DEFAULT 'PCS', -- Unit of Measure (PCS, KG, LTR, etc)
    reorder_level DECIMAL(19, 4) DEFAULT 0,
    min_stock_level DECIMAL(19, 4) DEFAULT 0,
    max_stock_level DECIMAL(19, 4),
    safety_stock DECIMAL(19, 4) DEFAULT 0,
    
    -- Physical Attributes
    weight DECIMAL(10, 4),
    weight_unit VARCHAR(10) DEFAULT 'KG',
    length DECIMAL(10, 4),
    width DECIMAL(10, 4),
    height DECIMAL(10, 4),
    dimension_unit VARCHAR(10) DEFAULT 'CM',
    volume DECIMAL(10, 4),
    volume_unit VARCHAR(10) DEFAULT 'M3',
    
    -- Supplier Information
    primary_supplier_id UUID,
    supplier_sku VARCHAR(100),
    lead_time_days INTEGER DEFAULT 0,
    
    -- Accounting Integration
    inventory_gl_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    cogs_gl_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    revenue_gl_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    
    -- Tax & Compliance
    tax_category VARCHAR(50),
    tax_rate DECIMAL(5, 2) DEFAULT 0,
    hs_code VARCHAR(50), -- Harmonized System Code
    country_of_origin VARCHAR(2),
    
    -- Tracking
    track_inventory BOOLEAN DEFAULT true,
    track_batch BOOLEAN DEFAULT false,
    track_serial BOOLEAN DEFAULT false,
    has_expiry BOOLEAN DEFAULT false,
    shelf_life_days INTEGER,
    
    -- Status & Images
    status VARCHAR(50) DEFAULT 'ACTIVE',
    image_url VARCHAR(500),
    images JSONB, -- Array of image URLs
    is_active BOOLEAN DEFAULT true,
    
    -- Metadata
    attributes JSONB, -- Custom attributes
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    UNIQUE(organization_id, sku)
);

CREATE INDEX idx_products_org ON inventory.products(organization_id);
CREATE INDEX idx_products_sku ON inventory.products(sku);
CREATE INDEX idx_products_barcode ON inventory.products(barcode);
CREATE INDEX idx_products_category ON inventory.products(category_id);
CREATE INDEX idx_products_active ON inventory.products(is_active);
CREATE INDEX idx_products_type ON inventory.products(product_type);

--changeset easyops:304-create-warehouses-table context:inventory
--comment: Create warehouses table
CREATE TABLE inventory.warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    location_id UUID REFERENCES admin.locations(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    warehouse_type VARCHAR(50) DEFAULT 'MAIN', -- MAIN, DISTRIBUTION, TRANSIT, VIRTUAL
    
    -- Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    
    -- Contact
    phone VARCHAR(50),
    email VARCHAR(255),
    manager_id UUID,
    
    -- Capacity
    total_capacity DECIMAL(19, 4),
    capacity_unit VARCHAR(20) DEFAULT 'M3',
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    status VARCHAR(50) DEFAULT 'OPERATIONAL',
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    UNIQUE(organization_id, code)
);

CREATE INDEX idx_warehouses_org ON inventory.warehouses(organization_id);
CREATE INDEX idx_warehouses_location ON inventory.warehouses(location_id);
CREATE INDEX idx_warehouses_active ON inventory.warehouses(is_active);

--changeset easyops:305-create-stock-locations-table context:inventory
--comment: Create stock locations/bins within warehouses
CREATE TABLE inventory.stock_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    location_type VARCHAR(50) DEFAULT 'BIN', -- ZONE, AISLE, RACK, BIN, SHELF
    parent_location_id UUID REFERENCES inventory.stock_locations(id),
    
    -- Position
    aisle VARCHAR(20),
    rack VARCHAR(20),
    shelf VARCHAR(20),
    bin VARCHAR(20),
    
    -- Capacity
    capacity DECIMAL(19, 4),
    capacity_unit VARCHAR(20),
    
    -- Restrictions
    temperature_controlled BOOLEAN DEFAULT false,
    min_temperature DECIMAL(5, 2),
    max_temperature DECIMAL(5, 2),
    
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(warehouse_id, code)
);

CREATE INDEX idx_stock_locations_warehouse ON inventory.stock_locations(warehouse_id);
CREATE INDEX idx_stock_locations_parent ON inventory.stock_locations(parent_location_id);

--changeset easyops:306-create-stock-table context:inventory
--comment: Create stock levels table
CREATE TABLE inventory.stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES inventory.products(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id) ON DELETE CASCADE,
    location_id UUID REFERENCES inventory.stock_locations(id),
    
    -- Quantities
    quantity_on_hand DECIMAL(19, 4) DEFAULT 0,
    quantity_allocated DECIMAL(19, 4) DEFAULT 0, -- Reserved for sales orders
    quantity_available DECIMAL(19, 4) DEFAULT 0, -- On hand - allocated
    quantity_on_order DECIMAL(19, 4) DEFAULT 0, -- From purchase orders
    quantity_in_transit DECIMAL(19, 4) DEFAULT 0,
    
    -- Batch/Lot/Serial
    batch_number VARCHAR(100),
    lot_number VARCHAR(100),
    serial_number VARCHAR(100),
    
    -- Expiry
    manufacture_date DATE,
    expiry_date DATE,
    
    -- Costing
    unit_cost DECIMAL(19, 4) DEFAULT 0,
    total_cost DECIMAL(19, 4) DEFAULT 0,
    valuation_method VARCHAR(20) DEFAULT 'FIFO', -- FIFO, LIFO, AVG
    
    -- Status
    status VARCHAR(50) DEFAULT 'AVAILABLE', -- AVAILABLE, QUARANTINE, DAMAGED, EXPIRED
    
    last_counted_at TIMESTAMP WITH TIME ZONE,
    last_movement_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(product_id, warehouse_id, location_id, batch_number, lot_number, serial_number)
);

CREATE INDEX idx_stock_org ON inventory.stock(organization_id);
CREATE INDEX idx_stock_product ON inventory.stock(product_id);
CREATE INDEX idx_stock_warehouse ON inventory.stock(warehouse_id);
CREATE INDEX idx_stock_location ON inventory.stock(location_id);
CREATE INDEX idx_stock_batch ON inventory.stock(batch_number);
CREATE INDEX idx_stock_serial ON inventory.stock(serial_number);
CREATE INDEX idx_stock_expiry ON inventory.stock(expiry_date);

--changeset easyops:307-create-stock-movements-table context:inventory
--comment: Create stock movements/transactions table
CREATE TABLE inventory.stock_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    
    -- Transaction Details
    transaction_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- RECEIPT, ISSUE, ADJUSTMENT, TRANSFER_OUT, TRANSFER_IN, RETURN
    
    -- Product & Location
    product_id UUID NOT NULL REFERENCES inventory.products(id),
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    location_id UUID REFERENCES inventory.stock_locations(id),
    
    -- Quantities
    quantity DECIMAL(19, 4) NOT NULL,
    uom VARCHAR(50),
    
    -- Batch/Serial
    batch_number VARCHAR(100),
    lot_number VARCHAR(100),
    serial_number VARCHAR(100),
    
    -- Cost
    unit_cost DECIMAL(19, 4) DEFAULT 0,
    total_cost DECIMAL(19, 4) DEFAULT 0,
    
    -- Source References
    source_type VARCHAR(50), -- PURCHASE_ORDER, SALES_ORDER, ADJUSTMENT, TRANSFER, MANUAL
    source_id UUID,
    source_document_number VARCHAR(100),
    
    -- Transfer specific (if TRANSFER_OUT or TRANSFER_IN)
    from_warehouse_id UUID REFERENCES inventory.warehouses(id),
    from_location_id UUID REFERENCES inventory.stock_locations(id),
    to_warehouse_id UUID REFERENCES inventory.warehouses(id),
    to_location_id UUID REFERENCES inventory.stock_locations(id),
    transfer_id UUID, -- Links IN and OUT movements
    
    -- Adjustment specific
    adjustment_reason VARCHAR(255),
    adjustment_type VARCHAR(50), -- INCREASE, DECREASE, DAMAGE, LOSS, FOUND
    
    -- GL Integration
    gl_journal_id UUID,
    
    -- Approval
    requires_approval BOOLEAN DEFAULT false,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_notes TEXT,
    
    -- Status
    status VARCHAR(50) DEFAULT 'COMPLETED', -- PENDING, COMPLETED, CANCELLED
    
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

CREATE INDEX idx_stock_movements_org ON inventory.stock_movements(organization_id);
CREATE INDEX idx_stock_movements_product ON inventory.stock_movements(product_id);
CREATE INDEX idx_stock_movements_warehouse ON inventory.stock_movements(warehouse_id);
CREATE INDEX idx_stock_movements_date ON inventory.stock_movements(transaction_date);
CREATE INDEX idx_stock_movements_type ON inventory.stock_movements(transaction_type);
CREATE INDEX idx_stock_movements_source ON inventory.stock_movements(source_type, source_id);
CREATE INDEX idx_stock_movements_transfer ON inventory.stock_movements(transfer_id);

--changeset easyops:308-create-stock-transfers-table context:inventory
--comment: Create stock transfer requests table
CREATE TABLE inventory.stock_transfers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    
    transfer_number VARCHAR(50) UNIQUE NOT NULL,
    transfer_date DATE NOT NULL,
    
    from_warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    to_warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, PENDING_APPROVAL, APPROVED, IN_TRANSIT, COMPLETED, CANCELLED
    
    requested_by UUID,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    shipped_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE,
    
    shipping_method VARCHAR(100),
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_stock_transfers_org ON inventory.stock_transfers(organization_id);
CREATE INDEX idx_stock_transfers_from ON inventory.stock_transfers(from_warehouse_id);
CREATE INDEX idx_stock_transfers_to ON inventory.stock_transfers(to_warehouse_id);
CREATE INDEX idx_stock_transfers_status ON inventory.stock_transfers(status);
CREATE INDEX idx_stock_transfers_date ON inventory.stock_transfers(transfer_date);

--changeset easyops:309-create-stock-transfer-lines-table context:inventory
--comment: Create stock transfer line items table
CREATE TABLE inventory.stock_transfer_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_id UUID NOT NULL REFERENCES inventory.stock_transfers(id) ON DELETE CASCADE,
    
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES inventory.products(id),
    
    requested_quantity DECIMAL(19, 4) NOT NULL,
    shipped_quantity DECIMAL(19, 4) DEFAULT 0,
    received_quantity DECIMAL(19, 4) DEFAULT 0,
    
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    
    from_location_id UUID REFERENCES inventory.stock_locations(id),
    to_location_id UUID REFERENCES inventory.stock_locations(id),
    
    unit_cost DECIMAL(19, 4) DEFAULT 0,
    notes TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(transfer_id, line_number)
);

CREATE INDEX idx_transfer_lines_transfer ON inventory.stock_transfer_lines(transfer_id);
CREATE INDEX idx_transfer_lines_product ON inventory.stock_transfer_lines(product_id);

--changeset easyops:310-create-stock-adjustments-table context:inventory
--comment: Create stock adjustments table
CREATE TABLE inventory.stock_adjustments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    
    adjustment_number VARCHAR(50) UNIQUE NOT NULL,
    adjustment_date DATE NOT NULL,
    
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    
    adjustment_type VARCHAR(50) NOT NULL, -- INCREASE, DECREASE, RECOUNT, DAMAGE, LOSS, FOUND
    reason VARCHAR(255) NOT NULL,
    
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED, POSTED, CANCELLED
    
    requires_approval BOOLEAN DEFAULT true,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    
    total_value DECIMAL(19, 4) DEFAULT 0,
    
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_stock_adjustments_org ON inventory.stock_adjustments(organization_id);
CREATE INDEX idx_stock_adjustments_warehouse ON inventory.stock_adjustments(warehouse_id);
CREATE INDEX idx_stock_adjustments_status ON inventory.stock_adjustments(status);
CREATE INDEX idx_stock_adjustments_date ON inventory.stock_adjustments(adjustment_date);

--changeset easyops:311-create-stock-adjustment-lines-table context:inventory
--comment: Create stock adjustment line items table
CREATE TABLE inventory.stock_adjustment_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    adjustment_id UUID NOT NULL REFERENCES inventory.stock_adjustments(id) ON DELETE CASCADE,
    
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES inventory.products(id),
    
    location_id UUID REFERENCES inventory.stock_locations(id),
    
    previous_quantity DECIMAL(19, 4) DEFAULT 0,
    new_quantity DECIMAL(19, 4) NOT NULL,
    adjustment_quantity DECIMAL(19, 4) NOT NULL, -- Can be negative
    
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    
    unit_cost DECIMAL(19, 4) DEFAULT 0,
    total_cost DECIMAL(19, 4) DEFAULT 0,
    
    reason VARCHAR(255),
    notes TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(adjustment_id, line_number)
);

CREATE INDEX idx_adjustment_lines_adjustment ON inventory.stock_adjustment_lines(adjustment_id);
CREATE INDEX idx_adjustment_lines_product ON inventory.stock_adjustment_lines(product_id);

--changeset easyops:312-create-batch-lots-table context:inventory
--comment: Create batch/lot tracking table
CREATE TABLE inventory.batch_lots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES inventory.products(id) ON DELETE CASCADE,
    
    batch_number VARCHAR(100) NOT NULL,
    lot_number VARCHAR(100),
    
    manufacture_date DATE,
    expiry_date DATE,
    
    supplier_id UUID,
    po_number VARCHAR(50),
    receipt_date DATE,
    
    initial_quantity DECIMAL(19, 4) DEFAULT 0,
    current_quantity DECIMAL(19, 4) DEFAULT 0,
    
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, QUARANTINE, EXPIRED, DEPLETED
    
    quality_certificate VARCHAR(500),
    notes TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(organization_id, product_id, batch_number)
);

CREATE INDEX idx_batch_lots_org ON inventory.batch_lots(organization_id);
CREATE INDEX idx_batch_lots_product ON inventory.batch_lots(product_id);
CREATE INDEX idx_batch_lots_batch ON inventory.batch_lots(batch_number);
CREATE INDEX idx_batch_lots_expiry ON inventory.batch_lots(expiry_date);

--changeset easyops:313-create-serial-numbers-table context:inventory
--comment: Create serial number tracking table
CREATE TABLE inventory.serial_numbers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES inventory.products(id) ON DELETE CASCADE,
    
    serial_number VARCHAR(100) NOT NULL,
    
    warehouse_id UUID REFERENCES inventory.warehouses(id),
    location_id UUID REFERENCES inventory.stock_locations(id),
    
    batch_number VARCHAR(100),
    
    purchase_date DATE,
    warranty_start_date DATE,
    warranty_end_date DATE,
    warranty_period_months INTEGER,
    
    status VARCHAR(50) DEFAULT 'IN_STOCK', -- IN_STOCK, ALLOCATED, SOLD, IN_SERVICE, RETURNED, SCRAPPED
    
    customer_id UUID, -- If sold
    sale_date DATE,
    sale_invoice_number VARCHAR(50),
    
    notes TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(organization_id, product_id, serial_number)
);

CREATE INDEX idx_serial_numbers_org ON inventory.serial_numbers(organization_id);
CREATE INDEX idx_serial_numbers_product ON inventory.serial_numbers(product_id);
CREATE INDEX idx_serial_numbers_serial ON inventory.serial_numbers(serial_number);
CREATE INDEX idx_serial_numbers_warehouse ON inventory.serial_numbers(warehouse_id);
CREATE INDEX idx_serial_numbers_status ON inventory.serial_numbers(status);

--changeset easyops:314-create-reorder-rules-table context:inventory
--comment: Create reorder point rules table
CREATE TABLE inventory.reorder_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES inventory.products(id) ON DELETE CASCADE,
    warehouse_id UUID REFERENCES inventory.warehouses(id),
    
    reorder_level DECIMAL(19, 4) NOT NULL,
    reorder_quantity DECIMAL(19, 4) NOT NULL,
    min_quantity DECIMAL(19, 4),
    max_quantity DECIMAL(19, 4),
    
    supplier_id UUID,
    lead_time_days INTEGER DEFAULT 7,
    
    auto_create_po BOOLEAN DEFAULT false,
    
    is_active BOOLEAN DEFAULT true,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    UNIQUE(product_id, warehouse_id)
);

CREATE INDEX idx_reorder_rules_org ON inventory.reorder_rules(organization_id);
CREATE INDEX idx_reorder_rules_product ON inventory.reorder_rules(product_id);
CREATE INDEX idx_reorder_rules_warehouse ON inventory.reorder_rules(warehouse_id);
CREATE INDEX idx_reorder_rules_active ON inventory.reorder_rules(is_active);

--changeset easyops:315-create-stock-counts-table context:inventory
--comment: Create physical inventory counts table
CREATE TABLE inventory.stock_counts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    
    count_number VARCHAR(50) UNIQUE NOT NULL,
    count_date DATE NOT NULL,
    count_type VARCHAR(50) DEFAULT 'FULL', -- FULL, CYCLE, SPOT
    
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    
    status VARCHAR(50) DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED, APPROVED, CANCELLED
    
    scheduled_date DATE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    counted_by UUID,
    verified_by UUID,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    
    total_variance_value DECIMAL(19, 4) DEFAULT 0,
    
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_stock_counts_org ON inventory.stock_counts(organization_id);
CREATE INDEX idx_stock_counts_warehouse ON inventory.stock_counts(warehouse_id);
CREATE INDEX idx_stock_counts_status ON inventory.stock_counts(status);
CREATE INDEX idx_stock_counts_date ON inventory.stock_counts(count_date);

--changeset easyops:316-create-stock-count-lines-table context:inventory
--comment: Create physical inventory count line items table
CREATE TABLE inventory.stock_count_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    count_id UUID NOT NULL REFERENCES inventory.stock_counts(id) ON DELETE CASCADE,
    
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES inventory.products(id),
    
    location_id UUID REFERENCES inventory.stock_locations(id),
    batch_number VARCHAR(100),
    
    system_quantity DECIMAL(19, 4) DEFAULT 0,
    counted_quantity DECIMAL(19, 4),
    variance_quantity DECIMAL(19, 4) DEFAULT 0,
    
    unit_cost DECIMAL(19, 4) DEFAULT 0,
    variance_value DECIMAL(19, 4) DEFAULT 0,
    
    variance_reason VARCHAR(255),
    notes TEXT,
    
    counted_at TIMESTAMP WITH TIME ZONE,
    counted_by UUID,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(count_id, line_number)
);

CREATE INDEX idx_count_lines_count ON inventory.stock_count_lines(count_id);
CREATE INDEX idx_count_lines_product ON inventory.stock_count_lines(product_id);

--changeset easyops:317-create-inventory-triggers context:inventory splitStatements:false
--comment: Create triggers for inventory tables
-- Trigger to update stock availability when quantities change
CREATE OR REPLACE FUNCTION update_stock_availability()
RETURNS TRIGGER AS $$
BEGIN
    NEW.quantity_available = NEW.quantity_on_hand - NEW.quantity_allocated;
    NEW.total_cost = NEW.quantity_on_hand * NEW.unit_cost;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_stock_availability
BEFORE INSERT OR UPDATE ON inventory.stock
FOR EACH ROW EXECUTE FUNCTION update_stock_availability();

-- Trigger to update count variance
CREATE OR REPLACE FUNCTION update_count_variance()
RETURNS TRIGGER AS $$
BEGIN
    NEW.variance_quantity = NEW.counted_quantity - NEW.system_quantity;
    NEW.variance_value = NEW.variance_quantity * NEW.unit_cost;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_count_variance
BEFORE INSERT OR UPDATE ON inventory.stock_count_lines
FOR EACH ROW EXECUTE FUNCTION update_count_variance();

-- Standard updated_at triggers
CREATE TRIGGER update_product_categories_updated_at BEFORE UPDATE ON inventory.product_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON inventory.products FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_warehouses_updated_at BEFORE UPDATE ON inventory.warehouses FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_locations_updated_at BEFORE UPDATE ON inventory.stock_locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_updated_at BEFORE UPDATE ON inventory.stock FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_movements_updated_at BEFORE UPDATE ON inventory.stock_movements FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_transfers_updated_at BEFORE UPDATE ON inventory.stock_transfers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_adjustments_updated_at BEFORE UPDATE ON inventory.stock_adjustments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_reorder_rules_updated_at BEFORE UPDATE ON inventory.reorder_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_stock_counts_updated_at BEFORE UPDATE ON inventory.stock_counts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:318-create-inventory-views context:inventory
--comment: Create useful views for inventory reporting
CREATE OR REPLACE VIEW inventory.v_stock_summary AS
SELECT 
    s.organization_id,
    s.product_id,
    p.sku,
    p.name as product_name,
    p.category_id,
    s.warehouse_id,
    w.name as warehouse_name,
    SUM(s.quantity_on_hand) as total_on_hand,
    SUM(s.quantity_allocated) as total_allocated,
    SUM(s.quantity_available) as total_available,
    SUM(s.quantity_on_order) as total_on_order,
    SUM(s.total_cost) as total_value,
    p.reorder_level,
    p.min_stock_level,
    CASE 
        WHEN SUM(s.quantity_available) <= 0 THEN 'OUT_OF_STOCK'
        WHEN SUM(s.quantity_available) <= p.min_stock_level THEN 'LOW_STOCK'
        WHEN SUM(s.quantity_available) <= p.reorder_level THEN 'REORDER_POINT'
        ELSE 'ADEQUATE'
    END as stock_status
FROM inventory.stock s
JOIN inventory.products p ON s.product_id = p.id
JOIN inventory.warehouses w ON s.warehouse_id = w.id
WHERE s.status = 'AVAILABLE'
GROUP BY s.organization_id, s.product_id, p.sku, p.name, p.category_id, s.warehouse_id, w.name, p.reorder_level, p.min_stock_level;

CREATE OR REPLACE VIEW inventory.v_expiring_stock AS
SELECT 
    s.organization_id,
    s.product_id,
    p.sku,
    p.name as product_name,
    s.warehouse_id,
    w.name as warehouse_name,
    s.batch_number,
    s.expiry_date,
    s.quantity_on_hand,
    s.total_cost,
    CURRENT_DATE - s.expiry_date as days_past_expiry,
    s.expiry_date - CURRENT_DATE as days_until_expiry,
    CASE 
        WHEN s.expiry_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN s.expiry_date - CURRENT_DATE <= 30 THEN 'EXPIRING_SOON'
        WHEN s.expiry_date - CURRENT_DATE <= 90 THEN 'EXPIRING_NEXT_QUARTER'
        ELSE 'GOOD'
    END as expiry_status
FROM inventory.stock s
JOIN inventory.products p ON s.product_id = p.id
JOIN inventory.warehouses w ON s.warehouse_id = w.id
WHERE s.expiry_date IS NOT NULL
AND s.quantity_on_hand > 0
ORDER BY s.expiry_date;

CREATE OR REPLACE VIEW inventory.v_low_stock_alerts AS
SELECT 
    s.organization_id,
    s.product_id,
    p.sku,
    p.name as product_name,
    p.category_id,
    s.warehouse_id,
    w.name as warehouse_name,
    SUM(s.quantity_available) as available_quantity,
    p.min_stock_level,
    p.reorder_level,
    p.safety_stock,
    r.reorder_quantity,
    r.lead_time_days,
    CASE 
        WHEN SUM(s.quantity_available) <= 0 THEN 'CRITICAL'
        WHEN SUM(s.quantity_available) <= p.min_stock_level THEN 'LOW'
        WHEN SUM(s.quantity_available) <= p.reorder_level THEN 'REORDER'
        ELSE 'OK'
    END as alert_level
FROM inventory.stock s
JOIN inventory.products p ON s.product_id = p.id
JOIN inventory.warehouses w ON s.warehouse_id = w.id
LEFT JOIN inventory.reorder_rules r ON s.product_id = r.product_id AND s.warehouse_id = r.warehouse_id
WHERE s.status = 'AVAILABLE'
AND p.track_inventory = true
GROUP BY s.organization_id, s.product_id, p.sku, p.name, p.category_id, s.warehouse_id, w.name,
         p.min_stock_level, p.reorder_level, p.safety_stock, r.reorder_quantity, r.lead_time_days
HAVING SUM(s.quantity_available) <= p.reorder_level;

--changeset easyops:319-grant-inventory-permissions context:inventory
--comment: Grant permissions on inventory tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA inventory TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA inventory TO easyops;
GRANT SELECT ON inventory.v_stock_summary TO easyops;
GRANT SELECT ON inventory.v_expiring_stock TO easyops;
GRANT SELECT ON inventory.v_low_stock_alerts TO easyops;

