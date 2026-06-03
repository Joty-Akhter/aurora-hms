# Legacy Database Scripts Backup

This directory contains the original database initialization scripts that were used before implementing Liquibase database versioning.

## What's Here

- `01-init.sql` - Original database initialization script
- `_phase_1.1_accounting_schema.sql.inc` - Phase 1.1 accounting schema
- `_phase_1.2_ar_ap_bank_schema.sql.inc` - Phase 1.2 AR/AP/Bank schema  
- `_phase_2_sales_schema.sql.inc` - Phase 2 sales schema
- `_test_data.sql.inc` - Test data
- `_update_schema.sql.inc` - Schema updates
- `_fix_*.sql` - Various fix scripts
- `_coa_template_standard.sql.template` - Chart of Accounts template

## Migration to Liquibase

All these scripts have been converted to Liquibase changesets and moved to:
`easyops-erp/database-versioning/changelog/`

The new Liquibase-based approach provides:
- Better version control
- Rollback capabilities
- Environment-specific deployments
- Atomic changesets
- Better change tracking

## Usage

These files are kept for reference only. For new database changes, use the Liquibase migration system in the `database-versioning/` directory.

## Conversion Status

✅ **Completed**: All scripts have been converted to Liquibase changesets
- Phase 0: Core system schema
- Phase 1: Accounting module (CoA, AR/AP/Bank)
- Phase 2: Sales module
- Data: Default roles, permissions, configuration

## See Also

- `easyops-erp/database-versioning/README.md` - New database versioning documentation
- `easyops-erp/database-versioning/docs/migration-guide.md` - Migration procedures
- `easyops-erp/database-versioning/docs/rollback-procedures.md` - Rollback procedures
