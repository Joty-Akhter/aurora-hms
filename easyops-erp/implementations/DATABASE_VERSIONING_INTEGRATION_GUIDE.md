# Database Versioning Integration Guide

This guide explains how to test the database versioning system with the complete EasyOps ERP application stack.

## 🎯 Overview

The database versioning system has been integrated with the full application stack to provide a complete testing environment. This allows you to:

- Test database migrations with real application services
- Validate that all services can connect to the migrated database
- Verify that test data works correctly with the application
- Test the complete development workflow

## 🚀 Quick Start Options

### Option 1: Test Database Versioning Standalone (Recommended for DB Testing)

**Purpose**: Test only the database versioning system without the full application stack.

```bash
# Navigate to database versioning directory
cd easyops-erp/database-versioning

# Run the test script
# Linux/Mac
./scripts/test-database-versioning.sh

# Windows
scripts\test-database-versioning.bat
```

**What this does**:
- Starts PostgreSQL, Adminer, and pgAdmin
- Runs Liquibase migrations
- Validates database structure and test data
- Provides access to database management tools

**Access Points**:
- Database: `localhost:5433` (easyops_dev / easyops123)
- Adminer: http://localhost:8081
- pgAdmin: http://localhost:5050 (admin@easyops.com / admin123)

### Option 2: Full Application Stack with Migrations (Recommended for Integration Testing)

**Purpose**: Start the complete application stack with database migrations.

```bash
# Navigate to project root
cd easyops-erp

# Run the full startup script
# Linux/Mac
./scripts/start-dev-with-migrations.sh

# Windows
scripts\start-dev-with-migrations.bat
```

**What this does**:
- Starts all infrastructure services (PostgreSQL, Redis, etc.)
- Runs Liquibase migrations
- Starts all microservices (Eureka, API Gateway, Business Services)
- Starts the frontend application
- Starts monitoring stack (Prometheus, Grafana)

**Access Points**:
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8081
- Eureka Registry: http://localhost:8761
- Database Admin: http://localhost:8080
- Grafana: http://localhost:3001 (admin/admin)

## 📊 Test Data Overview

The system includes comprehensive test data:

### Core Business Data
- **Chart of Accounts**: 25 accounts with complete structure
- **Customers**: 20 test customers (10 accounting + 10 sales)
- **Products**: 15 test products (goods and services)
- **Vendors**: 10 test vendors for accounts payable

### Transaction Data
- **Sales Orders**: 5 test orders with line items
- **Quotations**: 5 test quotations with line items
- **AR Invoices**: 8 test invoices with payment statuses
- **AP Bills**: 8 test bills with payment statuses
- **Bank Transactions**: 15 test bank transactions

### System Data
- **Fiscal Years**: 2023, 2024, 2025
- **Accounting Periods**: 12 monthly periods for 2024
- **Bank Accounts**: 4 test bank accounts
- **Users**: Admin user with system roles

## 🔧 Service Integration

### Database Connection
All services are configured to connect to the same PostgreSQL database:
- **Host**: postgres (Docker network) / localhost (external)
- **Database**: easyops
- **Username**: easyops_dev
- **Password**: easyops123

### Service Dependencies
Services start in the correct order with health checks:
1. **Infrastructure**: PostgreSQL, Redis
2. **Service Discovery**: Eureka
3. **Core Services**: API Gateway, User Management, Auth, RBAC
4. **Business Services**: Accounting, AR, AP, Bank, Sales
5. **Frontend**: React application
6. **Monitoring**: Prometheus, Grafana

## 🧪 Testing Scenarios

### 1. Database Migration Testing
```bash
# Test database versioning standalone
cd easyops-erp/database-versioning
./scripts/test-database-versioning.sh
```

**Validates**:
- Schema creation
- Test data insertion
- Referential integrity
- Database connectivity

### 2. Application Integration Testing
```bash
# Test full application stack
cd easyops-erp
./scripts/start-dev-with-migrations.sh
```

**Validates**:
- Service startup and health checks
- Database connectivity from services
- API endpoints functionality
- Frontend application loading

### 3. Business Process Testing
Once the application is running, you can test:

**Sales Process**:
1. View customers at http://localhost:3000/customers
2. Create quotations
3. Convert to sales orders
4. Generate invoices

**Accounting Process**:
1. View chart of accounts
2. Process AR invoices
3. Manage AP bills
4. Bank reconciliation

## 📈 Monitoring and Observability

### Service Health
- **Eureka Dashboard**: http://localhost:8761
- **Service Health Checks**: All services have health endpoints

### Metrics and Monitoring
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Service Metrics**: Available at `/actuator/metrics` for each service

### Database Management
- **Adminer**: http://localhost:8080 (full stack) or http://localhost:8081 (standalone)
- **pgAdmin**: http://localhost:5050 (standalone only)

## 🛠️ Development Workflow

### 1. Make Database Changes
```bash
# Create new changeset
cd easyops-erp/database-versioning
# Edit changelog files
# Update master-changelog.xml
```

### 2. Test Changes
```bash
# Test database changes
./scripts/test-database-versioning.sh

# Or test with full application
cd ..
./scripts/start-dev-with-migrations.sh
```

### 3. Validate Integration
- Check service health endpoints
- Test API functionality
- Verify frontend integration
- Review database changes

## 🔍 Troubleshooting

### Common Issues

**Database Connection Issues**:
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check database connectivity
docker-compose exec postgres psql -U easyops_dev -d easyops -c "SELECT 1;"
```

**Service Startup Issues**:
```bash
# Check service logs
docker-compose logs [service-name]

# Check service health
curl http://localhost:[port]/actuator/health
```

**Migration Issues**:
```bash
# Check Liquibase status
cd easyops-erp/database-versioning
./scripts/validate.sh

# Check database schema
docker-compose exec postgres psql -U easyops_dev -d easyops -c "\dt"
```

### Port Conflicts
If you encounter port conflicts:
- **Standalone DB**: Uses ports 5433, 8081, 5050
- **Full Stack**: Uses ports 5432, 8080, 3000, etc.

## 📚 Additional Resources

- **[Database Versioning README](./database-versioning/README.md)** - Detailed database versioning documentation
- **[Test Data Guide](./database-versioning/docs/test-data-guide.md)** - Comprehensive test data documentation
- **[Migration Guide](./database-versioning/docs/migration-guide.md)** - Database migration procedures
- **[Rollback Procedures](./database-versioning/docs/rollback-procedures.md)** - Rollback strategies

## 🎉 Success Criteria

Your database versioning system is working correctly when:

✅ **Database migrations run successfully**  
✅ **All schemas and tables are created**  
✅ **Test data is inserted correctly**  
✅ **Services can connect to the database**  
✅ **API endpoints return expected data**  
✅ **Frontend can display data from the database**  
✅ **Health checks pass for all services**  

## 🚀 Next Steps

After successful testing:

1. **Develop new features** using the established database structure
2. **Add new test data** as needed for development
3. **Create new migrations** for schema changes
4. **Test rollback procedures** to ensure data safety
5. **Integrate with CI/CD** for automated testing

The database versioning system is now fully integrated and ready for development!
