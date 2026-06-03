--liquibase formatted sql

--changeset easyops:306-fix-pharma-field-types context:inventory splitStatements:false
--comment: Fix pack_size and box_size field types to match BigDecimal in entity
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'products' AND column_name = 'pack_size' AND data_type = 'character varying') THEN
        ALTER TABLE inventory.products ALTER COLUMN pack_size TYPE DECIMAL(10, 2) USING (CASE WHEN pack_size ~ '^[0-9.]+$' THEN pack_size::numeric ELSE 1 END);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'inventory' AND table_name = 'products' AND column_name = 'box_size' AND data_type = 'integer') THEN
        ALTER TABLE inventory.products ALTER COLUMN box_size TYPE DECIMAL(10, 2) USING box_size::numeric;
    END IF;
END $$;
