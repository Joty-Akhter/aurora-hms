# Database Migration Guide

This guide explains how to use Liquibase for database migrations in the EasyOps ERP system.

## Overview

The EasyOps ERP system uses Liquibase for database versioning and migrations. All database schema changes are managed through SQL changesets organized by development phases.

## Quick Start

### Prerequisites

1. **Install Liquibase**: Download and install Liquibase from [https://www.liquibase.org/download](https://www.liquibase.org/download)
2. **Install PostgreSQL Client**: Ensure `psql` is available for database connection testing
3. **Start Database**: Use Docker Compose to start the PostgreSQL database

### Running Your First Migration

1. **Start the database**:
   ```bash
   cd easyops-erp
   docker-compose up -d postgres
   ```

2. **Run migrations**:
   ```bash
   cd database-versioning
   ./scripts/migrate.sh
   ```

3. **Validate the schema**:
   ```bash
   ./scripts/validate.sh
   ```

## RBAC and module-specific changelogs

**Hospital** and **pharma** permission rows (`rbac.permissions` / `rbac.role_permissions`) are **not** maintained in this `database-versioning` project. They are applied when you run each service’s own Liquibase changelog against the shared database:

- **Hospital / EHR** — `services/hospital-service/src/main/resources/db/changelog/` (see `changesets/002-hospital-permissions.sql`).
- **Pharma** — `services/pharma-service/src/main/resources/db/changelog/data/015-pharma-permissions.sql`.

The **`database-versioning`** changelog keeps **platform-wide** schema and shared seed data. See `easyops-erp/rbac/README.md` for the split.

## Migration Workflow

### Development Workflow

1. **Create a new changeset**:
   - Add your SQL changes to a new `.sql` file in the appropriate phase directory
   - Use the format: `--liquibase formatted sql` at the top
   - Each changeset should have a unique ID and author

2. **Test the migration**:
   ```bash
   # Run migrations
   ./scripts/migrate.sh
   
   # Validate the result
   ./scripts/validate.sh
   ```

3. **Rollback if needed**:
   ```bash
   ./scripts/rollback.sh --count 1
   ```

### Production Deployment

1. **Backup the database**:
   ```bash
   pg_dump -h localhost -U easyops -d easyops > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Run migrations**:
   ```bash
   ./scripts/migrate.sh --host production-host --database production-db
   ```

3. **Validate**:
   ```bash
   ./scripts/validate.sh --host production-host --database production-db
   ```

## Changeset Structure

### SQL Changeset Format

```sql
--liquibase formatted sql

--changeset author:id context:context-name
--comment: Description of the changeset
CREATE TABLE example_table (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset author:id2 context:context-name
--comment: Another changeset
CREATE INDEX idx_example_name ON example_table(name);
```

### Changeset Attributes

- **author**: Your name or identifier
- **id**: Unique identifier for the changeset
- **context**: Optional context for environment-specific changes
- **comment**: Description of what the changeset does

## Organization by Phases

### Phase 0: Core System Foundation
- `001-initial-schema.sql`: Core tables (users, organizations, etc.)
- `002-auth-schema.sql`: Authentication tables
- `003-rbac-schema.sql`: Role-based access control
- `004-notifications.sql`: Notification system
- `005-integration.sql`: Integration and monitoring

### Phase 1: Accounting Module
- `101-accounting-schema.sql`: Chart of accounts, journals, periods
- `102-ar-ap-bank.sql`: Accounts receivable, payable, bank management
- `103-fixes-and-updates.sql`: Bug fixes and schema updates

### Phase 2: Sales Module
- `201-sales-schema.sql`: Sales tables (customers, products, orders)
- `202-sales-updates.sql`: Sales module updates

### Data Files
- `001-default-roles.sql`: Default system roles
- `002-default-permissions.sql`: Default permissions
- `003-system-config.sql`: System configuration data

## Best Practices

### 1. Naming Convention
- Use descriptive names: `001-create-users-table.sql`
- Include phase and sequence: `101-accounting-schema.sql`
- Use kebab-case for file names

### 2. Atomic Changes
- Each changeset should be atomic and reversible
- Group related changes together
- Avoid breaking changes in a single changeset

### 3. Testing
- Always test migrations on a copy of production data
- Use the validation script to verify schema integrity
- Test rollback procedures

### 4. Documentation
- Include clear comments in changesets
- Document complex business logic
- Update this guide for new patterns

## Common Commands

### Migration Commands
```bash
# Run all pending migrations
./scripts/migrate.sh

# Run migrations for specific context
./scripts/migrate.sh --context production

# Validate current schema
./scripts/validate.sh

# Rollback last changeset
./scripts/rollback.sh --count 1

# Rollback to specific changeset
./scripts/rollback.sh --changeset 001-initial-schema
```

### Database Connection Options
```bash
# Custom database connection
./scripts/migrate.sh --host localhost --port 5433 --database easyops_test

# Production deployment
./scripts/migrate.sh --host prod-db.example.com --database easyops_prod
```

## Troubleshooting

### Common Issues

1. **Connection Failed**
   - Verify database is running: `docker-compose ps postgres`
   - Check connection parameters in scripts
   - Ensure PostgreSQL client tools are installed

2. **Migration Failed**
   - Check Liquibase logs for specific error
   - Verify SQL syntax in changeset
   - Ensure database user has required permissions

3. **Validation Failed**
   - Compare current schema with expected schema
   - Check for manual database changes
   - Regenerate changelog if needed: `./scripts/generate-changelog.sh`

4. **Rollback Failed**
   - Check if rollback SQL is defined for changeset
   - Verify database state before rollback
   - Consider manual rollback for complex changes

### Getting Help

1. **Check Liquibase Documentation**: [https://docs.liquibase.com/](https://docs.liquibase.com/)
2. **Review Changeset Logs**: Check `databasechangelog` table for executed changesets
3. **Validate Database State**: Use validation script to identify issues
4. **Generate New Changelog**: Use generate script to compare with current state

## Environment-Specific Configuration

### Development
- Uses local PostgreSQL instance (port 5432)
- Default credentials: easyops/easyops123
- Context: `default`

### Testing
- Uses test database (port 5433)
- Context: `test`
- Separate test data initialization

### Production
- Custom database connection parameters
- Context: `production`
- Requires backup before migration

## Security Considerations

1. **Database Credentials**: Use environment variables for production
2. **Backup Strategy**: Always backup before production migrations
3. **Access Control**: Limit database user permissions
4. **Audit Trail**: Liquibase maintains change history in `databasechangelog`

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Run Database Migrations
  run: |
    cd database-versioning
    ./scripts/migrate.sh --host ${{ secrets.DB_HOST }} --database ${{ secrets.DB_NAME }}
    ./scripts/validate.sh --host ${{ secrets.DB_HOST }} --database ${{ secrets.DB_NAME }}
```

### Docker Integration
```bash
# Run migrations in Docker container
docker run --rm -v $(pwd):/workspace liquibase/liquibase \
  --url=jdbc:postgresql://localhost:5432/easyops \
  --username=easyops --password=easyops123 \
  --changeLogFile=changelog/master-changelog.xml update
```
