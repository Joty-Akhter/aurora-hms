--liquibase formatted sql

--changeset easyops:057-create-customers-table context:ar
--comment: Create customer master (lightweight - full CRM in Phase 3) table
CREATE TABLE accounting.customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    customer_code VARCHAR(50) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    credit_limit DECIMAL(19, 4) DEFAULT 0,
    current_balance DECIMAL(19, 4) DEFAULT 0,
    payment_terms INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, customer_code)
);

CREATE INDEX idx_customers_org ON accounting.customers(organization_id);
CREATE INDEX idx_customers_code ON accounting.customers(customer_code);
CREATE INDEX idx_customers_active ON accounting.customers(is_active);

--changeset easyops:058-create-ar-invoices-table context:ar
--comment: Create AR Invoices table
CREATE TABLE accounting.ar_invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    customer_id UUID NOT NULL REFERENCES accounting.customers(id),
    period_id UUID REFERENCES accounting.periods(id),
    currency VARCHAR(3) DEFAULT 'USD',
    subtotal DECIMAL(19, 4) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) DEFAULT 0,
    paid_amount DECIMAL(19, 4) DEFAULT 0,
    balance_due DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    gl_journal_id UUID REFERENCES accounting.journal_entries(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ar_invoices_org ON accounting.ar_invoices(organization_id);
CREATE INDEX idx_ar_invoices_customer ON accounting.ar_invoices(customer_id);
CREATE INDEX idx_ar_invoices_date ON accounting.ar_invoices(invoice_date);
CREATE INDEX idx_ar_invoices_due_date ON accounting.ar_invoices(due_date);
CREATE INDEX idx_ar_invoices_status ON accounting.ar_invoices(status);
CREATE INDEX idx_ar_invoices_payment_status ON accounting.ar_invoices(payment_status);

--changeset easyops:059-create-ar-invoice-lines-table context:ar
--comment: Create AR Invoice Lines table
CREATE TABLE accounting.ar_invoice_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES accounting.ar_invoices(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    quantity DECIMAL(19, 4) DEFAULT 1,
    unit_price DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(19, 4) DEFAULT 0,
    account_id UUID REFERENCES accounting.chart_of_accounts(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:060-create-ar-receipts-table context:ar
--comment: Create AR Receipts/Payments table
CREATE TABLE accounting.ar_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    receipt_date DATE NOT NULL,
    customer_id UUID NOT NULL REFERENCES accounting.customers(id),
    payment_method VARCHAR(50),
    reference_number VARCHAR(100),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    bank_account_id UUID,
    gl_journal_id UUID REFERENCES accounting.journal_entries(id),
    notes TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id)
);

CREATE INDEX idx_ar_receipts_customer ON accounting.ar_receipts(customer_id);
CREATE INDEX idx_ar_receipts_date ON accounting.ar_receipts(receipt_date);
CREATE INDEX idx_ar_receipts_status ON accounting.ar_receipts(status);

--changeset easyops:061-create-ar-receipt-allocations-table context:ar
--comment: Create AR Receipt Allocations (which invoices this receipt pays) table
CREATE TABLE accounting.ar_receipt_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    receipt_id UUID NOT NULL REFERENCES accounting.ar_receipts(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES accounting.ar_invoices(id),
    allocated_amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:062-create-ar-credit-notes-table context:ar
--comment: Create AR Credit Notes table
CREATE TABLE accounting.ar_credit_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    credit_note_number VARCHAR(50) UNIQUE NOT NULL,
    credit_note_date DATE NOT NULL,
    customer_id UUID NOT NULL REFERENCES accounting.customers(id),
    invoice_id UUID REFERENCES accounting.ar_invoices(id),
    reason VARCHAR(20),
    notes VARCHAR(1000),
    subtotal DECIMAL(15, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

--changeset easyops:063-create-ar-credit-note-lines-table context:ar
--comment: Create AR Credit Note Lines table
CREATE TABLE accounting.ar_credit_note_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    credit_note_id UUID NOT NULL REFERENCES accounting.ar_credit_notes(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description VARCHAR(500),
    quantity DECIMAL(15, 4) DEFAULT 1,
    unit_price DECIMAL(15, 2) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(15, 2) DEFAULT 0,
    account_id UUID REFERENCES accounting.chart_of_accounts(id)
);

--changeset easyops:064-create-reminder-config-table context:ar
--comment: Create Payment Reminder Configuration table
CREATE TABLE accounting.reminder_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL UNIQUE REFERENCES admin.organizations(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT false,
    days_before_due INTEGER,
    days_after_due_level1 INTEGER DEFAULT 1,
    days_after_due_level2 INTEGER DEFAULT 7,
    days_after_due_level3 INTEGER DEFAULT 14,
    email_template_before_due VARCHAR(2000),
    email_template_level1 VARCHAR(2000),
    email_template_level2 VARCHAR(2000),
    email_template_level3 VARCHAR(2000),
    send_copy_to_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:065-create-reminder-history-table context:ar
--comment: Create Payment Reminder History table
CREATE TABLE accounting.reminder_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES accounting.ar_invoices(id) ON DELETE CASCADE,
    sent_date DATE NOT NULL,
    reminder_level INTEGER NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    email_sent BOOLEAN DEFAULT false,
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:066-create-vendors-table context:ap
--comment: Create Vendor/Supplier master table
CREATE TABLE accounting.vendors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    vendor_code VARCHAR(50) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    payment_terms INTEGER DEFAULT 30,
    tax_id VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, vendor_code)
);

CREATE INDEX idx_vendors_org ON accounting.vendors(organization_id);
CREATE INDEX idx_vendors_code ON accounting.vendors(vendor_code);
CREATE INDEX idx_vendors_active ON accounting.vendors(is_active);

--changeset easyops:067-create-ap-bills-table context:ap
--comment: Create AP Bills table
CREATE TABLE accounting.ap_bills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    bill_number VARCHAR(50) UNIQUE NOT NULL,
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    vendor_id UUID NOT NULL REFERENCES accounting.vendors(id),
    period_id UUID REFERENCES accounting.periods(id),
    currency VARCHAR(3) DEFAULT 'USD',
    subtotal DECIMAL(19, 4) DEFAULT 0,
    tax_amount DECIMAL(19, 4) DEFAULT 0,
    discount_amount DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) DEFAULT 0,
    paid_amount DECIMAL(19, 4) DEFAULT 0,
    balance_due DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    gl_journal_id UUID REFERENCES accounting.journal_entries(id),
    purchase_order_ref VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ap_bills_org ON accounting.ap_bills(organization_id);
CREATE INDEX idx_ap_bills_vendor ON accounting.ap_bills(vendor_id);
CREATE INDEX idx_ap_bills_date ON accounting.ap_bills(bill_date);
CREATE INDEX idx_ap_bills_due_date ON accounting.ap_bills(due_date);
CREATE INDEX idx_ap_bills_status ON accounting.ap_bills(status);
CREATE INDEX idx_ap_bills_payment_status ON accounting.ap_bills(payment_status);

--changeset easyops:068-create-ap-bill-lines-table context:ap
--comment: Create AP Bill Lines table
CREATE TABLE accounting.ap_bill_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_id UUID NOT NULL REFERENCES accounting.ap_bills(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    quantity DECIMAL(19, 4) DEFAULT 1,
    unit_price DECIMAL(19, 4) DEFAULT 0,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    tax_percent DECIMAL(5, 2) DEFAULT 0,
    line_total DECIMAL(19, 4) DEFAULT 0,
    account_id UUID REFERENCES accounting.chart_of_accounts(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:069-create-ap-payments-table context:ap
--comment: Create AP Payments table
CREATE TABLE accounting.ap_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    payment_number VARCHAR(50) UNIQUE NOT NULL,
    payment_date DATE NOT NULL,
    vendor_id UUID NOT NULL REFERENCES accounting.vendors(id),
    payment_method VARCHAR(50),
    reference_number VARCHAR(100),
    check_number VARCHAR(50),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    bank_account_id UUID,
    gl_journal_id UUID REFERENCES accounting.journal_entries(id),
    notes TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id)
);

CREATE INDEX idx_ap_payments_vendor ON accounting.ap_payments(vendor_id);
CREATE INDEX idx_ap_payments_date ON accounting.ap_payments(payment_date);
CREATE INDEX idx_ap_payments_status ON accounting.ap_payments(status);

--changeset easyops:070-create-ap-payment-allocations-table context:ap
--comment: Create AP Payment Allocations table
CREATE TABLE accounting.ap_payment_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES accounting.ap_payments(id) ON DELETE CASCADE,
    bill_id UUID NOT NULL REFERENCES accounting.ap_bills(id),
    allocated_amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:071-create-bank-accounts-table context:bank
--comment: Create Bank Accounts table
CREATE TABLE accounting.bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255),
    branch_name VARCHAR(255),
    account_type VARCHAR(50) DEFAULT 'CHECKING',
    currency VARCHAR(3) DEFAULT 'USD',
    gl_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    opening_balance DECIMAL(19, 4) DEFAULT 0,
    current_balance DECIMAL(19, 4) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, account_number)
);

CREATE INDEX idx_bank_accounts_org ON accounting.bank_accounts(organization_id);
CREATE INDEX idx_bank_accounts_active ON accounting.bank_accounts(is_active);

--changeset easyops:072-create-bank-transactions-table context:bank
--comment: Create Bank Transactions table
CREATE TABLE accounting.bank_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_account_id UUID NOT NULL REFERENCES accounting.bank_accounts(id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(50),
    reference_number VARCHAR(100),
    description TEXT,
    debit_amount DECIMAL(19, 4) DEFAULT 0,
    credit_amount DECIMAL(19, 4) DEFAULT 0,
    balance DECIMAL(19, 4) DEFAULT 0,
    is_reconciled BOOLEAN DEFAULT false,
    reconciled_at TIMESTAMP WITH TIME ZONE,
    gl_journal_id UUID REFERENCES accounting.journal_entries(id),
    source_type VARCHAR(50),
    source_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bank_trans_account ON accounting.bank_transactions(bank_account_id);
CREATE INDEX idx_bank_trans_date ON accounting.bank_transactions(transaction_date);
CREATE INDEX idx_bank_trans_reconciled ON accounting.bank_transactions(is_reconciled);

--changeset easyops:073-create-bank-reconciliations-table context:bank
--comment: Create Bank Reconciliation table
CREATE TABLE accounting.bank_reconciliations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bank_account_id UUID NOT NULL REFERENCES accounting.bank_accounts(id) ON DELETE CASCADE,
    reconciliation_date DATE NOT NULL,
    statement_date DATE NOT NULL,
    opening_balance DECIMAL(19, 4) DEFAULT 0,
    closing_balance DECIMAL(19, 4) DEFAULT 0,
    statement_balance DECIMAL(19, 4) DEFAULT 0,
    difference DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by UUID REFERENCES users.users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id)
);

CREATE INDEX idx_recon_bank_account ON accounting.bank_reconciliations(bank_account_id);
CREATE INDEX idx_recon_date ON accounting.bank_reconciliations(reconciliation_date);
CREATE INDEX idx_recon_status ON accounting.bank_reconciliations(status);

--changeset easyops:074-create-reconciliation-items-table context:bank
--comment: Create Reconciliation Items (matched transactions) table
CREATE TABLE accounting.reconciliation_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reconciliation_id UUID NOT NULL REFERENCES accounting.bank_reconciliations(id) ON DELETE CASCADE,
    bank_transaction_id UUID REFERENCES accounting.bank_transactions(id),
    statement_amount DECIMAL(19, 4),
    is_matched BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:075-create-ar-ap-bank-triggers context:ar,ap,bank splitStatements:false
--comment: Create triggers for AR/AP/Bank tables
CREATE OR REPLACE FUNCTION update_invoice_totals()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE accounting.ar_invoices
    SET 
        subtotal = (SELECT COALESCE(SUM(line_total), 0) FROM accounting.ar_invoice_lines WHERE invoice_id = NEW.invoice_id),
        tax_amount = (SELECT COALESCE(SUM(line_total * tax_percent / 100), 0) FROM accounting.ar_invoice_lines WHERE invoice_id = NEW.invoice_id),
        total_amount = subtotal + tax_amount - discount_amount,
        balance_due = total_amount - paid_amount
    WHERE id = NEW.invoice_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_invoice_totals
AFTER INSERT OR UPDATE OR DELETE ON accounting.ar_invoice_lines
FOR EACH ROW EXECUTE FUNCTION update_invoice_totals();

CREATE OR REPLACE FUNCTION update_bill_totals()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE accounting.ap_bills
    SET 
        subtotal = (SELECT COALESCE(SUM(line_total), 0) FROM accounting.ap_bill_lines WHERE bill_id = NEW.bill_id),
        tax_amount = (SELECT COALESCE(SUM(line_total * tax_percent / 100), 0) FROM accounting.ap_bill_lines WHERE bill_id = NEW.bill_id),
        total_amount = subtotal + tax_amount - discount_amount,
        balance_due = total_amount - paid_amount
    WHERE id = NEW.bill_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_bill_totals
AFTER INSERT OR UPDATE OR DELETE ON accounting.ap_bill_lines
FOR EACH ROW EXECUTE FUNCTION update_bill_totals();

CREATE OR REPLACE FUNCTION update_bank_balance()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE accounting.bank_accounts
    SET current_balance = (
        SELECT opening_balance + COALESCE(SUM(credit_amount - debit_amount), 0)
        FROM accounting.bank_transactions
        WHERE bank_account_id = NEW.bank_account_id
    )
    WHERE id = NEW.bank_account_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_bank_balance
AFTER INSERT OR UPDATE ON accounting.bank_transactions
FOR EACH ROW EXECUTE FUNCTION update_bank_balance();

CREATE TRIGGER update_ar_invoices_updated_at BEFORE UPDATE ON accounting.ar_invoices FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ap_bills_updated_at BEFORE UPDATE ON accounting.ap_bills FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:076-create-ar-ap-bank-views context:ar,ap,bank
--comment: Create views for AR/AP/Bank reporting
CREATE OR REPLACE VIEW accounting.v_ar_aging AS
SELECT 
    i.id,
    i.organization_id,
    i.invoice_number,
    i.invoice_date,
    i.due_date,
    c.customer_code,
    c.customer_name,
    i.total_amount,
    i.paid_amount,
    i.balance_due,
    CURRENT_DATE - i.due_date as days_overdue,
    CASE 
        WHEN CURRENT_DATE <= i.due_date THEN 'CURRENT'
        WHEN CURRENT_DATE - i.due_date BETWEEN 1 AND 30 THEN '1-30 DAYS'
        WHEN CURRENT_DATE - i.due_date BETWEEN 31 AND 60 THEN '31-60 DAYS'
        WHEN CURRENT_DATE - i.due_date BETWEEN 61 AND 90 THEN '61-90 DAYS'
        ELSE 'OVER 90 DAYS'
    END as aging_bucket
FROM accounting.ar_invoices i
JOIN accounting.customers c ON i.customer_id = c.id
WHERE i.balance_due > 0
ORDER BY i.due_date;

CREATE OR REPLACE VIEW accounting.v_ap_aging AS
SELECT 
    b.id,
    b.organization_id,
    b.bill_number,
    b.bill_date,
    b.due_date,
    v.vendor_code,
    v.vendor_name,
    b.total_amount,
    b.paid_amount,
    b.balance_due,
    CURRENT_DATE - b.due_date as days_overdue,
    CASE 
        WHEN CURRENT_DATE <= b.due_date THEN 'CURRENT'
        WHEN CURRENT_DATE - b.due_date BETWEEN 1 AND 30 THEN '1-30 DAYS'
        WHEN CURRENT_DATE - b.due_date BETWEEN 31 AND 60 THEN '31-60 DAYS'
        WHEN CURRENT_DATE - b.due_date BETWEEN 61 AND 90 THEN '61-90 DAYS'
        ELSE 'OVER 90 DAYS'
    END as aging_bucket
FROM accounting.ap_bills b
JOIN accounting.vendors v ON b.vendor_id = v.id
WHERE b.balance_due > 0
ORDER BY b.due_date;

CREATE OR REPLACE VIEW accounting.v_bank_statement AS
SELECT 
    bt.id,
    ba.account_name,
    ba.account_number,
    bt.transaction_date,
    bt.transaction_type,
    bt.reference_number,
    bt.description,
    bt.debit_amount,
    bt.credit_amount,
    bt.balance,
    bt.is_reconciled
FROM accounting.bank_transactions bt
JOIN accounting.bank_accounts ba ON bt.bank_account_id = ba.id
ORDER BY ba.account_number, bt.transaction_date, bt.created_at;

--changeset easyops:077-grant-ar-ap-bank-permissions context:ar,ap,bank
--comment: Grant permissions on AR/AP/Bank tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA accounting TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA accounting TO easyops;
GRANT SELECT ON accounting.v_ar_aging TO easyops;
GRANT SELECT ON accounting.v_ap_aging TO easyops;
GRANT SELECT ON accounting.v_bank_statement TO easyops;
