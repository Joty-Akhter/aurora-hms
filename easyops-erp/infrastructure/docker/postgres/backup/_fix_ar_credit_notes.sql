-- Add AR Credit Notes tables
-- This script can be run multiple times safely

-- AR Credit Notes
CREATE TABLE IF NOT EXISTS accounting.ar_credit_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    credit_note_number VARCHAR(50) UNIQUE NOT NULL,
    credit_note_date DATE NOT NULL,
    customer_id UUID NOT NULL REFERENCES accounting.customers(id),
    invoice_id UUID REFERENCES accounting.ar_invoices(id), -- Optional reference to invoice
    reason VARCHAR(20), -- RETURN, DAMAGE, PRICING_ERROR, DISCOUNT, CANCELLATION, OTHER
    notes VARCHAR(1000),
    subtotal DECIMAL(15, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, POSTED, APPLIED, CANCELLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- AR Credit Note Lines
CREATE TABLE IF NOT EXISTS accounting.ar_credit_note_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    credit_note_id UUID NOT NULL REFERENCES accounting.ar_credit_notes(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description VARCHAR(500),
    quantity DECIMAL(15, 4) DEFAULT 1,
    unit_price DECIMAL(15, 2) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(15, 2) DEFAULT 0,
    account_id UUID REFERENCES accounting.chart_of_accounts(id) -- Revenue account to credit
);

-- Comments
COMMENT ON TABLE accounting.ar_credit_notes IS 'Credit notes for customer returns, discounts, and adjustments';
COMMENT ON TABLE accounting.ar_credit_note_lines IS 'Line items for AR credit notes';

