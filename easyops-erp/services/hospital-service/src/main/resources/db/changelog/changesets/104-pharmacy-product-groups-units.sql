--liquibase formatted sql

--changeset easyops:104a-pharmacy-product-groups
CREATE TABLE IF NOT EXISTS hospital_pharmacy.product_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_hp_product_groups_name ON hospital_pharmacy.product_groups (LOWER(name));

--changeset easyops:104b-pharmacy-units
CREATE TABLE IF NOT EXISTS hospital_pharmacy.units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    abbreviation VARCHAR(20) NOT NULL,
    base_unit_id UUID REFERENCES hospital_pharmacy.units(id),
    conversion_factor NUMERIC(19,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_unit_conversion CHECK (
        (base_unit_id IS NULL AND conversion_factor IS NULL)
        OR (base_unit_id IS NOT NULL AND conversion_factor IS NOT NULL)
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_hp_units_abbreviation ON hospital_pharmacy.units (LOWER(abbreviation));
