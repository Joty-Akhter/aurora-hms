--liquibase formatted sql

--changeset easyops:101-insert-test-customers context:test-data
--comment: Insert test customers for development and testing
INSERT INTO accounting.customers (organization_id, customer_code, customer_name, email, phone, contact_person, credit_limit, payment_terms, is_active) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST001', 'Acme Corporation', 'contact@acme.com', '+1-555-0101', 'John Smith', 50000.00, 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST002', 'TechStart Inc', 'info@techstart.com', '+1-555-0102', 'Sarah Johnson', 25000.00, 15, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST003', 'Global Solutions Ltd', 'sales@globalsolutions.com', '+1-555-0103', 'Mike Chen', 100000.00, 45, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST004', 'Local Business Co', 'hello@localbiz.com', '+1-555-0104', 'Emily Davis', 15000.00, 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST005', 'Enterprise Systems', 'contact@enterprisesys.com', '+1-555-0105', 'Robert Wilson', 75000.00, 60, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST006', 'Small Shop LLC', 'orders@smallshop.com', '+1-555-0106', 'Lisa Brown', 5000.00, 15, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST007', 'Manufacturing Corp', 'sales@manufacturing.com', '+1-555-0107', 'David Miller', 200000.00, 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST008', 'Service Provider Inc', 'info@serviceprovider.com', '+1-555-0108', 'Jennifer Taylor', 30000.00, 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST009', 'Retail Chain Store', 'corporate@retailchain.com', '+1-555-0109', 'Michael Anderson', 125000.00, 45, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CUST010', 'Online Marketplace', 'billing@onlinemarket.com', '+1-555-0110', 'Amanda White', 50000.00, 30, true);

--changeset easyops:102-insert-test-vendors context:test-data
--comment: Insert test vendors for development and testing
INSERT INTO accounting.vendors (organization_id, vendor_code, vendor_name, email, phone, payment_terms, is_active) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND001', 'Office Supplies Co', 'orders@officesupplies.com', '+1-555-0201', 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND002', 'Tech Equipment Ltd', 'sales@techequipment.com', '+1-555-0202', 45, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND003', 'Marketing Services Inc', 'contact@marketingservices.com', '+1-555-0203', 15, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND004', 'Cleaning Supplies Corp', 'orders@cleaningsupplies.com', '+1-555-0204', 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND005', 'Software Licenses LLC', 'licensing@softwarelicenses.com', '+1-555-0205', 60, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND006', 'Utilities Provider', 'billing@utilitiesprovider.com', '+1-555-0206', 15, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND007', 'Consulting Firm', 'info@consultingfirm.com', '+1-555-0207', 30, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND008', 'Transportation Co', 'dispatch@transportation.com', '+1-555-0208', 45, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND009', 'Legal Services', 'legal@legalservices.com', '+1-555-0209', 60, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'VEND010', 'Insurance Broker', 'claims@insurancebroker.com', '+1-555-0210', 30, true);

--changeset easyops:103-insert-test-products context:test-data
--comment: Insert test products for development and testing
INSERT INTO sales.products (organization_id, product_code, product_name, description, product_type, category, unit_of_measure, unit_price, cost_price, tax_rate, is_active, track_inventory) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD001', 'Laptop Computer', 'High-performance business laptop with 16GB RAM and 512GB SSD', 'GOODS', 'Electronics', 'Unit', 1299.99, 899.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 'GOODS', 'Furniture', 'Unit', 299.99, 199.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD003', 'Software License', 'Annual software license for productivity suite', 'SERVICE', 'Software', 'License', 99.99, 50.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD004', 'Consulting Services', 'Business consulting services per hour', 'SERVICE', 'Services', 'Hour', 150.00, 0.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD005', 'Printer Paper', 'A4 white printer paper, 500 sheets per ream', 'GOODS', 'Office Supplies', 'Ream', 8.99, 4.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD006', 'Marketing Campaign', 'Complete digital marketing campaign setup', 'SERVICE', 'Marketing', 'Campaign', 2500.00, 0.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD007', 'Desk Lamp', 'LED desk lamp with adjustable brightness', 'GOODS', 'Furniture', 'Unit', 89.99, 59.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD008', 'Training Session', 'Professional training session for team', 'SERVICE', 'Training', 'Session', 500.00, 0.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD009', 'Coffee Maker', 'Commercial-grade coffee maker for office', 'GOODS', 'Appliances', 'Unit', 249.99, 179.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD010', 'Website Development', 'Custom website development project', 'SERVICE', 'Web Development', 'Project', 5000.00, 0.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD011', 'Office Desk', 'Executive office desk with drawers', 'GOODS', 'Furniture', 'Unit', 599.99, 399.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD012', 'Cloud Storage', 'Monthly cloud storage subscription', 'SERVICE', 'Cloud Services', 'Month', 29.99, 15.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD013', 'Smartphone', 'Business smartphone with security features', 'GOODS', 'Electronics', 'Unit', 899.99, 649.99, 8.5, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD014', 'Legal Consultation', 'Hourly legal consultation services', 'SERVICE', 'Legal', 'Hour', 200.00, 0.00, 8.5, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'PROD015', 'Cleaning Services', 'Monthly office cleaning service', 'SERVICE', 'Maintenance', 'Month', 800.00, 0.00, 8.5, true, false);

--changeset easyops:104-insert-test-bank-accounts context:test-data
--comment: Insert test bank accounts for development and testing
INSERT INTO accounting.bank_accounts (organization_id, account_number, account_name, bank_name, branch_name, account_type, currency, opening_balance, current_balance, is_active) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHK001', 'Main Business Checking', 'First National Bank', 'Downtown Branch', 'CHECKING', 'USD', 25000.00, 25000.00, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SAV001', 'Business Savings', 'First National Bank', 'Downtown Branch', 'SAVINGS', 'USD', 100000.00, 100000.00, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHK002', 'Payroll Checking', 'Community Credit Union', 'Main Office', 'CHECKING', 'USD', 5000.00, 5000.00, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CHK003', 'Petty Cash', 'Local Bank', 'Corporate Branch', 'CASH', 'USD', 1000.00, 1000.00, true);

--changeset easyops:105-insert-test-fiscal-years context:test-data
--comment: Insert test fiscal years for development and testing
INSERT INTO accounting.fiscal_years (organization_id, year_code, start_date, end_date, is_closed) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FY2023', '2023-01-01', '2023-12-31', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FY2024', '2024-01-01', '2024-12-31', false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'FY2025', '2025-01-01', '2025-12-31', false);

--changeset easyops:106-insert-test-periods context:test-data
--comment: Insert test accounting periods for current fiscal year
INSERT INTO accounting.periods (organization_id, fiscal_year_id, period_number, period_name, period_type, start_date, end_date, status) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 1, 'January 2024', 'MONTHLY', '2024-01-01', '2024-01-31', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 2, 'February 2024', 'MONTHLY', '2024-02-01', '2024-02-29', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 3, 'March 2024', 'MONTHLY', '2024-03-01', '2024-03-31', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 4, 'April 2024', 'MONTHLY', '2024-04-01', '2024-04-30', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 5, 'May 2024', 'MONTHLY', '2024-05-01', '2024-05-31', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 6, 'June 2024', 'MONTHLY', '2024-06-01', '2024-06-30', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 7, 'July 2024', 'MONTHLY', '2024-07-01', '2024-07-31', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 8, 'August 2024', 'MONTHLY', '2024-08-01', '2024-08-31', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 9, 'September 2024', 'MONTHLY', '2024-09-01', '2024-09-30', 'CLOSED'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 10, 'October 2024', 'MONTHLY', '2024-10-01', '2024-10-31', 'OPEN'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 11, 'November 2024', 'MONTHLY', '2024-11-01', '2024-11-30', 'OPEN'),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), (SELECT id FROM accounting.fiscal_years WHERE year_code = 'FY2024'), 12, 'December 2024', 'MONTHLY', '2024-12-01', '2024-12-31', 'OPEN');

--changeset easyops:107-insert-test-chart-of-accounts context:test-data
--comment: Insert test chart of accounts for DEMO_ORG — aligned with standard business template (cash/bank, AR, inventory, FA, liabilities, equity, revenue, COGS, operating, selling, admin, other) + payroll integration codes (1030, 2020, 6110, EPF_PAYABLE, CASH)
INSERT INTO accounting.chart_of_accounts (organization_id, account_code, account_name, account_type, account_category, level, is_group, is_active, allow_manual_entry) VALUES
-- Assets
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1000', 'ASSETS', 'ASSET', 'Assets', 1, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1030', 'Bank - Operating Account', 'ASSET', 'Current Assets', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'CASH', 'Cash', 'ASSET', 'Current Assets', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1100', 'Current Assets', 'ASSET', 'Current Assets', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1110', 'Cash and Cash Equivalents', 'ASSET', 'Current Assets', 3, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1111', 'Petty Cash', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1112', 'Cash on Hand', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1120', 'Bank Accounts', 'ASSET', 'Current Assets', 3, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1121', 'Main Business Checking', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1122', 'Business Savings', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1123', 'Payroll Checking', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1130', 'Accounts Receivable', 'ASSET', 'Current Assets', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1150', 'Allowance for Doubtful Accounts', 'ASSET', 'Current Assets', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1140', 'Inventory', 'ASSET', 'Current Assets', 3, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1141', 'Raw Materials', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1142', 'Work in Progress', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1143', 'Finished Goods', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1240', 'Goods in Transit', 'ASSET', 'Current Assets', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1300', 'Prepaid Expenses', 'ASSET', 'Current Assets', 3, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1310', 'Prepaid Insurance', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1320', 'Prepaid Rent', 'ASSET', 'Current Assets', 4, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1200', 'Fixed Assets', 'ASSET', 'Fixed Assets', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1210', 'Office Equipment', 'ASSET', 'Fixed Assets', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1215', 'Vehicle Fleet', 'ASSET', 'Fixed Assets', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '1220', 'Accumulated Depreciation', 'ASSET', 'Fixed Assets', 3, false, true, true),

-- Liabilities
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2000', 'LIABILITIES', 'LIABILITY', 'Liabilities', 1, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2100', 'Current Liabilities', 'LIABILITY', 'Current Liabilities', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2110', 'Accounts Payable', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2020', 'Accrued Expenses', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2130', 'Payroll Liabilities', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'EPF_PAYABLE', 'EPF Payable', 'LIABILITY', 'Current Liabilities', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2030', 'Sales Tax Payable', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2040', 'Income Tax Payable', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2050', 'Short-term Loans', 'LIABILITY', 'Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2500', 'Long-term Liabilities', 'LIABILITY', 'Non-Current Liabilities', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2510', 'Long-term Loans', 'LIABILITY', 'Non-Current Liabilities', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '2520', 'Bonds Payable', 'LIABILITY', 'Non-Current Liabilities', 3, false, true, true),

-- Equity
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '3000', 'EQUITY', 'EQUITY', 'Equity', 1, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '3100', 'Owner Equity', 'EQUITY', 'Owner Equity', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '3200', 'Retained Earnings', 'EQUITY', 'Retained Earnings', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '3030', 'Current Year Earnings', 'EQUITY', 'Equity', 2, false, true, true),

-- Revenue
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4000', 'REVENUE', 'REVENUE', 'Revenue', 1, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4100', 'Sales Revenue', 'REVENUE', 'Sales Revenue', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4200', 'Service Revenue', 'REVENUE', 'Service Revenue', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4030', 'Interest Income', 'REVENUE', 'Non-Operating Revenue', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4050', 'Sales Returns and Allowances', 'REVENUE', 'Operating Revenue', 2, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '4040', 'Other Income', 'REVENUE', 'Non-Operating Revenue', 2, false, true, true),

-- Expenses
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5000', 'EXPENSES', 'EXPENSE', 'Expenses', 1, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5001', 'Cost of Goods Sold', 'EXPENSE', 'Cost of Goods Sold', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5010', 'Materials Cost', 'EXPENSE', 'Cost of Goods Sold', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5020', 'Direct Labor', 'EXPENSE', 'Cost of Goods Sold', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5030', 'Freight and Delivery', 'EXPENSE', 'Cost of Goods Sold', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6000', 'Selling Expenses', 'EXPENSE', 'Operating Expenses', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6010', 'Advertising and Marketing', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6020', 'Sales Commissions', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6100', 'Administrative Expenses', 'EXPENSE', 'Operating Expenses', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6110', 'Salaries', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6120', 'Rent Expense', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '6160', 'Depreciation Expense', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5200', 'Operating Expenses', 'EXPENSE', 'Operating Expenses', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5210', 'Office Expenses', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5220', 'Marketing Expenses', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5230', 'Professional Services', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5240', 'Utilities', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '5250', 'Insurance', 'EXPENSE', 'Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '7000', 'Other Expenses', 'EXPENSE', 'Non-Operating Expenses', 2, true, true, false),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '7010', 'Interest Expense', 'EXPENSE', 'Non-Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '7020', 'Bank Charges', 'EXPENSE', 'Non-Operating Expenses', 3, false, true, true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), '7030', 'Bad Debt Expense', 'EXPENSE', 'Non-Operating Expenses', 3, false, true, true);

--changeset easyops:108-insert-test-sales-customers context:test-data
--comment: Insert test sales customers (different from accounting customers)
INSERT INTO sales.customers (organization_id, customer_code, customer_name, email, phone, contact_person, billing_address, shipping_address, credit_limit, payment_terms, is_active) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST001', 'Premium Client Corp', 'sales@premiumclient.com', '+1-555-1001', 'Alice Johnson', '123 Business Ave, Suite 100, New York, NY 10001', '123 Business Ave, Suite 100, New York, NY 10001', 75000.00, 'Net 30', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST002', 'Tech Innovators LLC', 'orders@techinnovators.com', '+1-555-1002', 'Bob Smith', '456 Innovation St, San Francisco, CA 94105', '456 Innovation St, San Francisco, CA 94105', 50000.00, 'Net 15', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST003', 'Global Enterprises', 'procurement@globalenterprises.com', '+1-555-1003', 'Carol Davis', '789 Corporate Blvd, Chicago, IL 60601', '789 Corporate Blvd, Chicago, IL 60601', 150000.00, 'Net 45', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST004', 'Startup Solutions Inc', 'info@startupsolutions.com', '+1-555-1004', 'David Wilson', '321 Startup Lane, Austin, TX 78701', '321 Startup Lane, Austin, TX 78701', 25000.00, 'Net 30', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST005', 'Manufacturing Co', 'orders@manufacturingco.com', '+1-555-1005', 'Eva Brown', '654 Industrial Park, Detroit, MI 48201', '654 Industrial Park, Detroit, MI 48201', 200000.00, 'Net 60', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST006', 'Retail Chain Store', 'corporate@retailchain.com', '+1-555-1006', 'Frank Miller', '987 Commerce St, Los Angeles, CA 90015', '987 Commerce St, Los Angeles, CA 90015', 100000.00, 'Net 30', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST007', 'Service Provider Ltd', 'accounts@serviceprovider.com', '+1-555-1007', 'Grace Taylor', '147 Service Ave, Seattle, WA 98101', '147 Service Ave, Seattle, WA 98101', 40000.00, 'Net 15', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST008', 'Consulting Firm', 'billing@consultingfirm.com', '+1-555-1008', 'Henry Anderson', '258 Consultant Blvd, Boston, MA 02101', '258 Consultant Blvd, Boston, MA 02101', 60000.00, 'Net 30', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST009', 'Non-Profit Organization', 'finance@nonprofit.org', '+1-555-1009', 'Ivy White', '369 Mission St, Washington, DC 20001', '369 Mission St, Washington, DC 20001', 30000.00, 'Net 15', true),
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'SCUST010', 'Educational Institution', 'purchasing@university.edu', '+1-555-1010', 'Jack Thompson', '741 Education Dr, Cambridge, MA 02138', '741 Education Dr, Cambridge, MA 02138', 80000.00, 'Net 45', true);
