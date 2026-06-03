-- Create sales schema
CREATE SCHEMA IF NOT EXISTS sales;

-- Set search path
SET search_path TO sales;

-- Customers table (denormalized from AR for performance)
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
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
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_code UNIQUE (organization_id, customer_code)
);

-- Products/Services table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    product_type VARCHAR(50) NOT NULL, -- GOODS, SERVICE
    category VARCHAR(100),
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) NOT NULL DEFAULT 0,
    cost_price DECIMAL(19, 4),
    tax_rate DECIMAL(5, 2) DEFAULT 0,
    revenue_account_id UUID,
    cogs_account_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    track_inventory BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_code UNIQUE (organization_id, product_code)
);

-- Sales Quotations
CREATE TABLE quotations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    quotation_number VARCHAR(50) NOT NULL,
    quotation_date DATE NOT NULL,
    valid_until DATE NOT NULL,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    contact_person VARCHAR(255),
    billing_address TEXT,
    shipping_address TEXT,
    
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED
    notes TEXT,
    terms_and_conditions TEXT,
    
    sales_person_id UUID,
    sales_person_name VARCHAR(255),
    
    converted_to_order_id UUID,
    converted_date TIMESTAMP,
    
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_quotation_number UNIQUE (organization_id, quotation_number),
    CONSTRAINT fk_quotation_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Quotation Lines
CREATE TABLE quotation_lines (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    
    product_id UUID,
    product_code VARCHAR(50),
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    
    quantity DECIMAL(19, 4) NOT NULL DEFAULT 1,
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) NOT NULL DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    line_total DECIMAL(19, 4) NOT NULL DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_quotation_line_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE,
    CONSTRAINT fk_quotation_line_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Sales Orders
CREATE TABLE sales_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    contact_person VARCHAR(255),
    billing_address TEXT,
    shipping_address TEXT,
    
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, INVOICED
    payment_status VARCHAR(50) DEFAULT 'UNPAID', -- UNPAID, PARTIALLY_PAID, PAID
    delivery_status VARCHAR(50) DEFAULT 'NOT_DELIVERED', -- NOT_DELIVERED, PARTIALLY_DELIVERED, DELIVERED
    
    priority VARCHAR(50) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    
    notes TEXT,
    terms_and_conditions TEXT,
    
    sales_person_id UUID,
    sales_person_name VARCHAR(255),
    
    quotation_id UUID,
    converted_to_invoice_id UUID,
    converted_to_invoice_date TIMESTAMP,
    
    approved_by UUID,
    approved_date TIMESTAMP,
    
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_order_number UNIQUE (organization_id, order_number),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_order_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id)
);

-- Sales Order Lines
CREATE TABLE sales_order_lines (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    
    product_id UUID,
    product_code VARCHAR(50),
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    
    quantity DECIMAL(19, 4) NOT NULL DEFAULT 1,
    unit_of_measure VARCHAR(50),
    unit_price DECIMAL(19, 4) NOT NULL DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    line_total DECIMAL(19, 4) NOT NULL DEFAULT 0,
    
    delivered_quantity DECIMAL(19, 4) DEFAULT 0,
    invoiced_quantity DECIMAL(19, 4) DEFAULT 0,
    
    revenue_account_id UUID,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_order_line_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_line_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Sales Deliveries (for tracking shipments)
CREATE TABLE deliveries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    delivery_number VARCHAR(50) NOT NULL,
    sales_order_id UUID NOT NULL,
    delivery_date DATE NOT NULL,
    shipping_address TEXT,
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_TRANSIT, DELIVERED, RETURNED
    notes TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_delivery_number UNIQUE (organization_id, delivery_number),
    CONSTRAINT fk_delivery_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id)
);

-- Delivery Lines
CREATE TABLE delivery_lines (
    id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL,
    sales_order_line_id UUID NOT NULL,
    product_id UUID,
    product_name VARCHAR(255),
    quantity DECIMAL(19, 4) NOT NULL DEFAULT 0,
    unit_of_measure VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_line_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_line_order_line FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_lines(id)
);

-- Indexes for performance
CREATE INDEX idx_customers_org ON customers(organization_id);
CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_active ON customers(is_active);

CREATE INDEX idx_products_org ON products(organization_id);
CREATE INDEX idx_products_code ON products(product_code);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_type ON products(product_type);

CREATE INDEX idx_quotations_org ON quotations(organization_id);
CREATE INDEX idx_quotations_number ON quotations(quotation_number);
CREATE INDEX idx_quotations_customer ON quotations(customer_id);
CREATE INDEX idx_quotations_status ON quotations(status);
CREATE INDEX idx_quotations_date ON quotations(quotation_date);

CREATE INDEX idx_quotation_lines_quotation ON quotation_lines(quotation_id);

CREATE INDEX idx_orders_org ON sales_orders(organization_id);
CREATE INDEX idx_orders_number ON sales_orders(order_number);
CREATE INDEX idx_orders_customer ON sales_orders(customer_id);
CREATE INDEX idx_orders_status ON sales_orders(status);
CREATE INDEX idx_orders_date ON sales_orders(order_date);
CREATE INDEX idx_orders_quotation ON sales_orders(quotation_id);

CREATE INDEX idx_order_lines_order ON sales_order_lines(sales_order_id);

CREATE INDEX idx_deliveries_org ON deliveries(organization_id);
CREATE INDEX idx_deliveries_order ON deliveries(sales_order_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);

-- Comments
COMMENT ON TABLE customers IS 'Customer master data for sales';
COMMENT ON TABLE products IS 'Product and service catalog';
COMMENT ON TABLE quotations IS 'Sales quotations/proposals';
COMMENT ON TABLE quotation_lines IS 'Line items in quotations';
COMMENT ON TABLE sales_orders IS 'Sales orders';
COMMENT ON TABLE sales_order_lines IS 'Line items in sales orders';
COMMENT ON TABLE deliveries IS 'Delivery/shipping records';
COMMENT ON TABLE delivery_lines IS 'Line items in deliveries';

