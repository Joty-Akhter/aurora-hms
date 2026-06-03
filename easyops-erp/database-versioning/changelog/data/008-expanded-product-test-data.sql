--liquibase formatted sql

--changeset easyops:301-insert-additional-product-categories context:test-data
--comment: Insert additional product categories for comprehensive testing
INSERT INTO inventory.product_categories (organization_id, code, name, description, is_active, display_order) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-RAW', 'Raw Materials', 'Raw materials for manufacturing', true, 6),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-FIN', 'Finished Goods', 'Manufactured finished products', true, 7),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-FOOD', 'Food & Beverage', 'Perishable food items', true, 8),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-CHEM', 'Chemicals', 'Industrial chemicals', true, 9),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAT-PACK', 'Packaging', 'Packaging materials', true, 10);

--changeset easyops:302-insert-expanded-electronics context:test-data
--comment: Insert expanded electronics inventory
INSERT INTO inventory.products (organization_id, sku, barcode, name, description, short_description, category_id, brand, manufacturer, model_number, product_type, item_type, cost_price, selling_price, wholesale_price, retail_price, currency, uom, reorder_level, min_stock_level, max_stock_level, safety_stock, weight, weight_unit, length, width, height, dimension_unit, tax_rate, track_inventory, track_serial, is_active, status) VALUES

-- High-end Electronics
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'LAPTOP-002', '885370278965', 'Apple MacBook Pro 14"', 
'MacBook Pro 14" with M3 Pro chip, 18GB RAM, 512GB SSD, Space Gray', 
'MacBook Pro 14" M3 Pro 18GB/512GB', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Apple', 'Apple Inc.', 'MBP14-M3PRO-2023', 
'GOODS', 'STOCKABLE', 1800.00, 2499.00, 2300.00, 2499.00, 'USD', 'PCS', 5, 2, 20, 1, 
1.60, 'KG', 31.26, 22.12, 1.55, 'CM', 10.00, true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PHONE-001', '194252707753', 'iPhone 15 Pro 256GB', 
'iPhone 15 Pro 256GB, Titanium Blue, Latest iOS', 
'iPhone 15 Pro 256GB Blue', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Apple', 'Apple Inc.', 'IPHONE15PRO-256', 
'GOODS', 'STOCKABLE', 800.00, 1199.00, 1000.00, 1199.00, 'USD', 'PCS', 10, 5, 30, 3, 
0.187, 'KG', 14.67, 7.09, 0.82, 'CM', 10.00, true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TABLET-001', '885909950805', 'iPad Air 10.9" 256GB', 
'iPad Air 10.9" with M1 chip, 256GB storage, WiFi, Space Gray', 
'iPad Air 256GB WiFi', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Apple', 'Apple Inc.', 'IPADAIR-M1-256', 
'GOODS', 'STOCKABLE', 450.00, 749.00, 650.00, 749.00, 'USD', 'PCS', 8, 3, 25, 2, 
0.461, 'KG', 24.76, 17.85, 0.61, 'CM', 10.00, true, false, true, 'ACTIVE'),

-- Computer Accessories
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'MONITOR-002', '5397184144022', 'LG UltraWide 34" Curved', 
'34" UltraWide QHD IPS curved monitor, 3440x1440, USB-C', 
'LG 34" UltraWide QHD', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'LG', 'LG Electronics', 'UW-34WP65C', 
'GOODS', 'STOCKABLE', 350.00, 549.00, 450.00, 549.00, 'USD', 'PCS', 10, 5, 30, 2, 
6.50, 'KG', 80.00, 35.50, 46.00, 'CM', 10.00, true, false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WEBCAM-001', '097855137678', 'Logitech Brio 4K Webcam', 
'4K Ultra HD webcam with HDR, auto-focus, dual microphones', 
'Logitech Brio 4K', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Logitech', 'Logitech', 'BRIO-4K', 
'GOODS', 'STOCKABLE', 120.00, 199.99, 160.00, 199.99, 'USD', 'PCS', 20, 10, 50, 5, 
0.180, 'KG', 10.20, 10.20, 4.30, 'CM', 10.00, true, false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'HEADSET-001', '097855140227', 'Logitech Zone Wireless', 
'Wireless headset with active noise cancellation, Teams certified', 
'Logitech Zone Wireless ANC', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Logitech', 'Logitech', 'ZONE-WL', 
'GOODS', 'STOCKABLE', 180.00, 299.99, 230.00, 299.99, 'USD', 'PCS', 15, 8, 40, 4, 
0.185, 'KG', 17.50, 18.00, 7.00, 'CM', 10.00, true, false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'DOCKING-001', '012303304567', 'Dell WD19TBS Dock', 
'Thunderbolt docking station, 180W power delivery, triple display support', 
'Dell WD19TBS Thunderbolt Dock', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Dell', 'Dell Technologies', 'WD19TBS', 
'GOODS', 'STOCKABLE', 220.00, 379.99, 280.00, 379.99, 'USD', 'PCS', 12, 6, 35, 3, 
0.585, 'KG', 20.50, 9.00, 2.90, 'CM', 10.00, true, true, true, 'ACTIVE'),

-- Networking Equipment
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SWITCH-001', '790069454318', 'Cisco 24-Port Gigabit Switch', 
'Managed Gigabit Ethernet switch, 24 ports, rack-mountable', 
'Cisco 24-Port Switch', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Cisco', 'Cisco Systems', 'SG250-24-K9', 
'GOODS', 'STOCKABLE', 280.00, 449.00, 350.00, 449.00, 'USD', 'PCS', 5, 2, 15, 1, 
2.30, 'KG', 44.00, 25.80, 4.40, 'CM', 10.00, true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'ROUTER-001', '606449146530', 'Netgear Nighthawk Pro', 
'WiFi 6 router, tri-band, up to 3Gbps, MU-MIMO', 'Netgear Nighthawk WiFi 6', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Netgear', 'Netgear Inc.', 'RAX200', 
'GOODS', 'STOCKABLE', 250.00, 399.99, 310.00, 399.99, 'USD', 'PCS', 8, 4, 25, 2, 
1.20, 'KG', 29.60, 21.30, 7.80, 'CM', 10.00, true, false, true, 'ACTIVE');

--changeset easyops:303-insert-expanded-furniture context:test-data
--comment: Insert expanded furniture products
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, brand, product_type, item_type, cost_price, selling_price, wholesale_price, uom, reorder_level, min_stock_level, safety_stock, weight, weight_unit, length, width, height, dimension_unit, track_inventory, is_active, status) VALUES

-- Office Furniture
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'DESK-002', 'Standing Desk Adjustable', 
'Electric height-adjustable standing desk, 60x30", memory presets', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'FlexiSpot', 
'GOODS', 'STOCKABLE', 280.00, 549.00, 400.00, 'PCS', 5, 2, 1, 
32.00, 'KG', 152.40, 76.20, 118.00, 'CM', true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHAIR-002', 'Herman Miller Aeron Chair', 
'Ergonomic office chair, size B, adjustable lumbar support, 12-year warranty', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'Herman Miller', 
'GOODS', 'STOCKABLE', 850.00, 1595.00, 1200.00, 'PCS', 3, 1, 1, 
21.00, 'KG', 66.00, 66.00, 100.00, 'CM', true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BOOKSHELF-001', '5-Tier Bookshelf', 
'Industrial style 5-tier bookshelf, metal frame, wood shelves', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'Industrial Home', 
'GOODS', 'STOCKABLE', 65.00, 149.99, 95.00, 'PCS', 10, 5, 2, 
18.50, 'KG', 180.00, 40.00, 180.00, 'CM', true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TABLE-001', 'Conference Table 8-Person', 
'Boat-shaped conference table, seats 8, cable management', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'Office Pro', 
'GOODS', 'STOCKABLE', 450.00, 899.00, 650.00, 'PCS', 3, 1, 1, 
68.00, 'KG', 244.00, 122.00, 76.00, 'CM', true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WHITEBOARD-001', 'Magnetic Whiteboard 6x4ft', 
'Magnetic dry-erase whiteboard with aluminum frame, wall-mounted', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN'), 'BoardMaster', 
'GOODS', 'STOCKABLE', 85.00, 179.00, 120.00, 'PCS', 8, 4, 2, 
12.00, 'KG', 183.00, 122.00, 2.50, 'CM', true, true, 'ACTIVE');

--changeset easyops:304-insert-expanded-supplies context:test-data
--comment: Insert expanded office supplies
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, brand, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, is_active, status) VALUES

-- Office Supplies
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TONER-001', 'HP LaserJet Toner Cartridge Black', 
'High-yield black toner cartridge, ~10,000 pages', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'HP', 
'GOODS', 'STOCKABLE', 45.00, 89.99, 'PCS', 20, 10, 5, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TONER-002', 'HP LaserJet Toner Cartridge Cyan', 
'High-yield cyan toner cartridge, ~10,000 pages', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'HP', 
'GOODS', 'STOCKABLE', 48.00, 92.99, 'PCS', 15, 8, 4, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'NOTEBOOK-001', 'Moleskine Classic Notebook Large', 
'Hard cover notebook, ruled, 240 pages, black', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'Moleskine', 
'GOODS', 'STOCKABLE', 12.00, 24.99, 'PCS', 50, 20, 10, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'STAPLER-001', 'Heavy Duty Stapler 100-Sheet', 
'Metal heavy-duty stapler, staples up to 100 sheets', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'Swingline', 
'GOODS', 'STOCKABLE', 18.00, 35.99, 'PCS', 25, 10, 5, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CALCULATOR-001', 'Scientific Calculator', 
'12-digit scientific calculator with 240 functions', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'Texas Instruments', 
'GOODS', 'STOCKABLE', 22.00, 45.00, 'PCS', 30, 15, 8, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'LABEL-001', 'Avery Address Labels 1x2.6"', 
'White address labels, 30 per sheet, 2100 labels per pack', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'Avery', 
'GOODS', 'STOCKABLE', 8.50, 18.99, 'PACK', 40, 20, 10, true, true, 'ACTIVE');

--changeset easyops:305-insert-raw-materials context:test-data
--comment: Insert raw materials for manufacturing
INSERT INTO inventory.products (organization_id, sku, barcode, name, description, category_id, brand, manufacturer, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, max_stock_level, safety_stock, weight, weight_unit, track_inventory, track_batch, is_active, status) VALUES

-- Metals & Materials
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'STEEL-001', 'STL-304-SHT-4X8', 'Stainless Steel Sheet 304 Grade', 
'Stainless steel sheet 304 grade, 4x8 feet, 16 gauge', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), NULL, NULL,
'GOODS', 'STOCKABLE', 185.00, 350.00, 'SHEET', 50, 20, 100, 15, 
35.00, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'ALUM-001', 'ALU-6061-ROD-2IN', 'Aluminum Rod 6061 2" Diameter', 
'Aluminum rod 6061-T6, 2" diameter, sold per foot', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), NULL, NULL,
'GOODS', 'STOCKABLE', 15.00, 28.50, 'FOOT', 100, 50, 300, 25, 
2.10, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PLASTIC-001', 'PLS-ABS-PELLET', 'ABS Plastic Pellets', 
'ABS plastic pellets for injection molding, natural color', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), NULL, NULL,
'GOODS', 'STOCKABLE', 2.50, 5.00, 'KG', 500, 200, 2000, 100, 
1.00, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WOOD-001', 'WD-OAK-PLY-4X8', 'Oak Plywood Sheet', 
'Red oak plywood, 4x8 feet, 3/4" thickness, furniture grade', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), NULL, NULL,
'GOODS', 'STOCKABLE', 65.00, 125.00, 'SHEET', 30, 15, 80, 10, 
28.00, 'KG', true, true, true, 'ACTIVE');

--changeset easyops:306-insert-finished-goods context:test-data
--comment: Insert finished goods products
INSERT INTO inventory.products (organization_id, sku, barcode, name, description, category_id, product_type, item_type, cost_price, selling_price, wholesale_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, track_serial, is_active, status) VALUES

-- Manufactured Products
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FIN-WIDGET-001', '123456789012', 'Premium Widget Model X', 
'Precision-engineered widget with titanium coating, model X series', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FIN'), 
'GOODS', 'STOCKABLE', 125.00, 249.99, 180.00, 'PCS', 20, 10, 5, true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FIN-GADGET-001', '123456789013', 'Smart Gadget Pro', 
'IoT-enabled smart gadget with cloud connectivity', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FIN'), 
'GOODS', 'STOCKABLE', 75.00, 149.99, 110.00, 'PCS', 30, 15, 8, true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FIN-ASSEMBLY-001', '123456789014', 'Complete Assembly Unit A1', 
'Ready-to-ship assembly unit, includes all components', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FIN'), 
'GOODS', 'STOCKABLE', 450.00, 899.00, 650.00, 'PCS', 10, 5, 3, true, true, true, 'ACTIVE');

--changeset easyops:307-insert-perishable-items context:test-data
--comment: Insert perishable food & beverage items with expiry tracking
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, track_batch, has_expiry, shelf_life_days, is_active, status) VALUES

-- Food & Beverage (Perishable)
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'COFFEE-001', 'Premium Coffee Beans 1kg', 
'Arabica coffee beans, medium roast, whole beans', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD'), 
'GOODS', 'STOCKABLE', 18.00, 35.00, 'KG', 50, 20, 10, true, true, true, 180, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'MILK-001', 'Organic Whole Milk 1L', 
'Organic whole milk, pasteurized, 3.5% fat', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD'), 
'GOODS', 'STOCKABLE', 1.80, 4.50, 'LTR', 100, 50, 30, true, true, true, 14, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'JUICE-001', 'Orange Juice Fresh 1L', 
'Freshly squeezed orange juice, no preservatives', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD'), 
'GOODS', 'STOCKABLE', 2.50, 6.99, 'LTR', 80, 40, 20, true, true, true, 7, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BREAD-001', 'Whole Wheat Bread Loaf', 
'Artisan whole wheat bread, 800g loaf', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD'), 
'GOODS', 'STOCKABLE', 2.00, 5.50, 'PCS', 60, 30, 15, true, true, true, 5, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHEESE-001', 'Aged Cheddar Cheese 500g', 
'Sharp cheddar cheese, aged 12 months, vacuum packed', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD'), 
'GOODS', 'STOCKABLE', 8.50, 18.99, 'PCS', 40, 20, 10, true, true, true, 90, true, 'ACTIVE');

--changeset easyops:308-insert-chemicals context:test-data
--comment: Insert chemical products with safety tracking
INSERT INTO inventory.products (organization_id, sku, barcode, name, description, short_description, category_id, manufacturer, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, track_batch, is_active, status, hs_code) VALUES

-- Industrial Chemicals
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHEM-001', 'CHM-H2O2-35', 'Hydrogen Peroxide 35% Industrial', 
'Hydrogen peroxide 35% concentration, industrial grade, 5L container', 
'H2O2 35% 5L Industrial',
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-CHEM'), 'ChemSupply Co.', 
'GOODS', 'STOCKABLE', 25.00, 55.00, 'LTR', 40, 20, 10, true, true, true, 'ACTIVE', '28470000'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHEM-002', 'CHM-ISOPROP-99', 'Isopropyl Alcohol 99%', 
'Isopropyl alcohol 99.9% pure, technical grade, 20L drum', 
'IPA 99% 20L Drum',
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-CHEM'), 'ChemSupply Co.', 
'GOODS', 'STOCKABLE', 80.00, 165.00, 'LTR', 25, 10, 5, true, true, true, 'ACTIVE', '29051200'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHEM-003', 'CHM-ACETONE', 'Acetone Technical Grade', 
'Acetone 99.5% pure, technical grade, 5L container', 
'Acetone 99.5% 5L',
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-CHEM'), 'ChemSupply Co.', 
'GOODS', 'STOCKABLE', 18.00, 42.00, 'LTR', 30, 15, 8, true, true, true, 'ACTIVE', '29141100'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'LUBR-001', 'LUB-IND-SAE30', 'Industrial Lubricating Oil SAE 30', 
'SAE 30 industrial lubricating oil, 20L drum', 
'SAE 30 Oil 20L',
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-CHEM'), 'MobilOil', 
'GOODS', 'STOCKABLE', 45.00, 95.00, 'LTR', 35, 15, 10, true, true, true, 'ACTIVE', '27101980');

--changeset easyops:309-insert-packaging-materials context:test-data
--comment: Insert packaging materials
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, is_active, status) VALUES

-- Packaging
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BOX-001', 'Corrugated Box 18x12x10"', 
'Heavy-duty corrugated shipping box, 200lb test', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK'), 
'GOODS', 'STOCKABLE', 1.20, 3.50, 'PCS', 200, 100, 50, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TAPE-001', 'Packing Tape 2" x 110yd', 
'Clear packing tape, 2" width, 110 yard roll, 2.0 mil', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK'), 
'GOODS', 'STOCKABLE', 2.50, 6.99, 'ROLL', 150, 75, 30, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BUBBLE-001', 'Bubble Wrap 12" x 100ft', 
'Bubble wrap roll, 12" wide, 100 feet, 3/16" bubbles', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK'), 
'GOODS', 'STOCKABLE', 12.00, 28.99, 'ROLL', 60, 30, 15, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PALLET-001', 'Wood Pallet 48x40"', 
'Standard wood pallet, 4-way entry, 2500lb capacity', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK'), 
'GOODS', 'STOCKABLE', 15.00, 32.00, 'PCS', 50, 25, 15, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'STRETCH-001', 'Stretch Wrap 18" x 1500ft', 
'Clear stretch wrap film, 18" wide, 1500 feet, 80 gauge', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK'), 
'GOODS', 'STOCKABLE', 22.00, 48.00, 'ROLL', 40, 20, 10, true, true, 'ACTIVE');

--changeset easyops:310-insert-software-licenses context:test-data
--comment: Insert software and license products
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, track_inventory, is_active, status) VALUES

-- Software & Services
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-ADOBE-CC', 'Adobe Creative Cloud All Apps', 
'Annual subscription for Adobe Creative Cloud - all applications', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 
'SERVICE', 'NON_STOCKABLE', 599.88, 839.88, 'LICENSE', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-AUTOCAD', 'AutoCAD LT Annual License', 
'AutoCAD LT annual subscription for 2D drafting', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 
'SERVICE', 'NON_STOCKABLE', 350.00, 550.00, 'LICENSE', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-SLACK-BUS', 'Slack Business+ Annual', 
'Slack Business+ plan, per user annual license', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 
'SERVICE', 'NON_STOCKABLE', 96.00, 150.00, 'LICENSE', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-ZOOM-PRO', 'Zoom Pro Annual License', 
'Zoom Pro plan, annual subscription per host', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 
'SERVICE', 'NON_STOCKABLE', 120.00, 189.00, 'LICENSE', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SW-GITHUB-TEAM', 'GitHub Team Monthly', 
'GitHub Team plan, per user per month', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SOFT'), 
'SERVICE', 'NON_STOCKABLE', 3.00, 4.99, 'LICENSE', false, true, 'ACTIVE');

--changeset easyops:311-insert-service-products context:test-data
--comment: Insert professional service products
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, track_inventory, is_active, status) VALUES

-- Professional Services
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-DEV', 'Software Development Services', 
'Custom software development, hourly rate for senior developer', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 
'SERVICE', 'NON_STOCKABLE', 75.00, 150.00, 'HOUR', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-DESIGN', 'Graphic Design Services', 
'Professional graphic design, hourly rate', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 
'SERVICE', 'NON_STOCKABLE', 50.00, 95.00, 'HOUR', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-TRAINING', 'IT Training Services', 
'On-site IT training, per day rate', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 
'SERVICE', 'NON_STOCKABLE', 400.00, 800.00, 'DAY', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-MAINT', 'Equipment Maintenance Service', 
'Quarterly equipment maintenance service', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 
'SERVICE', 'NON_STOCKABLE', 120.00, 250.00, 'SERVICE', false, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SRV-INSTALL', 'Installation Service', 
'Professional installation service for equipment', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SERV'), 
'SERVICE', 'NON_STOCKABLE', 80.00, 175.00, 'SERVICE', false, true, 'ACTIVE');

--changeset easyops:312-insert-international-products context:test-data
--comment: Insert international products with multi-currency
INSERT INTO inventory.products (organization_id, sku, barcode, name, description, category_id, brand, product_type, item_type, cost_price, selling_price, currency, uom, reorder_level, min_stock_level, safety_stock, track_inventory, country_of_origin, hs_code, is_active, status) VALUES

-- International Products
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'WATCH-001', '7612635117973', 'Swiss Automatic Watch', 
'Swiss-made automatic watch, sapphire crystal, 50m water resistant', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Tissot', 
'GOODS', 'STOCKABLE', 450.00, 899.00, 'USD', 'PCS', 5, 2, 1, true, 'CH', '91011100', true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CAMERA-001', '4549292145830', 'Mirrorless Camera Body', 
'24MP mirrorless camera body, 4K video, in-body stabilization', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Sony', 
'GOODS', 'STOCKABLE', 1200.00, 1998.00, 'USD', 'PCS', 3, 1, 1, true, 'JP', '85258020', true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'LENS-001', '4549292151718', 'Camera Lens 24-70mm F2.8', 
'Professional zoom lens, 24-70mm f/2.8, weather-sealed', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Sony', 
'GOODS', 'STOCKABLE', 1400.00, 2198.00, 'USD', 'PCS', 2, 1, 1, true, 'JP', '90059000', true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TOOL-001', '4006885213500', 'Cordless Drill Kit', 
'18V cordless drill/driver kit with 2 batteries and charger', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC'), 'Bosch', 
'GOODS', 'STOCKABLE', 85.00, 169.00, 'USD', 'PCS', 12, 6, 3, true, 'DE', '84679900', true, 'ACTIVE');

--changeset easyops:313-insert-bulk-materials context:test-data
--comment: Insert bulk materials sold by weight/volume
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, max_stock_level, safety_stock, weight, weight_unit, track_inventory, track_batch, is_active, status) VALUES

-- Bulk Materials
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SAND-001', 'Fine Sand Construction Grade', 
'Fine construction sand, washed and dried', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), 
'GOODS', 'STOCKABLE', 0.05, 0.15, 'KG', 5000, 2000, 20000, 1000, 
1.00, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CEMENT-001', 'Portland Cement Type I', 
'Portland cement type I, 50kg bags', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), 
'GOODS', 'STOCKABLE', 6.50, 14.99, 'BAG', 100, 50, 500, 30, 
50.00, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'GRAVEL-001', 'Gravel 3/4" Crushed Stone', 
'Crushed stone gravel, 3/4" size, sold per ton', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), 
'GOODS', 'STOCKABLE', 15.00, 35.00, 'TON', 50, 20, 200, 15, 
1000.00, 'KG', true, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PAINT-001', 'Interior Paint White 5-Gallon', 
'Premium interior latex paint, pure white, 5-gallon bucket', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW'), 
'GOODS', 'STOCKABLE', 85.00, 165.00, 'BUCKET', 30, 15, 100, 10, 
22.68, 'KG', true, true, true, 'ACTIVE');

--changeset easyops:314-insert-stock-for-new-products context:test-data
--comment: Insert stock levels for newly added products
INSERT INTO inventory.stock (organization_id, product_id, warehouse_id, quantity_on_hand, quantity_allocated, unit_cost, status) VALUES

-- Electronics Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'LAPTOP-002'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 8, 0, 1800.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'PHONE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 15, 2, 800.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'TABLET-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 12, 1, 450.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'MONITOR-002'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 18, 3, 350.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'WEBCAM-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 35, 5, 120.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'HEADSET-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 22, 3, 180.00, 'AVAILABLE'),

-- Furniture Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'DESK-002'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 6, 1, 280.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'CHAIR-002'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 4, 0, 850.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'TABLE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 5, 1, 450.00, 'AVAILABLE'),

-- Supplies Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'TONER-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 45, 0, 45.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'NOTEBOOK-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 85, 10, 12.00, 'AVAILABLE'),

-- Raw Materials Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'STEEL-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 75, 15, 185.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'ALUM-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 250, 30, 15.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'PLASTIC-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 850, 120, 2.50, 'AVAILABLE'),

-- Finished Goods Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'FIN-WIDGET-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 45, 8, 125.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'FIN-GADGET-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 68, 12, 75.00, 'AVAILABLE'),

-- Perishables Stock (Lower quantities due to shelf life)
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'COFFEE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 65, 0, 18.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'MILK-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 120, 15, 1.80, 'AVAILABLE'),

-- Chemicals Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'CHEM-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 55, 5, 25.00, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'CHEM-002'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 40, 8, 80.00, 'AVAILABLE'),

-- Packaging Stock
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'BOX-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 350, 50, 1.20, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'TAPE-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 220, 30, 2.50, 'AVAILABLE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 (SELECT id FROM inventory.products WHERE sku = 'PALLET-001'),
 (SELECT id FROM inventory.warehouses WHERE code = 'WH-MAIN'), 95, 15, 15.00, 'AVAILABLE');

--changeset easyops:315-insert-product-variants context:test-data
--comment: Insert product variants (sizes, colors)
INSERT INTO inventory.products (organization_id, sku, name, description, category_id, brand, product_type, item_type, cost_price, selling_price, uom, reorder_level, min_stock_level, safety_stock, track_inventory, is_active, status) VALUES

-- T-Shirt Variants (Example of variants)
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TSHIRT-BLK-S', 'Company T-Shirt Black Small', 
'Company branded t-shirt, 100% cotton, black, size S', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'CompanyBrand', 
'GOODS', 'STOCKABLE', 8.00, 19.99, 'PCS', 30, 15, 10, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TSHIRT-BLK-M', 'Company T-Shirt Black Medium', 
'Company branded t-shirt, 100% cotton, black, size M', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'CompanyBrand', 
'GOODS', 'STOCKABLE', 8.00, 19.99, 'PCS', 50, 25, 15, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TSHIRT-BLK-L', 'Company T-Shirt Black Large', 
'Company branded t-shirt, 100% cotton, black, size L', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'CompanyBrand', 
'GOODS', 'STOCKABLE', 8.00, 19.99, 'PCS', 50, 25, 15, true, true, 'ACTIVE'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'TSHIRT-WHT-M', 'Company T-Shirt White Medium', 
'Company branded t-shirt, 100% cotton, white, size M', 
(SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP'), 'CompanyBrand', 
'GOODS', 'STOCKABLE', 8.00, 19.99, 'PCS', 50, 25, 15, true, true, 'ACTIVE');

