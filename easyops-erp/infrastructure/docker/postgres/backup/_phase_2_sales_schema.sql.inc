-- ============================================================
-- PHASE 2: SALES MODULE SCHEMA
-- ============================================================
-- This script creates the sales schema and all related tables
-- for the Sales Module (Quotations, Orders, Customers, Products)
-- ============================================================

-- Create sales schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS sales;

-- Grant permissions to development user
GRANT ALL PRIVILEGES ON SCHEMA sales TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA sales TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA sales TO easyops_dev;

-- ============================================================
-- CUSTOMERS TABLE (Sales Module)
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    customer_code VARCHAR(50) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    contact_person VARCHAR(255),
    billing_address TEXT,
    shipping_address TEXT,
    credit_limit DECIMAL(19, 4) DEFAULT 0,
    current_balance DECIMAL(19, 4) DEFAULT 0,
    payment_terms VARCHAR(50),
    tax_number VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, customer_code)
);

-- ============================================================
-- PRODUCTS TABLE (Sales Module)
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    product_type VARCHAR(50) NOT NULL, -- GOODS, SERVICE
    category VARCHAR(100),
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) DEFAULT 0,
    cost_price DECIMAL(19, 4),
    tax_rate DECIMAL(5, 2) DEFAULT 0,
    revenue_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    cogs_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    is_active BOOLEAN DEFAULT true,
    track_inventory BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, product_code)
);

-- ============================================================
-- QUOTATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.quotations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    quotation_number VARCHAR(50) UNIQUE NOT NULL,
    quotation_date DATE NOT NULL,
    valid_until DATE NOT NULL,
    customer_id UUID NOT NULL REFERENCES sales.customers(id),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    contact_person VARCHAR(255),
    billing_address TEXT,
    shipping_address TEXT,
    subtotal DECIMAL(19, 4) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT' NOT NULL, -- DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED
    notes TEXT,
    terms_and_conditions TEXT,
    sales_person_id UUID,
    sales_person_name VARCHAR(255),
    converted_to_order_id UUID,
    converted_date TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- QUOTATION LINES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.quotation_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quotation_id UUID NOT NULL REFERENCES sales.quotations(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id UUID REFERENCES sales.products(id),
    product_code VARCHAR(50),
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity DECIMAL(19, 4) DEFAULT 1,
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    line_total DECIMAL(19, 4) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SALES ORDERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.sales_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    customer_id UUID NOT NULL REFERENCES sales.customers(id),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    contact_person VARCHAR(255),
    billing_address TEXT,
    shipping_address TEXT,
    subtotal DECIMAL(19, 4) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT' NOT NULL, -- DRAFT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, INVOICED
    payment_status VARCHAR(50) DEFAULT 'UNPAID', -- UNPAID, PARTIALLY_PAID, PAID
    delivery_status VARCHAR(50) DEFAULT 'NOT_DELIVERED', -- NOT_DELIVERED, PARTIALLY_DELIVERED, DELIVERED
    priority VARCHAR(50) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    notes TEXT,
    terms_and_conditions TEXT,
    sales_person_id UUID,
    sales_person_name VARCHAR(255),
    quotation_id UUID REFERENCES sales.quotations(id),
    converted_to_invoice_id UUID,
    converted_to_invoice_date TIMESTAMP WITH TIME ZONE,
    approved_by UUID,
    approved_date TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SALES ORDER LINES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS sales.sales_order_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sales_order_id UUID NOT NULL REFERENCES sales.sales_orders(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id UUID REFERENCES sales.products(id),
    product_code VARCHAR(50),
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity DECIMAL(19, 4) DEFAULT 1,
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    line_total DECIMAL(19, 4) DEFAULT 0,
    delivered_quantity DECIMAL(19, 4) DEFAULT 0,
    invoiced_quantity DECIMAL(19, 4) DEFAULT 0,
    revenue_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Customers indexes
CREATE INDEX IF NOT EXISTS idx_customers_org_id ON sales.customers(organization_id);
CREATE INDEX IF NOT EXISTS idx_customers_code ON sales.customers(customer_code);
CREATE INDEX IF NOT EXISTS idx_customers_active ON sales.customers(is_active);

-- Products indexes
CREATE INDEX IF NOT EXISTS idx_products_org_id ON sales.products(organization_id);
CREATE INDEX IF NOT EXISTS idx_products_code ON sales.products(product_code);
CREATE INDEX IF NOT EXISTS idx_products_active ON sales.products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_type ON sales.products(product_type);

-- Quotations indexes
CREATE INDEX IF NOT EXISTS idx_quotations_org_id ON sales.quotations(organization_id);
CREATE INDEX IF NOT EXISTS idx_quotations_customer_id ON sales.quotations(customer_id);
CREATE INDEX IF NOT EXISTS idx_quotations_status ON sales.quotations(status);
CREATE INDEX IF NOT EXISTS idx_quotations_date ON sales.quotations(quotation_date);
CREATE INDEX IF NOT EXISTS idx_quotations_number ON sales.quotations(quotation_number);

-- Quotation lines indexes
CREATE INDEX IF NOT EXISTS idx_quotation_lines_quotation_id ON sales.quotation_lines(quotation_id);
CREATE INDEX IF NOT EXISTS idx_quotation_lines_product_id ON sales.quotation_lines(product_id);

-- Sales orders indexes
CREATE INDEX IF NOT EXISTS idx_sales_orders_org_id ON sales.sales_orders(organization_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_customer_id ON sales.sales_orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_status ON sales.sales_orders(status);
CREATE INDEX IF NOT EXISTS idx_sales_orders_date ON sales.sales_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_sales_orders_number ON sales.sales_orders(order_number);
CREATE INDEX IF NOT EXISTS idx_sales_orders_quotation_id ON sales.sales_orders(quotation_id);

-- Sales order lines indexes
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_order_id ON sales.sales_order_lines(sales_order_id);
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_product_id ON sales.sales_order_lines(product_id);

-- ============================================================
-- COMMENTS
-- ============================================================
COMMENT ON SCHEMA sales IS 'Sales module schema for quotations, orders, customers, and products';
COMMENT ON TABLE sales.customers IS 'Customer master data for sales module';
COMMENT ON TABLE sales.products IS 'Product catalog for sales module';
COMMENT ON TABLE sales.quotations IS 'Sales quotations/proposals';
COMMENT ON TABLE sales.quotation_lines IS 'Line items for quotations';
COMMENT ON TABLE sales.sales_orders IS 'Sales orders';
COMMENT ON TABLE sales.sales_order_lines IS 'Line items for sales orders';

