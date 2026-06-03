--liquibase formatted sql

--changeset easyops:063-organization-app-data
--comment: Create organization-level master data table (UOM, gender, etc.)

SET search_path TO admin, public;

CREATE TABLE admin.organization_app_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    extra_attributes JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_org_app_data UNIQUE (organization_id, type, code)
);

CREATE INDEX idx_org_app_data_org_type ON admin.organization_app_data(organization_id, type);
CREATE INDEX idx_org_app_data_type_code ON admin.organization_app_data(type, code);

--rollback DROP TABLE IF EXISTS admin.organization_app_data CASCADE;

