-- ============================================================
-- TEST DATA FOR DEVELOPMENT
-- Sample Customers, Vendors, Products, and other test data
-- ============================================================

-- Get the default organization ID
DO $$ 
DECLARE
    v_org_id UUID;
    v_admin_id UUID;
    v_customer1_id UUID;
    v_customer2_id UUID;
    v_customer3_id UUID;
    v_vendor1_id UUID;
    v_vendor2_id UUID;
    v_vendor3_id UUID;
BEGIN
    -- Get default organization
    SELECT id INTO v_org_id FROM admin.organizations WHERE code = 'DEMO_ORG' LIMIT 1;
    
    -- Get admin user ID
    SELECT id INTO v_admin_id FROM users.users WHERE username = 'admin' LIMIT 1;
    
    IF v_org_id IS NULL THEN
        RAISE NOTICE 'Default organization not found. Skipping test data.';
        RETURN;
    END IF;

    -- ============================================================
    -- TEST CUSTOMERS
    -- ============================================================
    
    INSERT INTO accounting.customers (id, organization_id, customer_code, customer_name, email, phone, credit_limit, current_balance, payment_terms, is_active)
    VALUES 
    (uuid_generate_v4(), v_org_id, 'CUST-001', 'Acme Corporation', 'accounting@acme-corp.com', '+1-555-0101', 50000.00, 0.00, 30, true),
    (uuid_generate_v4(), v_org_id, 'CUST-002', 'TechStart Inc.', 'billing@techstart.io', '+1-555-0102', 25000.00, 0.00, 15, true),
    (uuid_generate_v4(), v_org_id, 'CUST-003', 'Global Retail Partners', 'payments@globalretail.com', '+1-555-0103', 75000.00, 0.00, 45, true),
    (uuid_generate_v4(), v_org_id, 'CUST-004', 'Metro Construction Ltd', 'finance@metroconstruction.com', '+1-555-0104', 100000.00, 0.00, 60, true),
    (uuid_generate_v4(), v_org_id, 'CUST-005', 'Sunrise Healthcare', 'accounts@sunrisehealthcare.com', '+1-555-0105', 30000.00, 0.00, 30, true),
    (uuid_generate_v4(), v_org_id, 'CUST-006', 'Digital Marketing Solutions', 'billing@digitalmarketing.com', '+1-555-0106', 15000.00, 0.00, 15, true),
    (uuid_generate_v4(), v_org_id, 'CUST-007', 'Foodie Restaurant Group', 'accounting@foodiegroup.com', '+1-555-0107', 20000.00, 0.00, 30, true),
    (uuid_generate_v4(), v_org_id, 'CUST-008', 'AutoParts Direct', 'billing@autopartsdirect.com', '+1-555-0108', 40000.00, 0.00, 30, true),
    (uuid_generate_v4(), v_org_id, 'CUST-009', 'Elite Fashion Boutique', 'payments@elitefashion.com', '+1-555-0109', 12000.00, 0.00, 15, true),
    (uuid_generate_v4(), v_org_id, 'CUST-010', 'Green Energy Solutions', 'finance@greenenergy.com', '+1-555-0110', 60000.00, 0.00, 45, true);
    
    RAISE NOTICE 'Inserted 10 test customers into accounting.customers';

    -- ============================================================
    -- TEST CUSTOMERS (for Sales Module)
    -- ============================================================
    
    INSERT INTO sales.customers (id, organization_id, customer_code, customer_name, email, phone, contact_person, billing_address, shipping_address, credit_limit, current_balance, payment_terms, is_active)
    VALUES 
    (uuid_generate_v4(), v_org_id, 'CUST-001', 'Acme Corporation', 'accounting@acme-corp.com', '+1-555-0101', 'John Smith', '123 Business Street, New York, NY 10001', '123 Business Street, New York, NY 10001', 50000.00, 0.00, 'Net 30', true),
    (uuid_generate_v4(), v_org_id, 'CUST-002', 'TechStart Inc.', 'billing@techstart.io', '+1-555-0102', 'Jane Doe', '456 Innovation Ave, San Francisco, CA 94102', '456 Innovation Ave, San Francisco, CA 94102', 25000.00, 0.00, 'Net 15', true),
    (uuid_generate_v4(), v_org_id, 'CUST-003', 'Global Retail Partners', 'payments@globalretail.com', '+1-555-0103', 'Bob Johnson', '789 Commerce Blvd, Chicago, IL 60601', '789 Commerce Blvd, Chicago, IL 60601', 75000.00, 0.00, 'Net 45', true),
    (uuid_generate_v4(), v_org_id, 'CUST-004', 'Metro Construction Ltd', 'finance@metroconstruction.com', '+1-555-0104', 'Alice Williams', '321 Industrial Way, Houston, TX 77002', '321 Industrial Way, Houston, TX 77002', 100000.00, 0.00, 'Net 60', true),
    (uuid_generate_v4(), v_org_id, 'CUST-005', 'Sunrise Healthcare', 'accounts@sunrisehealthcare.com', '+1-555-0105', 'Tom Brown', '654 Medical Center Dr, Boston, MA 02101', '654 Medical Center Dr, Boston, MA 02101', 30000.00, 0.00, 'Net 30', true),
    (uuid_generate_v4(), v_org_id, 'CUST-006', 'Digital Marketing Solutions', 'billing@digitalmarketing.com', '+1-555-0106', 'Emma Davis', '987 Marketing Plaza, Los Angeles, CA 90001', '987 Marketing Plaza, Los Angeles, CA 90001', 15000.00, 0.00, 'Net 15', true),
    (uuid_generate_v4(), v_org_id, 'CUST-007', 'Foodie Restaurant Group', 'accounting@foodiegroup.com', '+1-555-0107', 'Chris Wilson', '246 Restaurant Row, Miami, FL 33101', '246 Restaurant Row, Miami, FL 33101', 20000.00, 0.00, 'Net 30', true),
    (uuid_generate_v4(), v_org_id, 'CUST-008', 'AutoParts Direct', 'billing@autopartsdirect.com', '+1-555-0108', 'Lisa Garcia', '135 Auto Mall Dr, Detroit, MI 48201', '135 Auto Mall Dr, Detroit, MI 48201', 40000.00, 0.00, 'Net 30', true),
    (uuid_generate_v4(), v_org_id, 'CUST-009', 'Elite Fashion Boutique', 'payments@elitefashion.com', '+1-555-0109', 'Mark Martinez', '864 Fashion Ave, New York, NY 10018', '864 Fashion Ave, New York, NY 10018', 12000.00, 0.00, 'Net 15', true),
    (uuid_generate_v4(), v_org_id, 'CUST-010', 'Green Energy Solutions', 'finance@greenenergy.com', '+1-555-0110', 'Sarah Anderson', '753 Renewable Way, Seattle, WA 98101', '753 Renewable Way, Seattle, WA 98101', 60000.00, 0.00, 'Net 45', true);
    
    RAISE NOTICE 'Inserted 10 test customers into sales.customers';

    -- ============================================================
    -- TEST VENDORS/SUPPLIERS
    -- ============================================================
    
    INSERT INTO accounting.vendors (id, organization_id, vendor_code, vendor_name, email, phone, payment_terms, tax_id, is_active)
    VALUES 
    (uuid_generate_v4(), v_org_id, 'VEND-001', 'Office Supplies Co.', 'sales@officesupplies.com', '+1-555-0201', 30, '12-3456789', true),
    (uuid_generate_v4(), v_org_id, 'VEND-002', 'Tech Components Ltd', 'orders@techcomponents.com', '+1-555-0202', 45, '23-4567890', true),
    (uuid_generate_v4(), v_org_id, 'VEND-003', 'Industrial Equipment Inc', 'sales@industrialequip.com', '+1-555-0203', 60, '34-5678901', true),
    (uuid_generate_v4(), v_org_id, 'VEND-004', 'Marketing Services Group', 'billing@marketingservices.com', '+1-555-0204', 15, '45-6789012', true),
    (uuid_generate_v4(), v_org_id, 'VEND-005', 'Cloud Hosting Provider', 'accounts@cloudhosting.com', '+1-555-0205', 30, '56-7890123', true),
    (uuid_generate_v4(), v_org_id, 'VEND-006', 'Legal Advisory Services', 'billing@legaladvisory.com', '+1-555-0206', 15, '67-8901234', true),
    (uuid_generate_v4(), v_org_id, 'VEND-007', 'Shipping & Logistics Co', 'accounts@shippinglogistics.com', '+1-555-0207', 30, '78-9012345', true),
    (uuid_generate_v4(), v_org_id, 'VEND-008', 'Professional Cleaning Services', 'billing@procleaners.com', '+1-555-0208', 30, '89-0123456', true),
    (uuid_generate_v4(), v_org_id, 'VEND-009', 'Insurance Brokers LLC', 'payments@insurancebrokers.com', '+1-555-0209', 45, '90-1234567', true),
    (uuid_generate_v4(), v_org_id, 'VEND-010', 'Raw Materials Supplier', 'sales@rawmaterials.com', '+1-555-0210', 60, '01-2345678', true);
    
    RAISE NOTICE 'Inserted 10 test vendors';

    -- ============================================================
    -- TEST PRODUCTS (for Sales module)
    -- ============================================================
    
    INSERT INTO sales.products (id, organization_id, product_code, product_name, description, product_type, category, unit_of_measure, unit_price, cost_price, tax_rate, track_inventory, is_active)
    VALUES 
    (uuid_generate_v4(), v_org_id, 'PROD-001', 'Enterprise Software License', 'Annual software license for enterprise users', 'SERVICE', 'Software', 'License', 1999.00, 500.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-002', 'Professional Services - Hourly', 'Professional consulting services per hour', 'SERVICE', 'Services', 'Hour', 150.00, 75.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-003', 'Cloud Storage - 1TB', 'Monthly cloud storage subscription 1TB', 'SERVICE', 'Subscription', 'Month', 29.99, 10.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-004', 'Hardware - Desktop Computer', 'High-performance desktop computer', 'GOODS', 'Hardware', 'Unit', 1299.00, 800.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-005', 'Hardware - Laptop', 'Business laptop with warranty', 'GOODS', 'Hardware', 'Unit', 899.00, 600.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-006', 'Training Course - Basic', '2-day basic training course', 'SERVICE', 'Training', 'Course', 499.00, 200.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-007', 'Training Course - Advanced', '3-day advanced training course', 'SERVICE', 'Training', 'Course', 799.00, 300.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-008', 'Support Contract - Gold', 'Annual gold support contract 24/7', 'SERVICE', 'Support', 'Year', 2499.00, 800.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-009', 'Support Contract - Silver', 'Annual silver support contract business hours', 'SERVICE', 'Support', 'Year', 1499.00, 500.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-010', 'Custom Development - Project', 'Custom software development project', 'SERVICE', 'Services', 'Project', 5000.00, 2500.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-011', 'Network Equipment - Router', 'Enterprise-grade network router', 'GOODS', 'Hardware', 'Unit', 599.00, 350.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-012', 'Network Equipment - Switch', '24-port managed network switch', 'GOODS', 'Hardware', 'Unit', 399.00, 250.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-013', 'Security Software - Antivirus', 'Annual antivirus subscription per device', 'SERVICE', 'Software', 'Year', 49.99, 20.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-014', 'Security Software - Firewall', 'Annual firewall subscription', 'SERVICE', 'Software', 'Year', 199.00, 80.00, 0.00, false, true),
    (uuid_generate_v4(), v_org_id, 'PROD-015', 'Office Furniture - Desk', 'Ergonomic office desk', 'GOODS', 'Furniture', 'Unit', 349.00, 200.00, 8.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-016', 'Office Furniture - Chair', 'Ergonomic office chair', 'GOODS', 'Furniture', 'Unit', 249.00, 150.00, 8.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-017', 'Printer - Laser', 'Color laser printer', 'GOODS', 'Hardware', 'Unit', 449.00, 300.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-018', 'Printer Supplies - Toner', 'Replacement toner cartridge', 'GOODS', 'Supplies', 'Unit', 89.00, 45.00, 8.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-019', 'Monitor - 27 inch', '27 inch 4K monitor', 'GOODS', 'Hardware', 'Unit', 329.00, 200.00, 10.00, true, true),
    (uuid_generate_v4(), v_org_id, 'PROD-020', 'Keyboard and Mouse Set', 'Wireless keyboard and mouse combo', 'GOODS', 'Hardware', 'Set', 59.00, 30.00, 8.00, true, true);
    
    RAISE NOTICE 'Inserted 20 test products';

    -- ============================================================
    -- TEST DEPARTMENTS
    -- ============================================================
    
    INSERT INTO admin.departments (organization_id, code, name, description, type, cost_center, status)
    VALUES 
    (v_org_id, 'EXEC', 'Executive', 'Executive Management', 'DEPARTMENT', 'CC-1000', 'ACTIVE'),
    (v_org_id, 'FIN', 'Finance', 'Finance and Accounting', 'DEPARTMENT', 'CC-2000', 'ACTIVE'),
    (v_org_id, 'HR', 'Human Resources', 'Human Resources Management', 'DEPARTMENT', 'CC-3000', 'ACTIVE'),
    (v_org_id, 'IT', 'Information Technology', 'IT and Systems', 'DEPARTMENT', 'CC-4000', 'ACTIVE'),
    (v_org_id, 'SALES', 'Sales', 'Sales and Business Development', 'DEPARTMENT', 'CC-5000', 'ACTIVE'),
    (v_org_id, 'MKT', 'Marketing', 'Marketing and Communications', 'DEPARTMENT', 'CC-6000', 'ACTIVE'),
    (v_org_id, 'OPS', 'Operations', 'Operations and Logistics', 'DEPARTMENT', 'CC-7000', 'ACTIVE'),
    (v_org_id, 'CS', 'Customer Support', 'Customer Service and Support', 'DEPARTMENT', 'CC-8000', 'ACTIVE'),
    (v_org_id, 'RND', 'Research & Development', 'Research and Development', 'DEPARTMENT', 'CC-9000', 'ACTIVE'),
    (v_org_id, 'QA', 'Quality Assurance', 'Quality Control and Testing', 'DEPARTMENT', 'CC-9100', 'ACTIVE');
    
    RAISE NOTICE 'Inserted 10 test departments';

    -- ============================================================
    -- TEST LOCATIONS
    -- ============================================================
    
    INSERT INTO admin.locations (organization_id, code, name, type, address_line1, city, state, postal_code, country, phone, timezone, status)
    VALUES 
    (v_org_id, 'HQ', 'Headquarters', 'HEADQUARTERS', '123 Business Park Drive', 'New York', 'NY', '10001', 'US', '+1-555-1000', 'America/New_York', 'ACTIVE'),
    (v_org_id, 'WEST', 'West Coast Office', 'BRANCH', '456 Tech Boulevard', 'San Francisco', 'CA', '94102', 'US', '+1-555-2000', 'America/Los_Angeles', 'ACTIVE'),
    (v_org_id, 'SOUTH', 'Southern Regional Office', 'BRANCH', '789 Commerce Street', 'Austin', 'TX', '78701', 'US', '+1-555-3000', 'America/Chicago', 'ACTIVE'),
    (v_org_id, 'WH1', 'Main Warehouse', 'WAREHOUSE', '321 Industrial Way', 'Newark', 'NJ', '07102', 'US', '+1-555-4000', 'America/New_York', 'ACTIVE'),
    (v_org_id, 'WH2', 'West Warehouse', 'WAREHOUSE', '654 Distribution Center', 'Los Angeles', 'CA', '90001', 'US', '+1-555-5000', 'America/Los_Angeles', 'ACTIVE');
    
    RAISE NOTICE 'Inserted 5 test locations';

    -- ============================================================
    -- TEST ADDITIONAL USERS
    -- ============================================================
    
    -- Finance Manager
    INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
    VALUES ('finance.manager', 'finance.manager@easyops.com', 'TEMP_PLAIN_TEXT_Password123!', 'Sarah', 'Johnson', true, true)
    ON CONFLICT (username) DO NOTHING;
    
    -- Sales Manager
    INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
    VALUES ('sales.manager', 'sales.manager@easyops.com', 'TEMP_PLAIN_TEXT_Password123!', 'Michael', 'Chen', true, true)
    ON CONFLICT (username) DO NOTHING;
    
    -- Accountant
    INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
    VALUES ('accountant', 'accountant@easyops.com', 'TEMP_PLAIN_TEXT_Password123!', 'Emily', 'Davis', true, true)
    ON CONFLICT (username) DO NOTHING;
    
    -- Sales Representative
    INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
    VALUES ('sales.rep', 'sales.rep@easyops.com', 'TEMP_PLAIN_TEXT_Password123!', 'David', 'Wilson', true, true)
    ON CONFLICT (username) DO NOTHING;
    
    -- Warehouse Manager
    INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified)
    VALUES ('warehouse.manager', 'warehouse@easyops.com', 'TEMP_PLAIN_TEXT_Password123!', 'James', 'Brown', true, true)
    ON CONFLICT (username) DO NOTHING;
    
    RAISE NOTICE 'Inserted 5 additional test users';
    
    -- Assign test users to default organization
    INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
    SELECT u.id, v_org_id, 'MEMBER', false
    FROM users.users u
    WHERE u.username IN ('finance.manager', 'sales.manager', 'accountant', 'sales.rep', 'warehouse.manager')
    ON CONFLICT (user_id, organization_id) DO NOTHING;
    
    RAISE NOTICE 'Assigned test users to organization';
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'TEST DATA LOADING COMPLETE';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Test Customers (AR): 10';
    RAISE NOTICE 'Test Customers (Sales): 10';
    RAISE NOTICE 'Test Vendors: 10';
    RAISE NOTICE 'Test Products: 20';
    RAISE NOTICE 'Test Departments: 10';
    RAISE NOTICE 'Test Locations: 5';
    RAISE NOTICE 'Test Users: 5 additional';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'DEFAULT LOGIN CREDENTIALS:';
    RAISE NOTICE '  Username: admin';
    RAISE NOTICE '  Password: Admin123!';
    RAISE NOTICE '';
    RAISE NOTICE 'OTHER TEST USERS (all with password: Password123!):';
    RAISE NOTICE '  - finance.manager@easyops.com';
    RAISE NOTICE '  - sales.manager@easyops.com';
    RAISE NOTICE '  - accountant@easyops.com';
    RAISE NOTICE '  - sales.rep@easyops.com';
    RAISE NOTICE '  - warehouse.manager@easyops.com';
    RAISE NOTICE '========================================';

END $$;

