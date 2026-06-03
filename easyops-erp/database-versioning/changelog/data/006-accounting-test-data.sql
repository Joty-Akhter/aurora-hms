--liquibase formatted sql

--changeset easyops:113-insert-test-ar-invoices context:test-data
--comment: Insert test AR invoices for development and testing
INSERT INTO accounting.ar_invoices (organization_id, invoice_number, invoice_date, due_date, customer_id, period_id, currency, subtotal, tax_amount, total_amount, balance_due, status, payment_status, notes, created_by) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-001', '2024-10-01', '2024-10-31', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST001'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 15000.00, 1275.00, 16275.00, 16275.00, 'POSTED', 'UNPAID', 'Monthly service invoice', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-002', '2024-10-05', '2024-11-04', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST002'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 8500.00, 722.50, 9222.50, 9222.50, 'POSTED', 'UNPAID', 'Software license renewal', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-003', '2024-10-10', '2024-11-09', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST003'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 45000.00, 3825.00, 48825.00, 48825.00, 'POSTED', 'UNPAID', 'Large enterprise order', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-004', '2024-10-15', '2024-11-14', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST004'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 3200.00, 272.00, 3472.00, 3472.00, 'POSTED', 'UNPAID', 'Consulting services', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-005', '2024-09-01', '2024-09-30', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST001'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'September 2024'),
 'USD', 12000.00, 1020.00, 13020.00, 0.00, 'POSTED', 'PAID', 'Previous month invoice - paid', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-006', '2024-09-15', '2024-10-15', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST005'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'September 2024'),
 'USD', 28000.00, 2380.00, 30380.00, 0.00, 'POSTED', 'PAID', 'Equipment sale - paid', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-007', '2024-08-01', '2024-08-31', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST006'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'August 2024'),
 'USD', 5000.00, 425.00, 5425.00, 5425.00, 'POSTED', 'OVERDUE', 'Overdue invoice', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'INV-2024-008', '2024-08-15', '2024-09-14', 
 (SELECT id FROM accounting.customers WHERE customer_code = 'CUST007'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'August 2024'),
 'USD', 15000.00, 1275.00, 16275.00, 16275.00, 'POSTED', 'OVERDUE', 'Manufacturing services - overdue', 
 (SELECT id FROM users.users WHERE username = 'admin'));

--changeset easyops:114-insert-test-ar-invoice-lines context:test-data
--comment: Insert test AR invoice lines for development and testing
INSERT INTO accounting.ar_invoice_lines (invoice_id, line_number, description, quantity, unit_price, line_total, tax_percent) VALUES
-- Invoice INV-2024-001 lines
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-001'), 1, 'Software Development Services', 100, 120.00, 12000.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-001'), 2, 'Consulting Services', 20, 150.00, 3000.00, 8.5),

-- Invoice INV-2024-002 lines
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-002'), 1, 'Software License Renewal', 50, 99.99, 4999.50, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-002'), 2, 'Support Services', 12, 29.99, 359.88, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-002'), 3, 'Training Services', 10, 150.00, 1500.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-002'), 4, 'Implementation Services', 8, 200.00, 1600.00, 8.5),

-- Invoice INV-2024-003 lines
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-003'), 1, 'Enterprise Software License', 1, 25000.00, 25000.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-003'), 2, 'Custom Development', 100, 150.00, 15000.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-003'), 3, 'Integration Services', 50, 100.00, 5000.00, 8.5),

-- Invoice INV-2024-004 lines
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-004'), 1, 'Business Consulting', 16, 150.00, 2400.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-004'), 2, 'Strategic Planning', 8, 100.00, 800.00, 8.5),

-- Invoice INV-2024-005 lines (paid)
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-005'), 1, 'Monthly Subscription', 1, 10000.00, 10000.00, 8.5),
((SELECT id FROM accounting.ar_invoices WHERE invoice_number = 'INV-2024-005'), 2, 'Additional Services', 10, 200.00, 2000.00, 8.5);

--changeset easyops:115-insert-test-ap-bills context:test-data
--comment: Insert test AP bills for development and testing
INSERT INTO accounting.ap_bills (organization_id, bill_number, bill_date, due_date, vendor_id, period_id, currency, subtotal, tax_amount, total_amount, balance_due, status, payment_status, notes, created_by) VALUES
((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-001', '2024-10-01', '2024-10-31', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND001'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 2500.00, 212.50, 2712.50, 2712.50, 'POSTED', 'UNPAID', 'Office supplies monthly order', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-002', '2024-10-05', '2024-11-04', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND002'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 15000.00, 1275.00, 16275.00, 16275.00, 'POSTED', 'UNPAID', 'Computer equipment purchase', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-003', '2024-10-10', '2024-11-09', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND003'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 5000.00, 425.00, 5425.00, 5425.00, 'POSTED', 'UNPAID', 'Marketing campaign services', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-004', '2024-10-15', '2024-11-14', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND004'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'October 2024'),
 'USD', 800.00, 68.00, 868.00, 868.00, 'POSTED', 'UNPAID', 'Cleaning supplies', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-005', '2024-09-01', '2024-09-30', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND005'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'September 2024'),
 'USD', 3000.00, 255.00, 3255.00, 0.00, 'POSTED', 'PAID', 'Software licenses - paid', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-006', '2024-09-15', '2024-10-15', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND006'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'September 2024'),
 'USD', 1200.00, 102.00, 1302.00, 0.00, 'POSTED', 'PAID', 'Utilities bill - paid', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-007', '2024-08-01', '2024-08-31', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND007'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'August 2024'),
 'USD', 8000.00, 680.00, 8680.00, 8680.00, 'POSTED', 'OVERDUE', 'Consulting services - overdue', 
 (SELECT id FROM users.users WHERE username = 'admin')),

((SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG'), 'BILL-2024-008', '2024-08-15', '2024-09-14', 
 (SELECT id FROM accounting.vendors WHERE vendor_code = 'VEND008'), 
 (SELECT id FROM accounting.periods WHERE period_name = 'August 2024'),
 'USD', 2500.00, 212.50, 2712.50, 2712.50, 'POSTED', 'OVERDUE', 'Transportation services - overdue', 
 (SELECT id FROM users.users WHERE username = 'admin'));

--changeset easyops:116-insert-test-ap-bill-lines context:test-data
--comment: Insert test AP bill lines for development and testing
INSERT INTO accounting.ap_bill_lines (bill_id, line_number, description, quantity, unit_price, line_total, tax_percent) VALUES
-- Bill BILL-2024-001 lines
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 1, 'Printer Paper', 50, 8.99, 449.50, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 2, 'Pens and Pencils', 100, 1.50, 150.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 3, 'File Folders', 200, 2.00, 400.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 4, 'Notebooks', 50, 5.00, 250.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 5, 'Desk Organizers', 25, 12.00, 300.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 6, 'Staplers', 10, 25.00, 250.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 7, 'Toner Cartridges', 5, 80.00, 400.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 8, 'Markers and Highlighters', 30, 3.00, 90.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 9, 'Binder Clips', 20, 8.00, 160.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-001'), 10, 'Post-it Notes', 25, 4.00, 100.00, 8.5),

-- Bill BILL-2024-002 lines
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-002'), 1, 'Desktop Computers', 10, 1200.00, 12000.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-002'), 2, 'Monitors', 15, 300.00, 4500.00, 8.5),

-- Bill BILL-2024-003 lines
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-003'), 1, 'Digital Marketing Campaign', 1, 3000.00, 3000.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-003'), 2, 'Social Media Management', 2, 500.00, 1000.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-003'), 3, 'Content Creation', 10, 100.00, 1000.00, 8.5),

-- Bill BILL-2024-004 lines
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-004'), 1, 'Cleaning Supplies', 1, 500.00, 500.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-004'), 2, 'Paper Towels', 20, 8.00, 160.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-004'), 3, 'Disinfectant', 10, 12.00, 120.00, 8.5),
((SELECT id FROM accounting.ap_bills WHERE bill_number = 'BILL-2024-004'), 4, 'Trash Bags', 5, 15.00, 75.00, 8.5);

--changeset easyops:117-insert-test-bank-transactions context:test-data
--comment: Insert test bank transactions for development and testing
INSERT INTO accounting.bank_transactions (bank_account_id, transaction_date, transaction_type, reference_number, description, debit_amount, credit_amount, balance, is_reconciled, source_type, source_id) VALUES
-- Main Business Checking transactions
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-01', 'DEPOSIT', 'DEP001', 'Customer payment - Acme Corp', 0.00, 13020.00, 38020.00, false, 'AR_RECEIPT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-05', 'WITHDRAWAL', 'CHK001', 'Office supplies payment', 2712.50, 0.00, 35307.50, false, 'AP_PAYMENT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-10', 'DEPOSIT', 'DEP002', 'Customer payment - TechStart', 0.00, 9222.50, 44530.00, false, 'AR_RECEIPT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-15', 'WITHDRAWAL', 'CHK002', 'Computer equipment payment', 16275.00, 0.00, 28255.00, false, 'AP_PAYMENT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-20', 'DEPOSIT', 'DEP003', 'Customer payment - Global Solutions', 0.00, 48825.00, 77080.00, false, 'AR_RECEIPT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-25', 'WITHDRAWAL', 'CHK003', 'Marketing services payment', 5425.00, 0.00, 71655.00, false, 'AP_PAYMENT', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK001'), '2024-10-30', 'FEE', 'FEE001', 'Monthly maintenance fee', 15.00, 0.00, 71640.00, false, 'MANUAL', null),

-- Business Savings transactions
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'SAV001'), '2024-10-01', 'INTEREST', 'INT001', 'Monthly interest payment', 0.00, 150.00, 100150.00, false, 'MANUAL', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'SAV001'), '2024-10-15', 'TRANSFER', 'TRF001', 'Transfer to checking account', 10000.00, 0.00, 90150.00, false, 'MANUAL', null),

-- Payroll Checking transactions
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK002'), '2024-10-01', 'TRANSFER', 'TRF002', 'Payroll funding from main account', 0.00, 15000.00, 20000.00, false, 'MANUAL', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK002'), '2024-10-05', 'WITHDRAWAL', 'PAY001', 'Employee salary payments', 12500.00, 0.00, 7500.00, false, 'MANUAL', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK002'), '2024-10-05', 'WITHDRAWAL', 'PAY002', 'Payroll taxes', 2500.00, 0.00, 5000.00, false, 'MANUAL', null),

-- Petty Cash transactions
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK003'), '2024-10-10', 'WITHDRAWAL', 'CASH001', 'Office snacks and supplies', 50.00, 0.00, 950.00, false, 'MANUAL', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK003'), '2024-10-20', 'WITHDRAWAL', 'CASH002', 'Emergency office supplies', 75.00, 0.00, 875.00, false, 'MANUAL', null),
((SELECT id FROM accounting.bank_accounts WHERE account_number = 'CHK003'), '2024-10-30', 'DEPOSIT', 'CASH003', 'Reimbursement for business lunch', 0.00, 25.00, 900.00, false, 'MANUAL', null);
