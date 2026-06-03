--liquibase formatted sql

--changeset easyops:064-organization-app-data-seed
--comment: Seed basic UOM and gender values into admin.organization_app_data for all organizations

SET search_path TO admin, public;

-- Seed UOMs: GRAM, ML, PCS for each organization
INSERT INTO admin.organization_app_data (
    id, organization_id, type, code, name, description, extra_attributes,
    is_active, display_order, created_at, updated_at, created_by, updated_by
)
SELECT
    gen_random_uuid(),
    o.id AS organization_id,
    'UOM' AS type,
    u.code,
    u.name,
    u.description,
    u.extra_attributes::jsonb,
    TRUE AS is_active,
    u.display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
FROM admin.organizations o
CROSS JOIN (
    VALUES
        ('GRAM', 'Gram', 'Gram (g) unit of mass', '{"category":"MASS"}', 1),
        ('ML',   'Milliliter', 'Milliliter (ml) unit of volume', '{"category":"VOLUME"}', 2),
        ('PCS',  'Pieces', 'Pieces (pcs) unit of count', '{"category":"COUNT"}', 3)
) AS u(code, name, description, extra_attributes, display_order)
ON CONFLICT (organization_id, type, code) DO NOTHING;

-- Seed GENDERs for each organization
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
        ('OTHER', 'Other', 'Other / non-binary gender', 3),
        ('PREFER_NOT_TO_SAY', 'Prefer not to say', 'Prefer not to disclose gender', 4)
) AS g(code, name, description, display_order)
ON CONFLICT (organization_id, type, code) DO NOTHING;

--rollback DELETE FROM admin.organization_app_data WHERE type IN ('UOM','GENDER');

