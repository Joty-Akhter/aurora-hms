# Test Data Guide

This document describes the comprehensive test data included in the EasyOps ERP system for development and testing purposes.

## Overview

The test data is organized into several categories and includes realistic business scenarios to help developers and testers work with the system effectively.

## Test Data Files

### 1. Core Test Data (`004-test-data.sql`)

**Customers (Accounting Module)**
- 10 test customers with various credit limits and payment terms
- Mix of enterprise, mid-size, and small business customers
- Examples: Acme Corporation, TechStart Inc, Global Solutions Ltd

**Vendors**
- 10 test vendors for Accounts Payable testing
- Various payment terms and business types
- Examples: Office Supplies Co, Tech Equipment Ltd, Marketing Services Inc

**Products (Sales Module)**
- 15 test products including both goods and services
- Mix of electronics, furniture, software, and consulting services
- Realistic pricing with tax rates
- Examples: Laptop Computer, Office Chair, Software License, Consulting Services

**Bank Accounts**
- 4 test bank accounts representing different account types
- Main Business Checking, Business Savings, Payroll Checking, Petty Cash
- Opening balances and current balances set

**Fiscal Years and Periods**
- Fiscal Year 2024 (current year) with 12 monthly periods
- Some periods closed, current period open
- Proper date ranges for testing

**Chart of Accounts**
- Complete chart of accounts structure
- Assets, Liabilities, Equity, Revenue, and Expense accounts
- Hierarchical structure with proper account codes

### 2. Sales Test Data (`005-sales-test-data.sql`)

**Sales Customers**
- 10 additional customers specifically for sales module
- Complete address information (billing and shipping)
- Different credit limits and payment terms
- Examples: Premium Client Corp, Tech Innovators LLC, Global Enterprises

**Quotations**
- 5 test quotations in various statuses (DRAFT, SENT, ACCEPTED, EXPIRED)
- Realistic pricing with discounts and taxes
- Linked to sales customers and products
- Examples: QUO-2024-001 to QUO-2024-005

**Quotation Lines**
- Detailed line items for each quotation
- Product details, quantities, pricing, discounts
- Tax calculations included

**Sales Orders**
- 5 test sales orders in different statuses
- Mix of CONFIRMED, IN_PROGRESS, COMPLETED, DRAFT orders
- Linked to quotations where applicable
- Examples: SO-2024-001 to SO-2024-005

**Sales Order Lines**
- Detailed line items for each sales order
- Delivery and invoicing status tracking
- Quantities, pricing, and tax information

### 3. Accounting Test Data (`006-accounting-test-data.sql`)

**AR Invoices**
- 8 test invoices in various payment statuses
- Mix of PAID, UNPAID, and OVERDUE invoices
- Realistic business scenarios
- Examples: INV-2024-001 to INV-2024-008

**AR Invoice Lines**
- Detailed line items for each invoice
- Service descriptions, quantities, pricing
- Tax calculations included

**AP Bills**
- 8 test bills from various vendors
- Different payment statuses and due dates
- Realistic vendor scenarios
- Examples: BILL-2024-001 to BILL-2024-008

**AP Bill Lines**
- Detailed line items for each bill
- Product descriptions, quantities, pricing
- Tax calculations included

**Bank Transactions**
- 15 test bank transactions across all accounts
- Mix of deposits, withdrawals, transfers, fees
- Realistic transaction patterns
- Balance tracking included

## Data Relationships

The test data is designed to maintain referential integrity and realistic business relationships:

- **Customers**: Linked between accounting and sales modules
- **Products**: Referenced in quotations, sales orders, and invoice lines
- **Organizations**: All data belongs to the DEMO_ORG organization
- **Users**: Created by the admin user
- **Periods**: Invoices and bills linked to appropriate accounting periods
- **Bank Accounts**: Transactions linked to specific accounts

## Business Scenarios

### Sales Process Flow
1. **Quotation Creation**: Customer requests quote for products/services
2. **Quotation Approval**: Customer accepts quotation
3. **Sales Order**: Quotation converts to sales order
4. **Delivery**: Products/services delivered
5. **Invoicing**: AR invoice created from sales order
6. **Payment**: Customer payment received and recorded

### Purchase Process Flow
1. **Vendor Bills**: Various vendor invoices received
2. **Bill Approval**: Bills reviewed and approved
3. **Payment**: Vendor payments processed
4. **Bank Transactions**: Payments recorded in bank accounts

### Financial Reporting
- **Chart of Accounts**: Complete account structure for financial reporting
- **Fiscal Periods**: Proper period setup for monthly/yearly reporting
- **Bank Reconciliation**: Sample transactions for reconciliation testing

## Usage Guidelines

### Development
- Use test data for development and testing
- All data uses the DEMO_ORG organization
- Admin user is the creator for all records
- Test data includes realistic business scenarios

### Testing
- Test various business processes with the provided data
- Verify calculations (taxes, discounts, totals)
- Test status changes and workflows
- Validate reporting and reconciliation features

### Context Usage
- Test data is tagged with `context:test-data`
- Can be excluded from production deployments
- Use `--contexts=test-data` to include only test data
- Use `--exclude-contexts=test-data` to exclude test data

## Data Maintenance

### Adding New Test Data
1. Create new changeset in appropriate data file
2. Maintain referential integrity
3. Use realistic business scenarios
4. Include proper context tags

### Updating Existing Data
1. Use new changeset with appropriate ID
2. Update related records if necessary
3. Maintain data consistency
4. Document changes in this guide

## Sample Queries

### Find All Unpaid Invoices
```sql
SELECT invoice_number, customer_name, total_amount, due_date
FROM accounting.ar_invoices 
WHERE payment_status = 'UNPAID'
ORDER BY due_date;
```

### Sales Orders by Status
```sql
SELECT order_number, customer_name, status, total_amount
FROM sales.sales_orders
ORDER BY status, order_date DESC;
```

### Bank Account Balances
```sql
SELECT account_name, account_number, current_balance
FROM accounting.bank_accounts
WHERE is_active = true;
```

### Products by Category
```sql
SELECT category, COUNT(*) as product_count, AVG(unit_price) as avg_price
FROM sales.products
WHERE is_active = true
GROUP BY category
ORDER BY category;
```

## Notes

- All monetary amounts are in USD
- Tax rates are set to 8.5% (example rate)
- Dates are set to 2024 for current testing
- All test data uses the DEMO_ORG organization
- Admin user is the creator for all test records
- Test data is designed to be realistic and comprehensive
- Context tags allow selective deployment of test data
