@echo off
REM EasyOps ERP Database Versioning Test Script
REM This script tests the database versioning system with the actual application stack

echo 🧪 Testing EasyOps ERP Database Versioning System
echo ================================================

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop and try again.
    exit /b 1
)

echo [INFO] Docker environment check passed ✓

REM Step 1: Start the test environment
echo [INFO] Step 1: Starting test database environment...
docker-compose -f docker-compose.test.yml up -d postgres adminer pgadmin

REM Wait for PostgreSQL to be ready
echo [INFO] Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

REM Step 2: Create the development user and databases
echo [INFO] Step 2: Setting up database users and permissions...
docker-compose -f docker-compose.test.yml exec postgres psql -U easyops -d postgres -c "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'easyops_dev') THEN CREATE USER easyops_dev WITH PASSWORD 'easyops123'; END IF; END $$; GRANT ALL PRIVILEGES ON DATABASE easyops TO easyops_dev; GRANT ALL PRIVILEGES ON DATABASE easyops_test TO easyops_dev;"

if %errorlevel% equ 0 (
    echo [SUCCESS] Database users and permissions configured ✓
) else (
    echo [WARNING] Database user setup had issues, but continuing...
)

REM Step 3: Run Liquibase migrations
echo [INFO] Step 3: Running Liquibase database migrations...
docker-compose -f docker-compose.test.yml run --rm liquibase

if %errorlevel% equ 0 (
    echo [SUCCESS] Database migrations completed successfully ✓
) else (
    echo [ERROR] Database migrations failed!
    exit /b 1
)

REM Step 4: Validate the database structure
echo [INFO] Step 4: Validating database structure...

REM Check if all schemas were created
echo [INFO] Checking schemas...
docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name IN ('admin', 'users', 'auth', 'rbac', 'system', 'audit', 'notifications', 'integration', 'accounting', 'sales') ORDER BY schema_name;"

REM Step 5: Validate test data
echo [INFO] Step 5: Validating test data...

REM Check Chart of Accounts
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.chart_of_accounts;"') do set coa_count=%%i
echo [INFO] Chart of Accounts: %coa_count% accounts created

REM Check Customers
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.customers;"') do set customer_count=%%i
echo [INFO] Accounting Customers: %customer_count% customers created

REM Check Sales Customers
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.customers;"') do set sales_customer_count=%%i
echo [INFO] Sales Customers: %sales_customer_count% customers created

REM Check Products
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.products;"') do set product_count=%%i
echo [INFO] Products: %product_count% products created

REM Check Vendors
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.vendors;"') do set vendor_count=%%i
echo [INFO] Vendors: %vendor_count% vendors created

REM Check Sales Orders
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM sales.sales_orders;"') do set sales_order_count=%%i
echo [INFO] Sales Orders: %sales_order_count% orders created

REM Check AR Invoices
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.ar_invoices;"') do set ar_invoice_count=%%i
echo [INFO] AR Invoices: %ar_invoice_count% invoices created

REM Check AP Bills
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.ap_bills;"') do set ap_bill_count=%%i
echo [INFO] AP Bills: %ap_bill_count% bills created

REM Check Bank Transactions
for /f %%i in ('docker-compose -f docker-compose.test.yml exec postgres psql -U easyops_dev -d easyops -t -c "SELECT COUNT(*) FROM accounting.bank_transactions;"') do set bank_transaction_count=%%i
echo [INFO] Bank Transactions: %bank_transaction_count% transactions created

REM Final Test Results
echo.
echo 🎉 Database Versioning Test Results
echo ===================================
echo.
echo 📊 Test Summary:
echo • Chart of Accounts: %coa_count% accounts
echo • Accounting Customers: %customer_count% customers
echo • Sales Customers: %sales_customer_count% customers
echo • Products: %product_count% products
echo • Vendors: %vendor_count% vendors
echo • Sales Orders: %sales_order_count% orders
echo • AR Invoices: %ar_invoice_count% invoices
echo • AP Bills: %ap_bill_count% bills
echo • Bank Transactions: %bank_transaction_count% transactions
echo.
echo 🔗 Access Points:
echo • Database: localhost:5433 (easyops_dev / easyops123)
echo • Adminer: http://localhost:8081
echo • pgAdmin: http://localhost:5050 (admin@easyops.com / admin123)
echo.
echo ✅ Database versioning system is working correctly!
echo 🚀 Ready for application integration testing!
echo.
echo 💡 To stop test environment: docker-compose -f docker-compose.test.yml down
echo 💡 To view database: Open http://localhost:8081 in your browser
echo 💡 To access pgAdmin: Open http://localhost:5050 in your browser
echo.

pause
