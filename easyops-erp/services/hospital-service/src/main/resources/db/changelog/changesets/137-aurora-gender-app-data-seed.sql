--liquibase formatted sql

--changeset hospital-service:137-aurora-gender-app-data-seed
--comment: Seed GENDER lookup values for Aurora Specialized Hospital (ASHK) organization

SET search_path TO admin, public;

INSERT INTO admin.organization_app_data (
    id, organization_id, type, code, name, description, extra_attributes,
    is_active, display_order, created_at, updated_at, created_by, updated_by
)
SELECT
    gen_random_uuid(),
    o.id AS organization_id,
    'GENDER' AS type,
    g.code,
    g.name,
    g.description,
    NULL::jsonb AS extra_attributes,
    TRUE AS is_active,
    g.display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
FROM admin.organizations o
CROSS JOIN (
    VALUES
        ('MALE', 'Male', 'Male gender', 1),
        ('FEMALE', 'Female', 'Female gender', 2),
        ('OTHER', 'Others', 'Other / non-binary gender', 3)
) AS g(code, name, description, display_order)
WHERE o.code = 'ASHK'
ON CONFLICT (organization_id, type, code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = TRUE,
    display_order = EXCLUDED.display_order,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system';

--rollback DELETE FROM admin.organization_app_data WHERE type = 'GENDER' AND organization_id IN (SELECT id FROM admin.organizations WHERE code = 'ASHK');
