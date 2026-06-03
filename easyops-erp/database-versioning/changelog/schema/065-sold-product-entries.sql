--liquibase formatted sql

--changeset easyops:065-sold-product-entries context:pharma
--comment: Sold Product Entry (product-wise sales) split from Deposit Amount Entry

CREATE TABLE pharma.sold_product_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    area_id UUID NOT NULL REFERENCES pharma.areas(id),
    employee_id UUID,
    entry_date DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    total_product_amount DECIMAL(19, 2),
    status VARCHAR(50) DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE TABLE pharma.sold_product_entry_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sold_product_entry_id UUID NOT NULL REFERENCES pharma.sold_product_entries(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    tp_with_vat DECIMAL(19, 4),
    quantity_sold DECIMAL(19, 4) NOT NULL,
    current_outstanding_quantity DECIMAL(19, 4),
    product_amount DECIMAL(19, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sold_product_entries_area_date ON pharma.sold_product_entries(area_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_sold_product_entries_org_status ON pharma.sold_product_entries(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_sold_product_entry_lines_entry ON pharma.sold_product_entry_lines(sold_product_entry_id);
CREATE INDEX IF NOT EXISTS idx_sold_product_entry_lines_product ON pharma.sold_product_entry_lines(product_id, sold_product_entry_id);

COMMENT ON TABLE pharma.sold_product_entries IS 'Product-wise sales entries (split from deposit). Drives outstanding qty and target coverage.';
COMMENT ON TABLE pharma.sold_product_entry_lines IS 'Line items for sold product entries.';
