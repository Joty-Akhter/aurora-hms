-- Validation script for alien-pharma.sql
-- This script validates that all required tables and constraints exist
-- Run this BEFORE running alien-pharma.sql to ensure the database is ready

DO $$
DECLARE
    missing_tables TEXT[] := ARRAY[]::TEXT[];
    missing_schemas TEXT[] := ARRAY[]::TEXT[];
    schema_check TEXT;
    table_check TEXT;
    required_schemas TEXT[] := ARRAY['admin', 'users', 'rbac', 'inventory'];
    required_tables TEXT[] := ARRAY[
        'admin.organizations',
        'admin.user_organizations',
        'users.users',
        'rbac.roles',
        'rbac.user_roles',
        'inventory.product_categories',
        'inventory.products'
    ];
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Validating database structure for alien-pharma.sql';
    RAISE NOTICE '========================================';
    
    -- Check required schemas
    RAISE NOTICE 'Checking required schemas...';
    FOREACH schema_check IN ARRAY required_schemas
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.schemata s
            WHERE s.schema_name = schema_check
        ) THEN
            missing_schemas := array_append(missing_schemas, schema_check);
            RAISE WARNING 'Missing schema: %', schema_check;
        ELSE
            RAISE NOTICE '✓ Schema exists: %', schema_check;
        END IF;
    END LOOP;
    
    -- Check required tables
    RAISE NOTICE 'Checking required tables...';
    FOREACH table_check IN ARRAY required_tables
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables t
            WHERE t.table_schema = split_part(table_check, '.', 1)
            AND t.table_name = split_part(table_check, '.', 2)
        ) THEN
            missing_tables := array_append(missing_tables, table_check);
            RAISE WARNING 'Missing table: %', table_check;
        ELSE
            RAISE NOTICE '✓ Table exists: %', table_check;
        END IF;
    END LOOP;
    
    -- Check unique constraints
    RAISE NOTICE 'Checking unique constraints...';
    
    -- Check rbac.user_roles constraint
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'rbac'
        AND t.relname = 'user_roles'
        AND c.contype = 'u'
        AND array_length(c.conkey, 1) = 3
    ) THEN
        RAISE NOTICE '✓ Unique constraint exists on rbac.user_roles (user_id, role_id, organization_id)';
    ELSE
        RAISE WARNING 'Missing or incorrect unique constraint on rbac.user_roles';
    END IF;
    
    -- Check admin.user_organizations constraint
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'admin'
        AND t.relname = 'user_organizations'
        AND c.contype = 'u'
        AND array_length(c.conkey, 1) = 2
    ) THEN
        RAISE NOTICE '✓ Unique constraint exists on admin.user_organizations (user_id, organization_id)';
    ELSE
        RAISE WARNING 'Missing or incorrect unique constraint on admin.user_organizations';
    END IF;
    
    -- Summary
    RAISE NOTICE '========================================';
    IF array_length(missing_schemas, 1) IS NULL AND array_length(missing_tables, 1) IS NULL THEN
        RAISE NOTICE '✓ All required schemas and tables exist!';
        RAISE NOTICE '✓ Database is ready for alien-pharma.sql';
    ELSE
        RAISE WARNING '✗ Some required schemas or tables are missing!';
        RAISE WARNING 'Please ensure all database migrations have been run.';
    END IF;
    RAISE NOTICE '========================================';
END $$;
