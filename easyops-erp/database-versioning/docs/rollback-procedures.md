# Database Rollback Procedures

This document outlines the procedures for rolling back database changes in the EasyOps ERP system using Liquibase.

## Overview

Rollbacks in Liquibase allow you to undo database changes that have been applied. However, rollbacks require careful planning and execution, especially in production environments.

## Types of Rollbacks

### 1. Count-Based Rollback
Roll back a specific number of changesets from the most recent.

```bash
# Rollback the last 3 changesets
./scripts/rollback.sh --count 3
```

### 2. Changeset-Based Rollback
Roll back to a specific changeset ID.

```bash
# Rollback to changeset 001-initial-schema
./scripts/rollback.sh --changeset 001-initial-schema
```

### 3. Tag-Based Rollback
Roll back to a specific tag (if tags are used).

```bash
# Rollback to tag "v1.0"
liquibase rollback v1.0
```

## Rollback Requirements

### Prerequisites for Rollbacks

1. **Rollback SQL Defined**: Each changeset should have rollback SQL defined
2. **Database Backup**: Always backup before rollback operations
3. **Application Compatibility**: Ensure application code is compatible with rolled-back schema

### Rollback SQL Best Practices

When creating changesets, include rollback SQL:

```sql
--changeset easyops:001-create-table context:initial
--comment: Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

--rollback DROP TABLE users;
```

## Rollback Procedures by Environment

### Development Environment

1. **Identify the changeset to rollback**:
   ```bash
   liquibase status --contexts=dev
   ```

2. **Create backup** (optional but recommended):
   ```bash
   pg_dump -h localhost -p 5433 -U easyops -d easyops > backup_before_rollback.sql
   ```

3. **Perform rollback**:
   ```bash
   ./scripts/rollback.sh --count 1 --context dev
   ```

4. **Validate rollback**:
   ```bash
   ./scripts/validate.sh --context dev
   ```

5. **Test application compatibility**:
   - Restart application services
   - Run integration tests
   - Verify functionality

### Testing Environment

1. **Stop test services**:
   ```bash
   docker-compose stop sales-service accounting-service
   ```

2. **Backup test database**:
   ```bash
   pg_dump -h localhost -p 5434 -U easyops -d easyops_test > test_backup.sql
   ```

3. **Perform rollback**:
   ```bash
   ./scripts/rollback.sh --host localhost --port 5434 --database easyops_test --count 2
   ```

4. **Validate and restart services**:
   ```bash
   ./scripts/validate.sh --host localhost --port 5434 --database easyops_test
   docker-compose start sales-service accounting-service
   ```

### Production Environment

⚠️ **CRITICAL**: Production rollbacks require careful planning and coordination.

#### Pre-Rollback Checklist

- [ ] Business impact assessment completed
- [ ] Application team notified
- [ ] Database backup created and verified
- [ ] Rollback plan documented and approved
- [ ] Monitoring team alerted
- [ ] Maintenance window scheduled (if required)

#### Production Rollback Steps

1. **Create comprehensive backup**:
   ```bash
   pg_dump -h prod-db.example.com -U easyops -d easyops_prod > \
     prod_backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Verify backup integrity**:
   ```bash
   pg_restore --list prod_backup_*.sql | head -10
   ```

3. **Stop application services**:
   ```bash
   # Stop all services that depend on the database
   kubectl scale deployment easyops-api-gateway --replicas=0
   kubectl scale deployment easyops-accounting-service --replicas=0
   # ... other services
   ```

4. **Perform rollback**:
   ```bash
   ./scripts/rollback.sh \
     --host prod-db.example.com \
     --database easyops_prod \
     --changeset 001-initial-schema
   ```

5. **Validate rollback**:
   ```bash
   ./scripts/validate.sh \
     --host prod-db.example.com \
     --database easyops_prod
   ```

6. **Restart services gradually**:
   ```bash
   # Start core services first
   kubectl scale deployment easyops-api-gateway --replicas=2
   
   # Wait for health checks
   sleep 30
   
   # Start other services
   kubectl scale deployment easyops-accounting-service --replicas=2
   ```

7. **Monitor system health**:
   - Check application logs
   - Verify database connections
   - Monitor error rates
   - Test critical functionality

## Complex Rollback Scenarios

### Scenario 1: Data Migration Rollback

If a changeset involved data migration:

1. **Check for data dependencies**:
   ```sql
   SELECT COUNT(*) FROM table_with_migrated_data;
   ```

2. **Plan data restoration**:
   - Identify data that needs to be restored
   - Prepare restoration scripts
   - Test restoration on copy of production data

3. **Execute rollback with data restoration**:
   ```bash
   # Rollback schema changes
   ./scripts/rollback.sh --changeset data-migration-changeset
   
   # Restore data if needed
   psql -h prod-db.example.com -U easyops -d easyops_prod -f restore_data.sql
   ```

### Scenario 2: Multi-Service Rollback

When multiple services depend on schema changes:

1. **Identify service dependencies**:
   ```bash
   # Check which services use the changed tables
   grep -r "changed_table_name" services/*/src/
   ```

2. **Coordinate service rollbacks**:
   - Stop dependent services in reverse dependency order
   - Perform database rollback
   - Restart services in dependency order

3. **Version compatibility**:
   - Ensure application versions are compatible with rolled-back schema
   - May require rolling back application deployments

### Scenario 3: Partial Rollback

When only some changes need to be rolled back:

1. **Create custom rollback changeset**:
   ```sql
   --changeset easyops:rollback-specific-change context:rollback
   --comment: Rollback specific problematic change
   
   -- Remove only the problematic column
   ALTER TABLE problematic_table DROP COLUMN problematic_column;
   
   --rollback ALTER TABLE problematic_table ADD COLUMN problematic_column VARCHAR(255);
   ```

2. **Apply custom rollback**:
   ```bash
   ./scripts/migrate.sh --context rollback
   ```

## Rollback Validation

### Schema Validation

```bash
# Validate schema integrity
./scripts/validate.sh

# Check for orphaned data
psql -d easyops -c "SELECT * FROM information_schema.tables WHERE table_schema = 'public';"
```

### Application Validation

1. **Health checks**:
   ```bash
   curl -f http://localhost:8081/actuator/health
   curl -f http://localhost:8088/actuator/health
   ```

2. **Integration tests**:
   ```bash
   # Run critical path tests
   mvn test -Dtest=CriticalPathTests
   ```

3. **User acceptance testing**:
   - Test login functionality
   - Verify data access
   - Check report generation

## Emergency Rollback Procedures

### When Standard Rollback Fails

1. **Manual rollback**:
   ```sql
   -- Connect to database and manually reverse changes
   DROP TABLE IF EXISTS problematic_table;
   ALTER TABLE users DROP COLUMN problematic_column;
   ```

2. **Restore from backup**:
   ```bash
   # Stop all services
   docker-compose down
   
   # Drop and recreate database
   dropdb -h localhost -U easyops easyops
   createdb -h localhost -U easyops easyops
   
   # Restore from backup
   psql -h localhost -U easyops -d easyops < backup_before_changes.sql
   
   # Restart services
   docker-compose up -d
   ```

3. **Point-in-time recovery**:
   ```bash
   # If using WAL archiving
   pg_basebackup -h prod-db.example.com -D /tmp/recovery -Ft -z -P
   # Configure recovery.conf and start PostgreSQL
   ```

## Rollback Monitoring

### Key Metrics to Monitor

1. **Database metrics**:
   - Connection counts
   - Query performance
   - Error rates

2. **Application metrics**:
   - Response times
   - Error rates
   - Throughput

3. **Business metrics**:
   - Transaction volumes
   - User activity
   - System availability

### Alerting

Set up alerts for:
- Database connection failures
- High error rates
- Performance degradation
- Service unavailability

## Documentation and Communication

### Rollback Documentation

1. **Record rollback details**:
   - Reason for rollback
   - Changesets rolled back
   - Time and duration
   - Impact assessment
   - Lessons learned

2. **Update runbooks**:
   - Update operational procedures
   - Document new rollback scenarios
   - Improve rollback scripts

### Stakeholder Communication

1. **Before rollback**:
   - Notify all stakeholders
   - Explain impact and timeline
   - Provide status updates

2. **During rollback**:
   - Provide real-time updates
   - Communicate any issues
   - Update timeline if needed

3. **After rollback**:
   - Confirm completion
   - Report on system status
   - Schedule post-mortem if needed

## Prevention Strategies

### Reduce Rollback Need

1. **Thorough testing**:
   - Test all changes in development
   - Use staging environment that mirrors production
   - Perform integration testing

2. **Gradual deployment**:
   - Use feature flags
   - Deploy to subset of users first
   - Monitor metrics before full deployment

3. **Better changeset design**:
   - Make changes backward compatible
   - Use additive changes when possible
   - Plan rollback strategy upfront

### Improve Rollback Success

1. **Comprehensive rollback SQL**:
   - Include rollback for all changes
   - Test rollback SQL in development
   - Handle edge cases

2. **Automated rollback triggers**:
   - Set up monitoring to detect issues
   - Implement automatic rollback for critical failures
   - Use circuit breakers

3. **Regular rollback testing**:
   - Practice rollback procedures
   - Test rollback scenarios
   - Update procedures based on learnings
