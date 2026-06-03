# Test Data Summary

This document provides a quick overview of all test data added to the EasyOps ERP database versioning system.

## 📊 Data Overview

| Category | Count | Description |
|----------|-------|-------------|
| **Customers (Accounting)** | 10 | Various business customers with credit limits |
| **Customers (Sales)** | 10 | Sales module customers with addresses |
| **Vendors** | 10 | Suppliers for accounts payable |
| **Products** | 15 | Mix of goods and services with pricing |
| **Sales Orders** | 5 | Orders in various statuses |
| **Sales Order Lines** | 15 | Detailed line items for orders |
| **Quotations** | 5 | Quotes in different statuses |
| **Quotation Lines** | 10 | Detailed quote line items |
| **AR Invoices** | 8 | Customer invoices with payment status |
| **AR Invoice Lines** | 12 | Invoice line items |
| **AP Bills** | 8 | Vendor bills with payment status |
| **AP Bill Lines** | 25 | Bill line items |
| **Bank Transactions** | 15 | Various bank account transactions |
| **Bank Accounts** | 4 | Different account types |
| **Chart of Accounts** | 25 | Complete account structure |
| **Fiscal Years** | 3 | 2023, 2024, 2025 |
| **Accounting Periods** | 12 | Monthly periods for 2024 |

## 🏢 Business Entities

### Customers
- **Acme Corporation** - Large enterprise client
- **TechStart Inc** - Startup company
- **Global Solutions Ltd** - International business
- **Local Business Co** - Small local company
- **Enterprise Systems** - Large system integrator
- **Small Shop LLC** - Small retail business
- **Manufacturing Corp** - Industrial manufacturer
- **Service Provider Inc** - Professional services
- **Retail Chain Store** - Multi-location retailer
- **Online Marketplace** - E-commerce platform

### Products
- **Electronics**: Laptops, smartphones, monitors
- **Furniture**: Office chairs, desks, lamps
- **Software**: Licenses, cloud storage
- **Services**: Consulting, training, development
- **Office Supplies**: Paper, pens, organizers

### Vendors
- **Office Supplies Co** - Stationery and supplies
- **Tech Equipment Ltd** - Computer hardware
- **Marketing Services Inc** - Digital marketing
- **Cleaning Supplies Corp** - Maintenance supplies
- **Software Licenses LLC** - Software licensing
- **Utilities Provider** - Utility services
- **Consulting Firm** - Professional consulting
- **Transportation Co** - Logistics services
- **Legal Services** - Legal consultation
- **Insurance Broker** - Insurance services

## 💰 Financial Data

### Revenue Streams
- **Service Revenue**: $150,000+ in consulting and development
- **Product Sales**: $200,000+ in equipment and software
- **Subscription Revenue**: $50,000+ in recurring services

### Payment Terms
- **Net 15**: 2 customers/vendors
- **Net 30**: 8 customers/vendors
- **Net 45**: 3 customers/vendors
- **Net 60**: 2 customers/vendors

### Credit Limits
- **$5,000 - $25,000**: Small businesses
- **$25,000 - $75,000**: Mid-size companies
- **$75,000 - $200,000**: Large enterprises

## 📈 Business Scenarios

### Sales Process Flow
1. **Lead Generation** → Customer inquiries
2. **Quotation** → Formal quotes with pricing
3. **Sales Order** → Confirmed orders
4. **Delivery** → Product/service delivery
5. **Invoicing** → AR invoice generation
6. **Payment** → Customer payment collection

### Purchase Process Flow
1. **Vendor Bills** → Incoming invoices
2. **Bill Approval** → Review and approval
3. **Payment Processing** → Vendor payments
4. **Bank Reconciliation** → Transaction matching

### Financial Reporting
- **Chart of Accounts**: Complete account structure
- **Fiscal Periods**: Monthly accounting periods
- **Bank Reconciliation**: Sample transactions
- **AR Aging**: Outstanding receivables
- **AP Aging**: Outstanding payables

## 🎯 Testing Scenarios

### Data Validation
- **Referential Integrity**: All foreign keys properly linked
- **Calculations**: Tax, discounts, totals verified
- **Status Workflows**: Order and payment status changes
- **Date Logic**: Due dates, aging calculations

### Business Logic
- **Credit Limits**: Customer credit management
- **Payment Terms**: Vendor payment scheduling
- **Inventory Tracking**: Product availability
- **Multi-Currency**: USD-based transactions

### Reporting
- **Financial Statements**: P&L, Balance Sheet data
- **Sales Reports**: Order and revenue analysis
- **Purchase Reports**: Vendor and expense analysis
- **Bank Reports**: Cash flow and reconciliation

## 🔧 Technical Implementation

### Database Relationships
- **One-to-Many**: Customers → Orders → Lines
- **Many-to-Many**: Products ↔ Orders (via lines)
- **Hierarchical**: Chart of Accounts structure
- **Temporal**: Fiscal years and periods

### Data Consistency
- **Organization Scoping**: All data belongs to DEMO_ORG
- **User Attribution**: Admin user as creator
- **Date Ranges**: 2024 fiscal year focus
- **Currency**: USD for all transactions

### Context Management
- **Test Data Tag**: `context:test-data`
- **Production Exclusion**: Can be excluded from prod
- **Development Inclusion**: Included by default in dev
- **Selective Deployment**: Context-based filtering

## 📋 Usage Instructions

### Development
```bash
# Include test data
liquibase --contexts=test-data update

# Exclude test data
liquibase --exclude-contexts=test-data update
```

### Testing
- Use test data for automated testing
- Verify business logic with realistic scenarios
- Test edge cases with boundary data
- Validate calculations and workflows

### Training
- Use test data for user training
- Demonstrate business processes
- Show reporting capabilities
- Practice data entry and management

## 📚 Documentation

- **[Test Data Guide](./docs/test-data-guide.md)** - Detailed documentation
- **[Migration Guide](./docs/migration-guide.md)** - Database migration procedures
- **[Rollback Procedures](./docs/rollback-procedures.md)** - Rollback strategies

## 🔄 Maintenance

### Adding New Test Data
1. Create new changeset in appropriate data file
2. Maintain referential integrity
3. Use realistic business scenarios
4. Include proper context tags

### Updating Existing Data
1. Use new changeset with appropriate ID
2. Update related records if necessary
3. Maintain data consistency
4. Document changes

This comprehensive test data set provides a solid foundation for development, testing, and training activities in the EasyOps ERP system.
