--liquibase formatted sql

--changeset easyops:109-insert-test-quotations context:test-data
--comment: Insert test quotations for development and testing
INSERT INTO sales.quotations (organization_id, quotation_number, quotation_date, valid_until, customer_id, customer_name, customer_email, contact_person, subtotal, tax_amount, total_amount, status, notes, terms_and_conditions, sales_person_name, created_by) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'QUO-2024-001', '2024-10-01', '2024-11-01', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST001'), 'Premium Client Corp', 'sales@premiumclient.com', 'Alice Johnson',
 15000.00, 1275.00, 16275.00, 'SENT', 'High-priority client, need quick response', 
 'Payment terms: Net 30. Prices valid for 30 days from quotation date.', 'John Salesman', (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'QUO-2024-002', '2024-10-02', '2024-11-02', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST002'), 'Tech Innovators LLC', 'orders@techinnovators.com', 'Bob Smith',
 8500.00, 722.50, 9222.50, 'DRAFT', 'Tech startup with budget constraints', 
 'Payment terms: Net 15. Special pricing for startup.', 'Sarah Salesperson', (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'QUO-2024-003', '2024-10-03', '2024-11-03', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST003'), 'Global Enterprises', 'procurement@globalenterprises.com', 'Carol Davis',
 45000.00, 3825.00, 48825.00, 'ACCEPTED', 'Large enterprise deal, volume discount applied', 
 'Payment terms: Net 45. Volume discount: 10%.', 'Mike Sales Manager', (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'QUO-2024-004', '2024-10-04', '2024-11-04', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST004'), 'Startup Solutions Inc', 'info@startupsolutions.com', 'David Wilson',
 3200.00, 272.00, 3472.00, 'EXPIRED', 'Quotation expired, follow up needed', 
 'Payment terms: Net 30. Startup pricing applied.', 'Lisa Sales Rep', (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'QUO-2024-005', '2024-10-05', '2024-11-05', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST005'), 'Manufacturing Co', 'orders@manufacturingco.com', 'Eva Brown',
 28000.00, 2380.00, 30380.00, 'SENT', 'Manufacturing equipment quotation', 
 'Payment terms: Net 60. Equipment delivery in 4-6 weeks.', 'Robert Sales Director', (SELECT id FROM users.users WHERE username = 'admin'));

--changeset easyops:110-insert-test-quotation-lines context:test-data
--comment: Insert test quotation lines for development and testing
INSERT INTO sales.quotation_lines (quotation_id, line_number, product_id, product_code, product_name, description, quantity, unit_of_measure, unit_price, discount_percent, line_total, tax_percent, tax_amount) VALUES
-- Quotation QUO-2024-001 lines
((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-001'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 10, 'Unit', 1299.99, 0, 12999.90, 8.5, 1104.99),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-001'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD002'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 
 5, 'Unit', 299.99, 5, 1424.95, 8.5, 121.12),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-001'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD003'), 'PROD003', 'Software License', 'Annual software license for productivity suite', 
 15, 'License', 99.99, 10, 1349.85, 8.5, 114.74),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-001'), 4, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD004'), 'PROD004', 'Consulting Services', 'Business consulting services per hour', 
 20, 'Hour', 150.00, 0, 3000.00, 8.5, 255.00),

-- Quotation QUO-2024-002 lines
((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-002'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 5, 'Unit', 1299.99, 15, 5524.96, 8.5, 469.62),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-002'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD005'), 'PROD005', 'Printer Paper', 'A4 white printer paper, 500 sheets per ream', 
 20, 'Ream', 8.99, 0, 179.80, 8.5, 15.28),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-002'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD012'), 'PROD012', 'Cloud Storage', 'Monthly cloud storage subscription', 
 12, 'Month', 29.99, 0, 359.88, 8.5, 30.59),

-- Quotation QUO-2024-003 lines
((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-003'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 25, 'Unit', 1299.99, 10, 29249.78, 8.5, 2486.23),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-003'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD002'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 
 50, 'Unit', 299.99, 10, 13499.55, 8.5, 1147.46),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-003'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD011'), 'PROD011', 'Office Desk', 'Executive office desk with drawers', 
 15, 'Unit', 599.99, 5, 8549.86, 8.5, 726.74),

((SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-003'), 4, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD010'), 'PROD010', 'Website Development', 'Custom website development project', 
 1, 'Project', 5000.00, 0, 5000.00, 8.5, 425.00);

--changeset easyops:111-insert-test-sales-orders context:test-data
--comment: Insert test sales orders for development and testing
INSERT INTO sales.sales_orders (organization_id, order_number, order_date, expected_delivery_date, customer_id, customer_name, customer_email, contact_person, subtotal, tax_amount, total_amount, status, payment_status, delivery_status, priority, notes, terms_and_conditions, sales_person_name, quotation_id, created_by) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SO-2024-001', '2024-10-15', '2024-10-30', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST003'), 'Global Enterprises', 'procurement@globalenterprises.com', 'Carol Davis',
 45000.00, 3825.00, 48825.00, 'CONFIRMED', 'UNPAID', 'NOT_DELIVERED', 'HIGH', 'Large enterprise order, priority shipping', 
 'Payment terms: Net 45. Delivery within 2 weeks.', 'Mike Sales Manager', 
 (SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-003'), (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SO-2024-002', '2024-10-16', '2024-11-01', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST001'), 'Premium Client Corp', 'sales@premiumclient.com', 'Alice Johnson',
 12000.00, 1020.00, 13020.00, 'IN_PROGRESS', 'PARTIALLY_PAID', 'PARTIALLY_DELIVERED', 'NORMAL', 'Partial delivery completed', 
 'Payment terms: Net 30. Partial delivery acceptable.', 'John Salesman', 
 (SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-001'), (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SO-2024-003', '2024-10-17', '2024-10-25', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST005'), 'Manufacturing Co', 'orders@manufacturingco.com', 'Eva Brown',
 28000.00, 2380.00, 30380.00, 'COMPLETED', 'PAID', 'DELIVERED', 'NORMAL', 'Order completed successfully', 
 'Payment terms: Net 60. Manufacturing equipment delivered.', 'Robert Sales Director', 
 (SELECT id FROM sales.quotations WHERE quotation_number = 'QUO-2024-005'), (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SO-2024-004', '2024-10-18', '2024-11-05', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST007'), 'Service Provider Ltd', 'accounts@serviceprovider.com', 'Grace Taylor',
 3500.00, 297.50, 3797.50, 'DRAFT', 'UNPAID', 'NOT_DELIVERED', 'LOW', 'Service order, no physical delivery', 
 'Payment terms: Net 15. Services to be delivered remotely.', 'Lisa Sales Rep', null, (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SO-2024-005', '2024-10-19', '2024-10-31', 
 (SELECT id FROM sales.customers WHERE customer_code = 'SCUST010'), 'Educational Institution', 'purchasing@university.edu', 'Jack Thompson',
 15000.00, 1275.00, 16275.00, 'CONFIRMED', 'UNPAID', 'NOT_DELIVERED', 'NORMAL', 'Educational discount applied', 
 'Payment terms: Net 45. Educational pricing: 15% discount.', 'Sarah Salesperson', null, (SELECT id FROM users.users WHERE username = 'admin'));

--changeset easyops:112-insert-test-sales-order-lines context:test-data
--comment: Insert test sales order lines for development and testing
INSERT INTO sales.sales_order_lines (sales_order_id, line_number, product_id, product_code, product_name, description, quantity, unit_of_measure, unit_price, discount_percent, line_total, tax_percent, tax_amount, delivered_quantity, invoiced_quantity) VALUES
-- Sales Order SO-2024-001 lines
((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-001'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 25, 'Unit', 1299.99, 10, 29249.78, 8.5, 2486.23, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-001'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD002'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 
 50, 'Unit', 299.99, 10, 13499.55, 8.5, 1147.46, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-001'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD011'), 'PROD011', 'Office Desk', 'Executive office desk with drawers', 
 15, 'Unit', 599.99, 5, 8549.86, 8.5, 726.74, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-001'), 4, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD010'), 'PROD010', 'Website Development', 'Custom website development project', 
 1, 'Project', 5000.00, 0, 5000.00, 8.5, 425.00, 0, 0),

-- Sales Order SO-2024-002 lines
((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-002'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 8, 'Unit', 1299.99, 0, 10399.92, 8.5, 883.99, 5, 3),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-002'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD002'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 
 3, 'Unit', 299.99, 5, 854.96, 8.5, 72.67, 3, 3),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-002'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD003'), 'PROD003', 'Software License', 'Annual software license for productivity suite', 
 10, 'License', 99.99, 10, 899.91, 8.5, 76.49, 10, 10),

-- Sales Order SO-2024-003 lines
((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-003'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD007'), 'PROD007', 'Desk Lamp', 'LED desk lamp with adjustable brightness', 
 100, 'Unit', 89.99, 0, 8999.00, 8.5, 764.92, 100, 100),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-003'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD009'), 'PROD009', 'Coffee Maker', 'Commercial-grade coffee maker for office', 
 20, 'Unit', 249.99, 0, 4999.80, 8.5, 424.98, 20, 20),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-003'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD011'), 'PROD011', 'Office Desk', 'Executive office desk with drawers', 
 50, 'Unit', 599.99, 5, 28499.55, 8.5, 2422.46, 50, 50),

-- Sales Order SO-2024-004 lines
((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-004'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD004'), 'PROD004', 'Consulting Services', 'Business consulting services per hour', 
 20, 'Hour', 150.00, 0, 3000.00, 8.5, 255.00, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-004'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD008'), 'PROD008', 'Training Session', 'Professional training session for team', 
 1, 'Session', 500.00, 0, 500.00, 8.5, 42.50, 0, 0),

-- Sales Order SO-2024-005 lines
((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-005'), 1, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD001'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 
 10, 'Unit', 1299.99, 15, 11049.92, 8.5, 939.24, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-005'), 2, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD002'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 
 10, 'Unit', 299.99, 15, 2549.92, 8.5, 216.74, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-005'), 3, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD003'), 'PROD003', 'Software License', 'Annual software license for productivity suite', 
 10, 'License', 99.99, 15, 849.92, 8.5, 72.24, 0, 0),

((SELECT id FROM sales.sales_orders WHERE order_number = 'SO-2024-005'), 4, 
 (SELECT id FROM sales.products WHERE product_code = 'PROD012'), 'PROD012', 'Cloud Storage', 'Monthly cloud storage subscription', 
 12, 'Month', 29.99, 0, 359.88, 8.5, 30.59, 0, 0);
