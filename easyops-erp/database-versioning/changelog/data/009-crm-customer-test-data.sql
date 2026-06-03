--liquibase formatted sql

--changeset easyops:401-insert-crm-test-accounts context:test-data
--comment: Insert comprehensive test customer accounts for CRM module
INSERT INTO crm.accounts (
    organization_id, account_number, account_name, account_type, industry,
    phone, email, website,
    billing_street, billing_city, billing_state, billing_postal_code, billing_country,
    shipping_street, shipping_city, shipping_state, shipping_postal_code, shipping_country,
    company_size, annual_revenue, number_of_employees,
    rating, is_active, customer_since, tax_id, description
) VALUES

-- Large Enterprise Customers
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-001', 'Tech Solutions Global Inc.', 'CUSTOMER', 'Technology',
 '+1-555-0101', 'contact@techsolutions.com', 'www.techsolutions.com',
 '100 Silicon Valley Blvd, Suite 500', 'San Francisco', 'California', '94105', 'USA',
 '100 Silicon Valley Blvd, Suite 500', 'San Francisco', 'California', '94105', 'USA',
 'LARGE', 50000000.00, 500,
 'HOT', true, '2020-01-15', '94-1234567', 
 'Leading technology solutions provider specializing in enterprise software and cloud services'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-002', 'Global Manufacturing Corp', 'CUSTOMER', 'Manufacturing',
 '+1-555-0102', 'purchasing@globalmfg.com', 'www.globalmfg.com',
 '5000 Industrial Parkway', 'Detroit', 'Michigan', '48201', 'USA',
 '5001 Warehouse Road', 'Detroit', 'Michigan', '48202', 'USA',
 'LARGE', 120000000.00, 1200,
 'HOT', true, '2019-06-20', '38-7654321', 
 'Automotive parts manufacturer with operations across North America'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-003', 'Healthcare Systems Inc', 'CUSTOMER', 'Healthcare',
 '+1-555-0103', 'procurement@healthsys.com', 'www.healthcaresystems.com',
 '200 Medical Center Drive', 'Boston', 'Massachusetts', '02118', 'USA',
 '200 Medical Center Drive', 'Boston', 'Massachusetts', '02118', 'USA',
 'LARGE', 85000000.00, 800,
 'WARM', true, '2021-03-10', '04-9876543', 
 'Hospital network providing comprehensive healthcare services'),

-- Medium Business Customers
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-004', 'Retail Solutions LLC', 'CUSTOMER', 'Retail',
 '+1-555-0104', 'orders@retailsolutions.com', 'www.retailsolutions.com',
 '450 Commerce Street', 'Chicago', 'Illinois', '60601', 'USA',
 '450 Commerce Street', 'Chicago', 'Illinois', '60601', 'USA',
 'MEDIUM', 15000000.00, 150,
 'WARM', true, '2021-08-15', '36-5551234', 
 'Multi-location retail chain specializing in electronics and appliances'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-005', 'Education Partners Ltd', 'CUSTOMER', 'Education',
 '+1-555-0105', 'admin@edpartners.edu', 'www.educationpartners.edu',
 '789 University Avenue', 'Seattle', 'Washington', '98101', 'USA',
 '789 University Avenue', 'Seattle', 'Washington', '98101', 'USA',
 'MEDIUM', 8500000.00, 120,
 'WARM', true, '2022-01-20', '91-3334444', 
 'Educational institution providing K-12 and adult learning programs'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-006', 'Construction & Engineering Co', 'CUSTOMER', 'Construction',
 '+1-555-0106', 'projects@constructco.com', 'www.constructioneng.com',
 '1500 Builder Lane', 'Houston', 'Texas', '77002', 'USA',
 '1500 Builder Lane', 'Houston', 'Texas', '77002', 'USA',
 'MEDIUM', 22000000.00, 180,
 'HOT', true, '2020-11-05', '74-2223333', 
 'Commercial and residential construction with 25 years of experience'),

-- Small Business Customers
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-007', 'Green Valley Restaurant Group', 'CUSTOMER', 'Food & Beverage',
 '+1-555-0107', 'purchasing@greenvalley.com', 'www.greenvalleyrestaurants.com',
 '88 Main Street', 'Portland', 'Oregon', '97201', 'USA',
 '88 Main Street', 'Portland', 'Oregon', '97201', 'USA',
 'SMALL', 3500000.00, 45,
 'WARM', true, '2022-05-12', '93-4445555', 
 'Family-owned restaurant chain with 5 locations in Oregon'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-008', 'Legal Advisors Professional Corp', 'CUSTOMER', 'Legal Services',
 '+1-555-0108', 'office@legaladvisors.com', 'www.legaladvisorspro.com',
 '1200 Law Plaza, Floor 15', 'New York', 'New York', '10005', 'USA',
 '1200 Law Plaza, Floor 15', 'New York', 'New York', '10005', 'USA',
 'SMALL', 5000000.00, 35,
 'WARM', true, '2021-09-18', '13-6667777', 
 'Law firm specializing in corporate law and intellectual property'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-009', 'Digital Marketing Agency', 'CUSTOMER', 'Marketing',
 '+1-555-0109', 'hello@digitalmarketing.agency', 'www.digitalmarketingpro.com',
 '300 Creative Way', 'Austin', 'Texas', '78701', 'USA',
 '300 Creative Way', 'Austin', 'Texas', '78701', 'USA',
 'SMALL', 2800000.00, 28,
 'HOT', true, '2023-02-14', '74-8889999', 
 'Full-service digital marketing agency serving SMBs and startups'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-010', 'Fitness & Wellness Center', 'CUSTOMER', 'Health & Fitness',
 '+1-555-0110', 'info@fitnesswellness.com', 'www.fitnesswellness.com',
 '500 Health Boulevard', 'Denver', 'Colorado', '80202', 'USA',
 '500 Health Boulevard', 'Denver', 'Colorado', '80202', 'USA',
 'SMALL', 1200000.00, 22,
 'WARM', true, '2022-11-30', '84-1112222', 
 'Modern fitness center with spa, yoga, and personal training services'),

-- Prospects (Potential Customers)
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-011', 'Future Tech Innovations', 'PROSPECT', 'Technology',
 '+1-555-0111', 'sales@futuretech.io', 'www.futuretechinnovations.io',
 '2500 Innovation Drive', 'San Jose', 'California', '95110', 'USA',
 '2500 Innovation Drive', 'San Jose', 'California', '95110', 'USA',
 'MEDIUM', 12000000.00, 95,
 'HOT', true, NULL, '94-3334444', 
 'AI and machine learning startup, currently in evaluation phase'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-012', 'Eco-Friendly Products Co', 'PROSPECT', 'Consumer Goods',
 '+1-555-0112', 'contact@ecofriendly.com', 'www.ecofriendlyproducts.com',
 '750 Sustainable Ave', 'Portland', 'Oregon', '97202', 'USA',
 '750 Sustainable Ave', 'Portland', 'Oregon', '97202', 'USA',
 'SMALL', 4500000.00, 38,
 'WARM', true, NULL, '93-5556666', 
 'Sustainable products manufacturer, exploring partnership opportunities'),

-- Partners
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-013', 'Logistics Partners International', 'PARTNER', 'Logistics',
 '+1-555-0113', 'partnerships@logisticsintl.com', 'www.logisticspartners.com',
 '3000 Freight Terminal Road', 'Memphis', 'Tennessee', '38103', 'USA',
 '3000 Freight Terminal Road', 'Memphis', 'Tennessee', '38103', 'USA',
 'LARGE', 75000000.00, 650,
 'HOT', true, '2020-04-22', '47-7778888', 
 'Strategic logistics partner for distribution and warehousing'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-014', 'Software Integration Services', 'PARTNER', 'Technology',
 '+1-555-0114', 'partner@softwareintegration.com', 'www.softwareintegration.com',
 '1800 Developer Plaza', 'Seattle', 'Washington', '98102', 'USA',
 '1800 Developer Plaza', 'Seattle', 'Washington', '98102', 'USA',
 'MEDIUM', 18000000.00, 125,
 'HOT', true, '2021-07-10', '91-9990000', 
 'Technology partner providing API integration and consulting services');

--changeset easyops:402-insert-crm-test-contacts context:test-data
--comment: Insert test contacts for CRM accounts
INSERT INTO crm.contacts (
    organization_id, account_id, first_name, last_name, job_title, department,
    email, phone, mobile,
    street, city, state, postal_code, country,
    is_primary, is_active, notes
) VALUES

-- Contacts for Tech Solutions Global
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-001'),
 'John', 'Anderson', 'CTO', 'Technology',
 'john.anderson@techsolutions.com', '+1-555-0101', '+1-555-0121',
 '100 Silicon Valley Blvd', 'San Francisco', 'CA', '94105', 'USA',
 true, true, 'Primary technical decision maker'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-001'),
 'Sarah', 'Mitchell', 'VP of Procurement', 'Purchasing',
 'sarah.mitchell@techsolutions.com', '+1-555-0101', '+1-555-0122',
 '100 Silicon Valley Blvd', 'San Francisco', 'CA', '94105', 'USA',
 false, true, 'Handles purchasing and vendor relationships'),

-- Contacts for Global Manufacturing
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-002'),
 'Michael', 'Roberts', 'Procurement Director', 'Supply Chain',
 'michael.roberts@globalmfg.com', '+1-555-0102', '+1-555-0123',
 '5000 Industrial Parkway', 'Detroit', 'MI', '48201', 'USA',
 true, true, 'Main contact for all purchasing activities'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-002'),
 'Jennifer', 'Lee', 'Plant Manager', 'Operations',
 'jennifer.lee@globalmfg.com', '+1-555-0102', '+1-555-0124',
 '5000 Industrial Parkway', 'Detroit', 'MI', '48201', 'USA',
 false, true, 'Responsible for manufacturing operations'),

-- Contacts for Healthcare Systems
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-003'),
 'Dr. Emily', 'Chen', 'Chief of Operations', 'Administration',
 'emily.chen@healthsys.com', '+1-555-0103', '+1-555-0125',
 '200 Medical Center Drive', 'Boston', 'MA', '02118', 'USA',
 true, true, 'Oversees hospital procurement and operations'),

-- Contacts for Retail Solutions
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-004'),
 'David', 'Thompson', 'Purchasing Manager', 'Procurement',
 'david.thompson@retailsolutions.com', '+1-555-0104', '+1-555-0126',
 '450 Commerce Street', 'Chicago', 'IL', '60601', 'USA',
 true, true, 'Manages inventory and supplier relationships'),

-- Contacts for Education Partners
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-005'),
 'Patricia', 'Williams', 'Director of Finance', 'Finance',
 'patricia.williams@edpartners.edu', '+1-555-0105', '+1-555-0127',
 '789 University Avenue', 'Seattle', 'WA', '98101', 'USA',
 true, true, 'Handles budgets and purchasing approvals'),

-- Contacts for Construction & Engineering
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-006'),
 'Robert', 'Johnson', 'Project Manager', 'Projects',
 'robert.johnson@constructco.com', '+1-555-0106', '+1-555-0128',
 '1500 Builder Lane', 'Houston', 'TX', '77002', 'USA',
 true, true, 'Manages large construction projects and procurement'),

-- Contacts for Restaurant Group
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-007'),
 'Maria', 'Garcia', 'Owner', 'Management',
 'maria@greenvalley.com', '+1-555-0107', '+1-555-0129',
 '88 Main Street', 'Portland', 'OR', '97201', 'USA',
 true, true, 'Owner and primary decision maker for all restaurant operations'),

-- Contacts for Legal Advisors
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-008'),
 'James', 'Wilson', 'Senior Partner', 'Management',
 'james.wilson@legaladvisors.com', '+1-555-0108', '+1-555-0130',
 '1200 Law Plaza', 'New York', 'NY', '10005', 'USA',
 true, true, 'Senior partner managing firm operations'),

-- Contacts for Digital Marketing Agency
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-009'),
 'Lisa', 'Brown', 'Creative Director', 'Creative',
 'lisa.brown@digitalmarketing.agency', '+1-555-0109', '+1-555-0131',
 '300 Creative Way', 'Austin', 'TX', '78701', 'USA',
 true, true, 'Leads creative team and client relationships'),

-- Contacts for Fitness Center
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-010'),
 'Kevin', 'Martinez', 'General Manager', 'Operations',
 'kevin.martinez@fitnesswellness.com', '+1-555-0110', '+1-555-0132',
 '500 Health Boulevard', 'Denver', 'CO', '80202', 'USA',
 true, true, 'General manager responsible for facility operations'),

-- Contacts for Prospects
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-011'),
 'Amanda', 'Taylor', 'VP of Engineering', 'Engineering',
 'amanda.taylor@futuretech.io', '+1-555-0111', '+1-555-0133',
 '2500 Innovation Drive', 'San Jose', 'CA', '95110', 'USA',
 true, true, 'Evaluating solutions for upcoming project'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-012'),
 'Christopher', 'Davis', 'CEO', 'Executive',
 'chris.davis@ecofriendly.com', '+1-555-0112', '+1-555-0134',
 '750 Sustainable Ave', 'Portland', 'OR', '97202', 'USA',
 true, true, 'Founder and CEO exploring partnership opportunities');

--changeset easyops:403-insert-additional-accounts context:test-data
--comment: Insert additional diverse customer accounts
INSERT INTO crm.accounts (
    organization_id, account_number, account_name, account_type, industry,
    phone, email, website,
    billing_street, billing_city, billing_state, billing_postal_code, billing_country,
    company_size, annual_revenue, number_of_employees,
    rating, is_active, customer_since, tax_id
) VALUES

-- Tech Companies
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-015', 'Cloud Computing Services Ltd', 'CUSTOMER', 'Technology',
 '+1-555-0115', 'sales@cloudcompute.com', 'www.cloudcomputing.com',
 '400 Data Center Pkwy', 'Santa Clara', 'CA', '95054', 'USA',
 'MEDIUM', 32000000.00, 210,
 'HOT', true, '2022-03-15', '94-1119999'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-016', 'Mobile App Developers Inc', 'CUSTOMER', 'Technology',
 '+1-555-0116', 'contact@mobileapps.dev', 'www.mobileappdev.com',
 '250 App Street', 'San Francisco', 'CA', '94107', 'USA',
 'SMALL', 4200000.00, 42,
 'WARM', true, '2023-06-20', '94-2220000'),

-- Financial Services
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-017', 'Premier Financial Group', 'CUSTOMER', 'Financial Services',
 '+1-555-0117', 'corporate@premierfinancial.com', 'www.premierfinancial.com',
 '1000 Wall Street', 'New York', 'NY', '10005', 'USA',
 'LARGE', 95000000.00, 420,
 'HOT', true, '2019-02-10', '13-3331111'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-018', 'Community Credit Union', 'CUSTOMER', 'Financial Services',
 '+1-555-0118', 'membership@communitycu.org', 'www.communitycu.org',
 '600 Banking Plaza', 'Phoenix', 'AZ', '85001', 'USA',
 'MEDIUM', 18000000.00, 85,
 'WARM', true, '2021-05-18', '86-4442222'),

-- Retail & E-commerce
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-019', 'Online Marketplace Solutions', 'CUSTOMER', 'E-commerce',
 '+1-555-0119', 'support@onlinemarketplace.com', 'www.onlinemarketplace.com',
 '900 E-commerce Way', 'Seattle', 'WA', '98109', 'USA',
 'MEDIUM', 28000000.00, 175,
 'HOT', true, '2020-08-25', '91-5553333'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-020', 'Fashion Boutique Chain', 'CUSTOMER', 'Retail',
 '+1-555-0120', 'corporate@fashionboutique.com', 'www.fashionboutique.com',
 '1500 Fashion Avenue', 'Los Angeles', 'CA', '90015', 'USA',
 'SMALL', 6800000.00, 65,
 'WARM', true, '2022-10-12', '95-6664444'),

-- Manufacturing
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-021', 'Precision Parts Manufacturing', 'CUSTOMER', 'Manufacturing',
 '+1-555-0121', 'sales@precisionparts.com', 'www.precisionparts.com',
 '2200 Factory Road', 'Milwaukee', 'WI', '53201', 'USA',
 'MEDIUM', 24000000.00, 160,
 'HOT', true, '2020-12-03', '55-7775555'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-022', 'Electronics Assembly Corp', 'CUSTOMER', 'Manufacturing',
 '+1-555-0122', 'procurement@electronicsassembly.com', 'www.electronicsassembly.com',
 '800 Assembly Line Blvd', 'San Diego', 'CA', '92101', 'USA',
 'MEDIUM', 35000000.00, 240,
 'HOT', true, '2019-09-15', '95-8886666'),

-- Professional Services
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-023', 'Accounting & Tax Services', 'CUSTOMER', 'Professional Services',
 '+1-555-0123', 'info@accountingtax.com', 'www.accountingtax.com',
 '1100 CPA Plaza', 'Atlanta', 'GA', '30303', 'USA',
 'SMALL', 3200000.00, 28,
 'WARM', true, '2022-04-08', '58-9997777'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-024', 'Business Consulting Group', 'CUSTOMER', 'Consulting',
 '+1-555-0124', 'inquiries@bizconsulting.com', 'www.businessconsulting.com',
 '2400 Executive Drive', 'Dallas', 'TX', '75201', 'USA',
 'MEDIUM', 16000000.00, 95,
 'WARM', true, '2021-11-22', '74-0001111'),

-- Healthcare
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-025', 'Medical Equipment Suppliers', 'CUSTOMER', 'Healthcare',
 '+1-555-0125', 'orders@medequip.com', 'www.medicalequipment.com',
 '500 Medical Park', 'Philadelphia', 'PA', '19102', 'USA',
 'MEDIUM', 42000000.00, 280,
 'HOT', true, '2020-05-30', '23-1112222'),

-- International Customers
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-026', 'European Tech Holdings GmbH', 'CUSTOMER', 'Technology',
 '+49-30-12345678', 'info@eurotech.de', 'www.europeantech.de',
 'Hauptstrasse 100', 'Berlin', NULL, '10115', 'Germany',
 'LARGE', 62000000.00, 380,
 'WARM', true, '2021-02-28', 'DE123456789'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-027', 'Asia Pacific Solutions Pte Ltd', 'CUSTOMER', 'Technology',
 '+65-6789-0123', 'contact@asiapac.com.sg', 'www.asiapacific.com',
 '10 Marina Boulevard', 'Singapore', NULL, '018983', 'Singapore',
 'LARGE', 78000000.00, 450,
 'HOT', true, '2020-07-12', 'SG201234567A'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'ACC-028', 'UK Distribution Partners Ltd', 'PARTNER', 'Logistics',
 '+44-20-7123-4567', 'partners@ukdistribution.co.uk', 'www.ukdistribution.co.uk',
 '50 Warehouse Lane', 'London', NULL, 'E14 5AB', 'United Kingdom',
 'MEDIUM', 28000000.00, 195,
 'HOT', true, '2021-09-05', 'GB987654321');

--changeset easyops:404-insert-more-contacts context:test-data
--comment: Insert additional contacts for new accounts
INSERT INTO crm.contacts (
    organization_id, account_id, first_name, last_name, job_title, department,
    email, phone, is_primary, is_active
) VALUES

-- Retail Solutions
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-004'),
 'Susan', 'Parker', 'Store Operations Manager', 'Operations',
 'susan.parker@retailsolutions.com', '+1-555-0104 ext.202', true, true),

-- Construction & Engineering
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-006'),
 'Thomas', 'White', 'Senior Engineer', 'Engineering',
 'thomas.white@constructco.com', '+1-555-0106 ext.301', true, true),

-- Legal Advisors
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-008'),
 'Elizabeth', 'Moore', 'Office Manager', 'Administration',
 'elizabeth.moore@legaladvisors.com', '+1-555-0108 ext.100', true, true),

-- Digital Marketing
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-009'),
 'Daniel', 'Harris', 'Account Executive', 'Sales',
 'daniel.harris@digitalmarketing.agency', '+1-555-0109 ext.150', true, true),

-- Premier Financial
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-017'),
 'Margaret', 'Clark', 'VP of Technology', 'IT',
 'margaret.clark@premierfinancial.com', '+1-555-0117 ext.500', true, true),

-- Asia Pacific Solutions
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'),
 (SELECT account_id FROM crm.accounts WHERE account_number = 'ACC-027'),
 'Wei', 'Zhang', 'Managing Director', 'Executive',
 'wei.zhang@asiapac.com.sg', '+65-6789-0123', true, true);

--changeset easyops:405-insert-individual-customers context:test-data
--comment: Insert individual B2C customers
INSERT INTO crm.accounts (
    organization_id, account_number, account_name, account_type, industry,
    phone, email,
    billing_street, billing_city, billing_state, billing_postal_code, billing_country,
    shipping_street, shipping_city, shipping_state, shipping_postal_code, shipping_country,
    company_size, rating, is_active, customer_since
) VALUES

-- Individual Customers (B2C)
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'IND-001', 'Alice Johnson', 'CUSTOMER', 'Individual',
 '+1-555-1001', 'alice.johnson@email.com',
 '123 Maple Street, Apt 4B', 'Boston', 'MA', '02108', 'USA',
 '123 Maple Street, Apt 4B', 'Boston', 'MA', '02108', 'USA',
 NULL, 'WARM', true, '2023-01-15'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'IND-002', 'Robert Chen', 'CUSTOMER', 'Individual',
 '+1-555-1002', 'robert.chen@email.com',
 '456 Oak Avenue', 'San Francisco', 'CA', '94102', 'USA',
 '789 Different St', 'Oakland', 'CA', '94601', 'USA',
 NULL, 'HOT', true, '2022-11-20'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'IND-003', 'Maria Rodriguez', 'CUSTOMER', 'Individual',
 '+1-555-1003', 'maria.rodriguez@email.com',
 '789 Pine Road', 'Miami', 'FL', '33101', 'USA',
 '789 Pine Road', 'Miami', 'FL', '33101', 'USA',
 NULL, 'WARM', true, '2023-03-08'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'IND-004', 'David Kim', 'CUSTOMER', 'Individual',
 '+1-555-1004', 'david.kim@email.com',
 '321 Elm Street', 'Seattle', 'WA', '98104', 'USA',
 '321 Elm Street', 'Seattle', 'WA', '98104', 'USA',
 NULL, 'WARM', true, '2023-05-22'),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 
 'IND-005', 'Jennifer Smith', 'CUSTOMER', 'Individual',
 '+1-555-1005', 'jennifer.smith@email.com',
 '654 Birch Lane', 'Denver', 'CO', '80202', 'USA',
 '654 Birch Lane', 'Denver', 'CO', '80202', 'USA',
 NULL, 'HOT', true, '2022-09-10');

--changeset easyops:406-sync-to-ar-customers context:test-data
--comment: Sync CRM accounts to AR customers for financial tracking
INSERT INTO accounting.customers (
    organization_id, customer_code, customer_name, email, phone,
    credit_limit, payment_terms, is_active
)
SELECT 
    organization_id,
    account_number,
    account_name,
    email,
    phone,
    CASE 
        WHEN company_size = 'LARGE' THEN 500000.00
        WHEN company_size = 'MEDIUM' THEN 100000.00
        WHEN company_size = 'SMALL' THEN 25000.00
        ELSE 5000.00
    END as credit_limit,
    CASE 
        WHEN company_size = 'LARGE' THEN 45
        WHEN company_size = 'MEDIUM' THEN 30
        ELSE 15
    END as payment_terms,
    is_active
FROM crm.accounts
WHERE account_type = 'CUSTOMER'
ON CONFLICT (organization_id, customer_code) DO NOTHING;

