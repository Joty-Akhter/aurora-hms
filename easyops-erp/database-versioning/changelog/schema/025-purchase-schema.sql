--liquibase formatted sql

--changeset easyops:025-create-purchase-schema
--comment: Create purchase schema and core tables for Purchase Module (Phase 4)

-- Create purchase schema
CREATE SCHEMA IF NOT EXISTS purchase;

-- Set search path
SET search_path TO purchase, public;

--rollback DROP SCHEMA IF EXISTS purchase CASCADE;

--changeset easyops:025-create-purchase-orders-table
--comment: Create purchase_orders table for PO header information

CREATE TABLE purchase.purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    po_number VARCHAR(50) NOT NULL,
    po_date DATE NOT NULL,
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    -- Status: DRAFT, SUBMITTED, APPROVED, RECEIVED, CANCELLED, CLOSED
    
    -- Financial details
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal_amount DECIMAL(15, 2) DEFAULT 0.00,
    tax_amount DECIMAL(15, 2) DEFAULT 0.00,
    discount_amount DECIMAL(15, 2) DEFAULT 0.00,
    total_amount DECIMAL(15, 2) DEFAULT 0.00,
    
    -- Delivery information
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    delivery_address TEXT,
    shipping_method VARCHAR(100),
    
    -- Terms and conditions
    payment_terms INTEGER DEFAULT 30, -- Days
    payment_method VARCHAR(50),
    incoterms VARCHAR(50),
    
    -- Approval workflow
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_by VARCHAR(255),
    submitted_at TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    cancelled_by VARCHAR(255),
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    -- Additional information
    priority VARCHAR(20) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    notes TEXT,
    terms_conditions TEXT,
    
    -- Audit fields
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1,
    
    CONSTRAINT uk_purchase_orders_po_number UNIQUE (organization_id, po_number),
    CONSTRAINT chk_purchase_orders_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'RECEIVED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT chk_purchase_orders_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

-- Create indexes
CREATE INDEX idx_purchase_orders_organization ON purchase.purchase_orders(organization_id);
CREATE INDEX idx_purchase_orders_vendor ON purchase.purchase_orders(vendor_id);
CREATE INDEX idx_purchase_orders_status ON purchase.purchase_orders(status);
CREATE INDEX idx_purchase_orders_po_date ON purchase.purchase_orders(po_date);
CREATE INDEX idx_purchase_orders_po_number ON purchase.purchase_orders(po_number);

-- Add comments
COMMENT ON TABLE purchase.purchase_orders IS 'Purchase order header information';
COMMENT ON COLUMN purchase.purchase_orders.status IS 'PO Status: DRAFT, SUBMITTED, APPROVED, RECEIVED, CANCELLED, CLOSED';

--rollback DROP TABLE IF EXISTS purchase.purchase_orders CASCADE;

--changeset easyops:025-create-purchase-order-lines-table
--comment: Create purchase_order_lines table for PO line items

CREATE TABLE purchase.purchase_order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id UUID NOT NULL REFERENCES purchase.purchase_orders(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    
    -- Product information
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_description TEXT,
    
    -- Quantity and pricing
    quantity DECIMAL(15, 3) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'EA',
    unit_price DECIMAL(15, 4) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0.00,
    discount_amount DECIMAL(15, 2) DEFAULT 0.00,
    tax_percent DECIMAL(5, 2) DEFAULT 0.00,
    tax_amount DECIMAL(15, 2) DEFAULT 0.00,
    line_total DECIMAL(15, 2) NOT NULL,
    
    -- Delivery information
    requested_delivery_date DATE,
    promised_delivery_date DATE,
    warehouse_id UUID,
    warehouse_name VARCHAR(255),
    
    -- Receipt tracking
    received_quantity DECIMAL(15, 3) DEFAULT 0.00,
    pending_quantity DECIMAL(15, 3),
    invoiced_quantity DECIMAL(15, 3) DEFAULT 0.00,
    
    -- Status and approval
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, APPROVED, PARTIALLY_RECEIVED, FULLY_RECEIVED, CANCELLED
    
    -- Additional information
    notes TEXT,
    specifications TEXT,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_purchase_order_lines_line_number UNIQUE (po_id, line_number),
    CONSTRAINT chk_purchase_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_order_lines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_purchase_order_lines_status CHECK (status IN ('PENDING', 'APPROVED', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED', 'CANCELLED'))
);

-- Create indexes
CREATE INDEX idx_purchase_order_lines_po_id ON purchase.purchase_order_lines(po_id);
CREATE INDEX idx_purchase_order_lines_product ON purchase.purchase_order_lines(product_id);
CREATE INDEX idx_purchase_order_lines_status ON purchase.purchase_order_lines(status);

-- Add comments
COMMENT ON TABLE purchase.purchase_order_lines IS 'Purchase order line items';
COMMENT ON COLUMN purchase.purchase_order_lines.pending_quantity IS 'Quantity ordered minus quantity received';

--rollback DROP TABLE IF EXISTS purchase.purchase_order_lines CASCADE;

--changeset easyops:025-create-purchase-receipts-table
--comment: Create purchase_receipts table for goods receipt notes

CREATE TABLE purchase.purchase_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    po_id UUID NOT NULL REFERENCES purchase.purchase_orders(id),
    receipt_number VARCHAR(50) NOT NULL,
    receipt_date DATE NOT NULL,
    
    -- Vendor information
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    
    -- Receipt details
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    -- Status: DRAFT, RECEIVED, INSPECTED, REJECTED, POSTED
    total_quantity DECIMAL(15, 3) DEFAULT 0.00,
    total_amount DECIMAL(15, 2) DEFAULT 0.00,
    
    -- Delivery information
    delivery_note_number VARCHAR(100),
    transporter_name VARCHAR(255),
    vehicle_number VARCHAR(50),
    received_by VARCHAR(255) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Quality inspection
    inspection_status VARCHAR(50),
    -- Status: PENDING, PASSED, FAILED, PARTIAL
    inspected_by VARCHAR(255),
    inspected_at TIMESTAMP,
    inspection_notes TEXT,
    
    -- Warehouse information
    warehouse_id UUID,
    warehouse_name VARCHAR(255),
    
    -- Additional information
    notes TEXT,
    attachments JSONB,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_purchase_receipts_receipt_number UNIQUE (organization_id, receipt_number),
    CONSTRAINT chk_purchase_receipts_status CHECK (status IN ('DRAFT', 'RECEIVED', 'INSPECTED', 'REJECTED', 'POSTED')),
    CONSTRAINT chk_purchase_receipts_inspection_status CHECK (inspection_status IN ('PENDING', 'PASSED', 'FAILED', 'PARTIAL'))
);

-- Create indexes
CREATE INDEX idx_purchase_receipts_organization ON purchase.purchase_receipts(organization_id);
CREATE INDEX idx_purchase_receipts_po_id ON purchase.purchase_receipts(po_id);
CREATE INDEX idx_purchase_receipts_vendor ON purchase.purchase_receipts(vendor_id);
CREATE INDEX idx_purchase_receipts_status ON purchase.purchase_receipts(status);
CREATE INDEX idx_purchase_receipts_receipt_date ON purchase.purchase_receipts(receipt_date);

-- Add comments
COMMENT ON TABLE purchase.purchase_receipts IS 'Goods receipt notes (GRN) for purchase orders';

--rollback DROP TABLE IF EXISTS purchase.purchase_receipts CASCADE;

--changeset easyops:025-create-purchase-receipt-lines-table
--comment: Create purchase_receipt_lines table for GRN line items

CREATE TABLE purchase.purchase_receipt_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL REFERENCES purchase.purchase_receipts(id) ON DELETE CASCADE,
    po_line_id UUID NOT NULL REFERENCES purchase.purchase_order_lines(id),
    line_number INTEGER NOT NULL,
    
    -- Product information
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    
    -- Quantity details
    ordered_quantity DECIMAL(15, 3) NOT NULL,
    received_quantity DECIMAL(15, 3) NOT NULL,
    accepted_quantity DECIMAL(15, 3) DEFAULT 0.00,
    rejected_quantity DECIMAL(15, 3) DEFAULT 0.00,
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'EA',
    
    -- Pricing
    unit_price DECIMAL(15, 4) NOT NULL,
    line_total DECIMAL(15, 2) NOT NULL,
    
    -- Quality information
    condition VARCHAR(50) NOT NULL DEFAULT 'GOOD',
    -- Condition: GOOD, DAMAGED, DEFECTIVE, SHORT, EXCESS
    quality_rating INTEGER, -- 1-5 scale
    
    -- Batch and serial tracking
    batch_number VARCHAR(100),
    serial_numbers JSONB,
    expiry_date DATE,
    
    -- Warehouse location
    warehouse_id UUID,
    warehouse_name VARCHAR(255),
    location_code VARCHAR(100),
    
    -- Additional information
    notes TEXT,
    rejection_reason TEXT,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_purchase_receipt_lines_line_number UNIQUE (receipt_id, line_number),
    CONSTRAINT chk_purchase_receipt_lines_received_qty CHECK (received_quantity >= 0),
    CONSTRAINT chk_purchase_receipt_lines_condition CHECK (condition IN ('GOOD', 'DAMAGED', 'DEFECTIVE', 'SHORT', 'EXCESS'))
);

-- Create indexes
CREATE INDEX idx_purchase_receipt_lines_receipt_id ON purchase.purchase_receipt_lines(receipt_id);
CREATE INDEX idx_purchase_receipt_lines_po_line_id ON purchase.purchase_receipt_lines(po_line_id);
CREATE INDEX idx_purchase_receipt_lines_product ON purchase.purchase_receipt_lines(product_id);
CREATE INDEX idx_purchase_receipt_lines_batch ON purchase.purchase_receipt_lines(batch_number);

-- Add comments
COMMENT ON TABLE purchase.purchase_receipt_lines IS 'Goods receipt note line items';

--rollback DROP TABLE IF EXISTS purchase.purchase_receipt_lines CASCADE;

--changeset easyops:025-create-purchase-invoices-table
--comment: Create purchase_invoices table for vendor invoices

CREATE TABLE purchase.purchase_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    po_id UUID NOT NULL REFERENCES purchase.purchase_orders(id),
    receipt_id UUID REFERENCES purchase.purchase_receipts(id),
    invoice_number VARCHAR(50) NOT NULL,
    invoice_date DATE NOT NULL,
    
    -- Vendor information
    vendor_id UUID NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_invoice_number VARCHAR(100),
    
    -- Financial details
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal_amount DECIMAL(15, 2) DEFAULT 0.00,
    tax_amount DECIMAL(15, 2) DEFAULT 0.00,
    discount_amount DECIMAL(15, 2) DEFAULT 0.00,
    shipping_amount DECIMAL(15, 2) DEFAULT 0.00,
    total_amount DECIMAL(15, 2) NOT NULL,
    
    -- Payment information
    due_date DATE,
    payment_terms INTEGER DEFAULT 30,
    paid_amount DECIMAL(15, 2) DEFAULT 0.00,
    balance_amount DECIMAL(15, 2),
    payment_status VARCHAR(50) DEFAULT 'UNPAID',
    -- Status: UNPAID, PARTIALLY_PAID, PAID, OVERDUE
    
    -- Invoice status
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    -- Status: DRAFT, SUBMITTED, APPROVED, POSTED, CANCELLED
    matching_status VARCHAR(50) DEFAULT 'PENDING',
    -- Status: PENDING, MATCHED, VARIANCE, APPROVED
    
    -- Three-way matching
    price_variance DECIMAL(15, 2) DEFAULT 0.00,
    quantity_variance DECIMAL(15, 3) DEFAULT 0.00,
    variance_notes TEXT,
    
    -- Approval workflow
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    posted_by VARCHAR(255),
    posted_at TIMESTAMP,
    
    -- Bill integration
    bill_id UUID,
    bill_created BOOLEAN DEFAULT FALSE,
    bill_created_at TIMESTAMP,
    
    -- Additional information
    notes TEXT,
    attachments JSONB,
    
    -- Audit fields
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_purchase_invoices_invoice_number UNIQUE (organization_id, invoice_number),
    CONSTRAINT chk_purchase_invoices_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'POSTED', 'CANCELLED')),
    CONSTRAINT chk_purchase_invoices_payment_status CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    CONSTRAINT chk_purchase_invoices_matching_status CHECK (matching_status IN ('PENDING', 'MATCHED', 'VARIANCE', 'APPROVED'))
);

-- Create indexes
CREATE INDEX idx_purchase_invoices_organization ON purchase.purchase_invoices(organization_id);
CREATE INDEX idx_purchase_invoices_po_id ON purchase.purchase_invoices(po_id);
CREATE INDEX idx_purchase_invoices_receipt_id ON purchase.purchase_invoices(receipt_id);
CREATE INDEX idx_purchase_invoices_vendor ON purchase.purchase_invoices(vendor_id);
CREATE INDEX idx_purchase_invoices_status ON purchase.purchase_invoices(status);
CREATE INDEX idx_purchase_invoices_payment_status ON purchase.purchase_invoices(payment_status);
CREATE INDEX idx_purchase_invoices_invoice_date ON purchase.purchase_invoices(invoice_date);
CREATE INDEX idx_purchase_invoices_due_date ON purchase.purchase_invoices(due_date);

-- Add comments
COMMENT ON TABLE purchase.purchase_invoices IS 'Vendor invoices for purchase orders';

--rollback DROP TABLE IF EXISTS purchase.purchase_invoices CASCADE;

--changeset easyops:025-create-purchase-invoice-lines-table
--comment: Create purchase_invoice_lines table for invoice line items

CREATE TABLE purchase.purchase_invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES purchase.purchase_invoices(id) ON DELETE CASCADE,
    po_line_id UUID NOT NULL REFERENCES purchase.purchase_order_lines(id),
    receipt_line_id UUID REFERENCES purchase.purchase_receipt_lines(id),
    line_number INTEGER NOT NULL,
    
    -- Product information
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    
    -- Quantity details
    ordered_quantity DECIMAL(15, 3) NOT NULL,
    received_quantity DECIMAL(15, 3) NOT NULL,
    invoiced_quantity DECIMAL(15, 3) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'EA',
    
    -- Pricing
    po_unit_price DECIMAL(15, 4) NOT NULL,
    invoice_unit_price DECIMAL(15, 4) NOT NULL,
    tax_percent DECIMAL(5, 2) DEFAULT 0.00,
    tax_amount DECIMAL(15, 2) DEFAULT 0.00,
    line_total DECIMAL(15, 2) NOT NULL,
    
    -- Variance tracking
    quantity_variance DECIMAL(15, 3) DEFAULT 0.00,
    price_variance DECIMAL(15, 2) DEFAULT 0.00,
    total_variance DECIMAL(15, 2) DEFAULT 0.00,
    variance_status VARCHAR(50) DEFAULT 'MATCHED',
    -- Status: MATCHED, UNDER_INVOICED, OVER_INVOICED, PRICE_VARIANCE, APPROVED
    variance_notes TEXT,
    
    -- Additional information
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_purchase_invoice_lines_line_number UNIQUE (invoice_id, line_number),
    CONSTRAINT chk_purchase_invoice_lines_invoiced_qty CHECK (invoiced_quantity >= 0),
    CONSTRAINT chk_purchase_invoice_lines_variance_status CHECK (variance_status IN ('MATCHED', 'UNDER_INVOICED', 'OVER_INVOICED', 'PRICE_VARIANCE', 'APPROVED'))
);

-- Create indexes
CREATE INDEX idx_purchase_invoice_lines_invoice_id ON purchase.purchase_invoice_lines(invoice_id);
CREATE INDEX idx_purchase_invoice_lines_po_line_id ON purchase.purchase_invoice_lines(po_line_id);
CREATE INDEX idx_purchase_invoice_lines_receipt_line_id ON purchase.purchase_invoice_lines(receipt_line_id);
CREATE INDEX idx_purchase_invoice_lines_product ON purchase.purchase_invoice_lines(product_id);

-- Add comments
COMMENT ON TABLE purchase.purchase_invoice_lines IS 'Vendor invoice line items with three-way matching';

--rollback DROP TABLE IF EXISTS purchase.purchase_invoice_lines CASCADE;

--changeset easyops:025-create-purchase-approvals-table
--comment: Create purchase_approvals table for approval workflow tracking

CREATE TABLE purchase.purchase_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    -- Type: PURCHASE_ORDER, PURCHASE_INVOICE
    document_id UUID NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    
    -- Approval details
    approval_level INTEGER NOT NULL,
    approver_id VARCHAR(255) NOT NULL,
    approver_name VARCHAR(255) NOT NULL,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, APPROVED, REJECTED
    
    -- Decision information
    approved_at TIMESTAMP,
    decision_notes TEXT,
    rejection_reason TEXT,
    
    -- Additional information
    amount DECIMAL(15, 2),
    currency VARCHAR(3),
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_purchase_approvals_document_type CHECK (document_type IN ('PURCHASE_ORDER', 'PURCHASE_INVOICE')),
    CONSTRAINT chk_purchase_approvals_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Create indexes
CREATE INDEX idx_purchase_approvals_organization ON purchase.purchase_approvals(organization_id);
CREATE INDEX idx_purchase_approvals_document ON purchase.purchase_approvals(document_type, document_id);
CREATE INDEX idx_purchase_approvals_approver ON purchase.purchase_approvals(approver_id);
CREATE INDEX idx_purchase_approvals_status ON purchase.purchase_approvals(approval_status);

-- Add comments
COMMENT ON TABLE purchase.purchase_approvals IS 'Purchase document approval workflow tracking';

--rollback DROP TABLE IF EXISTS purchase.purchase_approvals CASCADE;
