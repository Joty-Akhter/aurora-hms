--liquibase formatted sql

--changeset easyops:201-insert-test-warehouses context:test-data
--comment: Insert test warehouses for development and testing
INSERT INTO inventory.warehouses (organization_id, code, name, warehouse_type, address_line1, city, state, country, phone, is_active, status) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WH-MAIN', 'Main Warehouse', 'MAIN', '123 Warehouse Blvd', 'Los Angeles', 'CA', 'US', '+1-555-0301', true, 'OPERATIONAL'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WH-EAST', 'East Coast Distribution Center', 'DISTRIBUTION', '456 Distribution Way', 'New York', 'NY', 'US', '+1-555-0302', true, 'OPERATIONAL'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WH-WEST', 'West Coast Warehouse', 'DISTRIBUTION', '789 Logistics Ave', 'Seattle', 'WA', 'US', '+1-555-0303', true, 'OPERATIONAL'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WH-TRANSIT', 'Transit Warehouse', 'TRANSIT', '321 Transit St', 'Chicago', 'IL', 'US', '+1-555-0304', true, 'OPERATIONAL');

--changeset easyops:202-insert-test-product-categories context:test-data
--comment: Insert test product categories
INSERT INTO inventory.product_categories (organization_id, code, name, description, is_active, display_order) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-ELEC', 'Electronics', 'Electronic devices and components', true, 1),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-FURN', 'Furniture', 'Office and home furniture', true, 2),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-SUPP', 'Supplies', 'Office supplies and consumables', true, 3),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-SOFT', 'Software', 'Software products and licenses', true, 4),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-SERV', 'Services', 'Service products', true, 5);

--changeset easyops:203-insert-test-inventory-products context:test-data
--comment: Insert test products for inventory module
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, is_active, status) VALUES
-- Electronics
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'LAPTOP-001', 'Dell Latitude 5420 Laptop', 'Business laptop with 16GB RAM, 512GB SSD',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'GOODS', 'STOCKABLE', 800.00, 1200.00, 'PCS', 10, 5, 2, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'MONITOR-001', 'Dell 24" Monitor', 'Full HD monitor with HDMI and DisplayPort',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'GOODS', 'STOCKABLE', 150.00, 250.00, 'PCS', 15, 8, 3, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'MOUSE-001', 'Logitech Wireless Mouse', 'Ergonomic wireless mouse',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'GOODS', 'STOCKABLE', 15.00, 30.00, 'PCS', 50, 20, 10, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'KEYBOARD-001', 'Logitech Keyboard', 'Wireless keyboard with numeric pad',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'GOODS', 'STOCKABLE', 25.00, 50.00, 'PCS', 40, 15, 8, true, true, 'ACTIVE'),

-- Furniture
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'DESK-001', 'Executive Office Desk', 'Large executive desk with drawers',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'GOODS', 'STOCKABLE', 300.00, 600.00, 'PCS', 5, 2, 1, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHAIR-001', 'Ergonomic Office Chair', 'Adjustable ergonomic chair',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'GOODS', 'STOCKABLE', 150.00, 350.00, 'PCS', 10, 5, 2, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CABINET-001', 'File Cabinet 4-Drawer', 'Metal file cabinet',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'GOODS', 'STOCKABLE', 100.00, 200.00, 'PCS', 8, 3, 1, true, true, 'ACTIVE'),

-- Supplies
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PAPER-001', 'A4 Paper 500 Sheets', 'Premium A4 white paper',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'GOODS', 'STOCKABLE', 5.00, 12.00, 'REAM', 100, 50, 20, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PEN-001', 'Ballpoint Pens Box of 12', 'Blue ink ballpoint pens',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'GOODS', 'STOCKABLE', 3.00, 8.00, 'BOX', 80, 30, 15, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FOLDER-001', 'Manila File Folders Box of 100', 'Standard manila folders',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'GOODS', 'STOCKABLE', 8.00, 20.00, 'BOX', 50, 20, 10, true, true, 'ACTIVE'),

-- Software
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-MS365', 'Microsoft 365 Business License', 'Annual license',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 'SERVICE', 'NON_STOCKABLE', 100.00, 150.00, 'LICENSE', 0, 0, 0, false, true, 'ACTIVE'),

-- Services
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-CONSULT', 'IT Consulting Services', 'Hourly consulting',
 (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 'SERVICE', 'NON_STOCKABLE', 80.00, 150.00, 'HOUR', 0, 0, 0, false, true, 'ACTIVE');

--changeset easyops:204-insert-test-stock context:test-data
--comment: Insert initial stock levels for test products
INSERT INTO inventory.stock (organization_id, product_id, warehouse_id, quantity_on_hand, quantity_allocated, unit_cost, status) VALUES
-- Main Warehouse Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'LAPTOP-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 25, 0, 800.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'MONITOR-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 50, 0, 150.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'MOUSE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 150, 0, 15.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'KEYBOARD-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 100, 0, 25.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'DESK-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 8, 0, 300.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'CHAIR-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 20, 0, 150.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'PAPER-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 200, 0, 5.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'PEN-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 150, 0, 3.00, 'AVAILABLE'),

-- East Warehouse Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'LAPTOP-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-EAST'), 15, 0, 800.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'MONITOR-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-EAST'), 30, 0, 150.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'CHAIR-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-EAST'), 12, 0, 150.00, 'AVAILABLE');

--changeset easyops:205-insert-test-stock-movements context:test-data
--comment: Insert sample stock movements for history
INSERT INTO inventory.stock_movements (organization_id, transaction_number, transaction_date, transaction_type, product_id, warehouse_id, quantity, uom, unit_cost, total_cost, source_type, status, created_by) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'REC-001', '2024-10-01', 'RECEIPT', 
 (SELECT id FROM inventory.products WHERE sku = 'LAPTOP-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 25, 'PCS', 800.00, 20000.00, 'PURCHASE_ORDER', 'COMPLETED',
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'REC-002', '2024-10-01', 'RECEIPT', 
 (SELECT id FROM inventory.products WHERE sku = 'MONITOR-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 50, 'PCS', 150.00, 7500.00, 'PURCHASE_ORDER', 'COMPLETED',
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'ISS-001', '2024-10-05', 'ISSUE', 
 (SELECT id FROM inventory.products WHERE sku = 'LAPTOP-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), -3, 'PCS', 800.00, -2400.00, 'SALES_ORDER', 'COMPLETED',
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'ADJ-001', '2024-10-10', 'ADJUSTMENT', 
 (SELECT id FROM inventory.products WHERE sku = 'MOUSE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 5, 'PCS', 15.00, 75.00, 'MANUAL', 'COMPLETED',
 (SELECT id FROM users.users WHERE username = 'admin'));

