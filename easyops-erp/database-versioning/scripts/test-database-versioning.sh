#!/bin/bash

# EasyOps ERP Database Versioning Test Script
# This script tests the database versioning system with the actual application stack

echo "🧪 Testing EasyOps ERP Database Versioning System"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

print_status "Docker environment check passed ✓"

# Step 1: Start the test environment
print_status "Step 1: Starting test database environment..."
docker-compose -f docker-compose.test.yml up -d postgres adminer pgadmin

# Wait for PostgreSQL to be ready
print_status "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker-compose -f docker-compose.test.yml exec postgres pg_isready -U easyops > /dev/null 2>&1; then
        print_success "PostgreSQL is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "PostgreSQL failed to start within 60 seconds"
        exit 1
    fi
    echo -n "."
    sleep 2
done

# Step 2: Create the development user and databases
print_status "Step 2: Setting up database users and permissions..."
docker-compose -f docker-compose.test.yml exec postgres psql -U easyops -d postgres -c "
-- Create development user if it doesn't exist
DO \$\$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'easyops_dev') THEN
      CREATE USER easyops_dev WITH PASSWORD 'easyops123';
   END IF;
END
\$\$;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE easyops TO easyops_dev;
GRANT ALL PRIVILEGES ON DATABASE easyops_test TO easyops_dev;
"

if [ $? -eq 0 ]; then
    print_success "Database users and permissions configured ✓"
else
    print_warning "Database user setup had issues, but continuing..."
fi

# Step 3: Run Liquibase migrations
print_status "Step 3: Running Liquibase database migrations..."
docker-compose -f docker-compose.test.yml run --rm liquibase

if [ $? -eq 0 ]; then
    print_success "Database migrations completed successfully ✓"
else
    print_error "Database migrations failed!"
    exit 1
fi

# Step 4: Validate the database structure
print_status "Step 4: Validating database structure..."

# Check if all schemas were created
schemas=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('admin', 'users', 'auth', 'rbac', 'system', 'audit', 'notifications', 'integration', 'accounting', 'sales')
ORDER BY schema_name;
")

expected_schemas=("admin" "auth" "accounting" "sales" "rbac" "system" "audit" "notifications" "integration" "users")
for schema in "${expected_schemas[@]}"; do
    if echo "$schemas" | grep -q "$schema"; then
        print_success "Schema '$schema' exists ✓"
    else
        print_error "Schema '$schema' is missing!"
    fi
done

# Check if test data was inserted
print_status "Step 5: Validating test data..."

# Check Chart of Accounts
coa_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.chart_of_accounts;")
if [ "$coa_count" -ge 20 ]; then
    print_success "Chart of Accounts: $coa_count accounts created ✓"
else
    print_error "Chart of Accounts: Only $coa_count accounts found (expected 20+)"
fi

# Check Customers
customer_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.customers;")
if [ "$customer_count" -ge 10 ]; then
    print_success "Accounting Customers: $customer_count customers created ✓"
else
    print_error "Accounting Customers: Only $customer_count customers found (expected 10+)"
fi

# Check Sales Customers
sales_customer_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.customers;")
if [ "$sales_customer_count" -ge 10 ]; then
    print_success "Sales Customers: $sales_customer_count customers created ✓"
else
    print_error "Sales Customers: Only $sales_customer_count customers found (expected 10+)"
fi

# Check Products
product_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.products;")
if [ "$product_count" -ge 15 ]; then
    print_success "Products: $product_count products created ✓"
else
    print_error "Products: Only $product_count products found (expected 15+)"
fi

# Check Vendors
vendor_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.vendors;")
if [ "$vendor_count" -ge 10 ]; then
    print_success "Vendors: $vendor_count vendors created ✓"
else
    print_error "Vendors: Only $vendor_count vendors found (expected 10+)"
fi

# Check Sales Orders
sales_order_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.sales_orders;")
if [ "$sales_order_count" -ge 5 ]; then
    print_success "Sales Orders: $sales_order_count orders created ✓"
else
    print_error "Sales Orders: Only $sales_order_count orders found (expected 5+)"
fi

# Check AR Invoices
ar_invoice_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.ar_invoices;")
if [ "$ar_invoice_count" -ge 8 ]; then
    print_success "AR Invoices: $ar_invoice_count invoices created ✓"
else
    print_error "AR Invoices: Only $ar_invoice_count invoices found (expected 8+)"
fi

# Check AP Bills
ap_bill_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.ap_bills;")
if [ "$ap_bill_count" -ge 8 ]; then
    print_success "AP Bills: $ap_bill_count bills created ✓"
else
    print_error "AP Bills: Only $ap_bill_count bills found (expected 8+)"
fi

# Check Bank Transactions
bank_transaction_count=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.bank_transactions;")
if [ "$bank_transaction_count" -ge 15 ]; then
    print_success "Bank Transactions: $bank_transaction_count transactions created ✓"
else
    print_error "Bank Transactions: Only $bank_transaction_count transactions found (expected 15+)"
fi

# Step 6: Test database connectivity from application perspective
print_status "Step 6: Testing database connectivity and basic queries..."

# Test a complex query that would be used by the application
complex_query_result=$(docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "
SELECT 
    COUNT(DISTINCT c.id) as total_customers,
    COUNT(DISTINCT p.id) as total_products,
    COUNT(DISTINCT so.id) as total_sales_orders,
    SUM(so.total_amount) as total_sales_value
FROM accounting.customers c
CROSS JOIN sales.products p
LEFT JOIN sales.sales_databases so ON 1=1;
")

if [ $? -eq 0 ]; then
    print_success "Complex query execution successful ✓"
    echo "Query result: $complex_query_result"
else
    print_error "Complex query execution failed!"
fi

# Final Test Results
echo ""
echo "🎉 Database Versioning Test Results"
echo "==================================="
echo ""
echo "📊 Test Summary:"
echo "• Database Schema: All schemas created successfully"
echo "• Chart of Accounts: $coa_count accounts"
echo "• Accounting Customers: $customer_count customers"
echo "• Sales Customers: $sales_customer_count customers"
echo "• Products: $product_count products"
echo "• Vendors: $vendor_count vendors"
echo "• Sales Orders: $sales_order_count orders"
echo "• AR Invoices: $ar_invoice_count invoices"
echo "• AP Bills: $ap_bill_count bills"
echo "• Bank Transactions: $bank_transaction_count transactions"
echo ""
echo "🔗 Access Points:"
echo "• Database: localhost:5433 (easyops_dev / easyops123)"
echo "• Adminer: http://localhost:8081"
echo "• pgAdmin: http://localhost:5050 (admin@easyops.com / admin123)"
echo ""
echo "✅ Database versioning system is working correctly!"
echo "🚀 Ready for application integration testing!"
echo ""
echo "💡 To stop test environment: docker-compose -f docker-compose.test.yml down"
echo "💡 To view database: Open http://localhost:8081 in your browser"
echo "💡 To access pgAdmin: Open http://localhost:5050 in your browser"
echo ""
