--liquibase formatted sql

--changeset easyops:hosp-079-aurora-sole-organization splitStatements:false
--validCheckSum: ANY
--comment: Single-tenant reset for shared DBs: keep only Aurora Hospital organization. Select Aurora (or fallback DEMO_ORG), rebrand target to Aurora metadata, repoint FK organization_id values to target across schemas, then delete non-target organizations.

DO $hosp079$
DECLARE
    target_org_id UUID;
    target_org_code TEXT;
    fk_row RECORD;
    update_sql TEXT;
BEGIN
    -- Prefer an existing Aurora org (ASHK); fallback to DEMO_ORG.
    SELECT id
      INTO target_org_id
      FROM admin.organizations
     WHERE code = 'ASHK'
     LIMIT 1;

    IF target_org_id IS NULL THEN
        SELECT id
          INTO target_org_id
          FROM admin.organizations
         WHERE code = 'DEMO_ORG'
         LIMIT 1;
    END IF;

    -- Last fallback: seed Aurora row if neither demo nor Aurora(ASHK) exists.
    IF target_org_id IS NULL THEN
        INSERT INTO admin.organizations (
            id,
            code,
            name,
            legal_name,
            description,
            logo,
            website,
            email,
            phone,
            industry,
            business_type,
            currency,
            timezone,
            locale,
            address_line1,
            address_line2,
            city,
            state,
            postal_code,
            country,
            subscription_plan,
            subscription_status,
            subscription_start_date,
            max_users,
            max_storage,
            status,
            is_active,
            created_at,
            updated_at
        ) VALUES (
            'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid,
            'ASHK',
            'Aurora Specialized Hospital',
            'Aurora Specialized Hospital Ltd.',
            'Multi-specialty hospital',
            'aurora.png',
            'https://www.aurora.hospital/',
            'info@aurora.hospital',
            '09610-989998',
            'Healthcare',
            'Hospital',
            'BDT',
            'Asia/Dhaka',
            'en-US',
            '19/1, Kakrail',
            'Kakrail',
            'Dhaka',
            'Dhaka',
            '1000',
            'BD',
            'ENTERPRISE',
            'ACTIVE',
            CURRENT_TIMESTAMP,
            500,
            10737418240,
            'ACTIVE',
            TRUE,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        RETURNING id INTO target_org_id;
    END IF;

    SELECT code INTO target_org_code
    FROM admin.organizations
    WHERE id = target_org_id;

    -- Rebrand target organization to Aurora metadata with ASHK code.
    UPDATE admin.organizations
       SET code = 'ASHK',
           name = 'Aurora Specialized Hospital',
           legal_name = 'Aurora Specialized Hospital Ltd.',
           description = 'Multi-specialty hospital',
           logo = 'aurora.png',
           website = 'https://www.aurora.hospital/',
           email = 'info@aurora.hospital',
           phone = '09610-989998',
           industry = 'Healthcare',
           business_type = 'Hospital',
           currency = 'BDT',
           timezone = 'Asia/Dhaka',
           locale = 'en-US',
           address_line1 = '19/1, Kakrail',
           address_line2 = 'Kakrail',
           city = 'Dhaka',
           state = 'Dhaka',
           postal_code = '1000',
           country = 'BD',
           subscription_plan = 'ENTERPRISE',
           subscription_status = 'ACTIVE',
           max_users = 500,
           max_storage = 10737418240,
           status = 'ACTIVE',
           is_active = TRUE,
           updated_at = CURRENT_TIMESTAMP
     WHERE id = target_org_id;

    -- If another row already had code ASHK, normalize it first to avoid unique conflicts.
    IF target_org_code IS DISTINCT FROM 'ASHK' THEN
        UPDATE admin.organizations
           SET code = CONCAT('LEGACY_', code, '_', SUBSTRING(id::text, 1, 8)),
               updated_at = CURRENT_TIMESTAMP
         WHERE code = 'ASHK'
           AND id <> target_org_id;

        UPDATE admin.organizations
           SET code = 'ASHK',
               updated_at = CURRENT_TIMESTAMP
         WHERE id = target_org_id;
    END IF;

    -- Ensure each user has a single primary org assignment before mass re-pointing.
    IF to_regclass('admin.user_organizations') IS NOT NULL THEN
        DELETE FROM admin.user_organizations uo
        WHERE uo.id IN (
            SELECT id FROM (
                SELECT id,
                       ROW_NUMBER() OVER (
                           PARTITION BY user_id
                           ORDER BY is_primary DESC NULLS LAST, joined_at DESC NULLS LAST, updated_at DESC NULLS LAST
                       ) AS rn
                FROM admin.user_organizations
            ) s
            WHERE s.rn > 1
        );

        UPDATE admin.user_organizations
        SET organization_id = target_org_id,
            is_primary = TRUE,
            status = COALESCE(status, 'ACTIVE'),
            updated_at = CURRENT_TIMESTAMP
        WHERE organization_id IS DISTINCT FROM target_org_id
           OR is_primary IS DISTINCT FROM TRUE;
    END IF;

    -- Re-point all FK organization_id values to target org across all schemas.
    FOR fk_row IN
        SELECT
            n.nspname AS schema_name,
            c.relname AS table_name,
            a.attname AS column_name
        FROM pg_constraint co
        JOIN pg_class c ON c.oid = co.conrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN LATERAL unnest(co.conkey) AS k(attnum) ON TRUE
        JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = k.attnum
        WHERE co.contype = 'f'
          AND co.confrelid = 'admin.organizations'::regclass
          AND NOT (n.nspname = 'admin' AND c.relname = 'organizations')
    LOOP
        update_sql := format(
            'UPDATE %I.%I SET %I = $1 WHERE %I IS NOT NULL AND %I IS DISTINCT FROM $1',
            fk_row.schema_name,
            fk_row.table_name,
            fk_row.column_name,
            fk_row.column_name,
            fk_row.column_name
        );
        EXECUTE update_sql USING target_org_id;
    END LOOP;

    -- Keep only Aurora organization.
    DELETE FROM admin.organizations
    WHERE id <> target_org_id;
END $hosp079$ LANGUAGE plpgsql;

