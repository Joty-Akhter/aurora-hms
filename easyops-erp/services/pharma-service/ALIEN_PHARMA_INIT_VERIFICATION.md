# Alien Pharma Initialization Script Verification

## Configuration Status

### ✅ Java Configuration
- **Class**: `AlienPharmaDataInitializer`
- **Location**: `services/pharma-service/src/main/java/com/easyops/pharma/config/AlienPharmaDataInitializer.java`
- **Component**: `@Component` - automatically discovered by Spring
- **Event Listener**: `ApplicationListener<ContextRefreshedEvent>` - runs when Spring context is refreshed
- **Initialization Flag**: `volatile boolean initialized` - prevents multiple executions

### ✅ Configuration Properties
- **Property**: `pharma.alien-pharma.init.enabled`
- **Default**: `true` (enabled by default)
- **Location**: `services/pharma-service/src/main/resources/application.yml` (lines 42-45)
- **Can be disabled**: Set `PHARMA_ALIEN_PHARMA_INIT_ENABLED=false` environment variable

### ✅ SQL Script
- **File**: `alien-pharma.sql`
- **Location**: `services/pharma-service/src/main/resources/sql/alien-pharma.sql`
- **Path in code**: `sql/alien-pharma.sql` (ClassPathResource)
- **Status**: ✅ File exists
- **ScriptUtils-safe**: ✅ No DO $$ blocks, all statements properly terminated

## Execution Flow

1. **Spring Context Refresh** → `ContextRefreshedEvent` fired
2. **Check Initialization Flag** → If already initialized, skip
3. **Check Enabled Flag** → If disabled, log and skip
4. **Run Script** → `runScript("sql/alien-pharma.sql", true)` with `continueOnError=true`
5. **Verify UOMs** → Check if UOMs were created successfully
6. **Fallback** → If script fails, attempt UOM fallback insertion

## Script Contents

The script performs:
1. ✅ Deletes all existing organizations and related data (with `continueOnError=true` to handle missing tables)
2. ✅ Creates Alien Pharma organization
3. ✅ Creates roles and permissions
4. ✅ Links users to organization
5. ✅ Creates departments (admin and hr schemas)
6. ✅ Creates product categories
7. ✅ Creates HR departments and positions
8. ✅ Creates employee record for admin user
9. ✅ Creates pharmaceutical products (7 products with TP/MRP)
10. ✅ Inserts UOMs (GRAM, ML, PCS)

## Potential Issues

### ⚠️ Error Handling
- Script runs with `continueOnError=true`, so some DELETE statements may fail silently if tables don't exist
- This is intentional to handle schema variations

### ⚠️ Transaction Behavior
- Script runs with `autoCommit=true`, so each statement commits immediately
- If a statement fails, previous statements are already committed
- This is acceptable for initialization scripts

### ⚠️ Multiple Executions
- Protected by `volatile boolean initialized` flag
- Will only run once per application context refresh
- If service restarts, it will run again (which is desired for fresh initialization)

## Verification Steps

To verify the script is running at startup:

1. **Check Logs** - Look for these log messages:
   ```
   Starting Alien Pharma data initialization...
   Running alien-pharma.sql initialization script...
   Executed script: sql/alien-pharma.sql
   Alien Pharma UOMs in organization_app_data: 3
   Alien Pharma data initialization completed successfully
   ```

2. **Check Database** - After startup, verify:
   ```sql
   SELECT code, name FROM admin.organizations WHERE code = 'ALIEN_PHARMA';
   SELECT COUNT(*) FROM inventory.products WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA');
   SELECT COUNT(*) FROM hr.positions WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA');
   SELECT COUNT(*) FROM admin.organization_app_data WHERE type = 'UOM' AND organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA');
   ```

3. **Check for Errors** - Look for error messages in logs:
   ```
   Failed to initialize Alien Pharma data: ...
   ```

## Troubleshooting

### Script Not Running
- Check if `pharma.alien-pharma.init.enabled=true` in application.yml
- Check if `PHARMA_ALIEN_PHARMA_INIT_ENABLED` environment variable is set to `false`
- Check logs for "Alien Pharma data initialization is DISABLED"

### Script Failing
- Check database connection settings
- Check if database user has necessary permissions
- Check logs for specific SQL errors
- Verify script file exists in classpath: `src/main/resources/sql/alien-pharma.sql`

### Data Not Created
- Check if script executed successfully (look for "Executed script" log)
- Verify UOM count (should be 3)
- Check if organization was created
- Verify products were created (should be 7)

## Testing

To test manually:
```bash
# Test script execution via psql
docker exec -i easyops-postgres psql -U easyops -d easyops < services/pharma-service/src/main/resources/sql/alien-pharma.sql

# Or via Spring ScriptUtils (requires running service)
# The script will run automatically on service startup
```
