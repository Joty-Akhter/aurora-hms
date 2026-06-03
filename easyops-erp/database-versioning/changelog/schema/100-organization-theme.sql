-- Organization theme / branding columns
--changeset easyops:100-organization-theme splitStatements:false

ALTER TABLE admin.organizations
    ADD COLUMN IF NOT EXISTS theme_mode           VARCHAR(10)  NOT NULL DEFAULT 'light',
    ADD COLUMN IF NOT EXISTS theme_primary_color  VARCHAR(20)  NOT NULL DEFAULT '#2563eb',
    ADD COLUMN IF NOT EXISTS theme_secondary_color VARCHAR(20) NOT NULL DEFAULT '#0891b2',
    ADD COLUMN IF NOT EXISTS theme_accent_color   VARCHAR(20)  NOT NULL DEFAULT '#7c3aed',
    ADD COLUMN IF NOT EXISTS theme_sidebar_color  VARCHAR(20)  NOT NULL DEFAULT '#1e293b',
    ADD COLUMN IF NOT EXISTS theme_sidebar_text_color VARCHAR(20) NOT NULL DEFAULT '#f1f5f9';

COMMENT ON COLUMN admin.organizations.theme_mode IS 'UI color mode: light or dark';
COMMENT ON COLUMN admin.organizations.theme_primary_color IS 'Primary brand color (hex, e.g. #2563eb)';
COMMENT ON COLUMN admin.organizations.theme_secondary_color IS 'Secondary brand color (hex)';
COMMENT ON COLUMN admin.organizations.theme_accent_color IS 'Accent/highlight color (hex)';
COMMENT ON COLUMN admin.organizations.theme_sidebar_color IS 'Sidebar background color (hex)';
COMMENT ON COLUMN admin.organizations.theme_sidebar_text_color IS 'Sidebar text/icon color (hex)';
