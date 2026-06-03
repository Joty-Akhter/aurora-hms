-- Update schema for Phase 0.2 Organization Service

-- Organization settings table
CREATE TABLE IF NOT EXISTS admin.organization_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(50),
    is_encrypted BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, setting_key)
);

-- Departments table
CREATE TABLE IF NOT EXISTS admin.departments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    parent_department_id UUID REFERENCES admin.departments(id) ON DELETE SET NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) DEFAULT 'DEPARTMENT',
    manager_id UUID,
    cost_center VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, code)
);

-- Locations table
CREATE TABLE IF NOT EXISTS admin.locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) DEFAULT 'BRANCH',
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    phone VARCHAR(50),
    email VARCHAR(255),
    manager_id UUID,
    timezone VARCHAR(100),
    operating_hours JSONB,
    coordinates JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, code)
);

-- Invitations table
CREATE TABLE IF NOT EXISTS admin.invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    invited_by UUID NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User organizations table (enhanced)
CREATE TABLE IF NOT EXISTS admin.user_organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    is_primary BOOLEAN DEFAULT false,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    invited_by UUID,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    permissions JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, organization_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_org_settings_org ON admin.organization_settings(organization_id);
CREATE INDEX IF NOT EXISTS idx_departments_org ON admin.departments(organization_id);
CREATE INDEX IF NOT EXISTS idx_departments_parent ON admin.departments(parent_department_id);
CREATE INDEX IF NOT EXISTS idx_departments_manager ON admin.departments(manager_id);
CREATE INDEX IF NOT EXISTS idx_locations_org ON admin.locations(organization_id);
CREATE INDEX IF NOT EXISTS idx_locations_manager ON admin.locations(manager_id);
CREATE INDEX IF NOT EXISTS idx_user_organizations_user_id ON admin.user_organizations(user_id);
CREATE INDEX IF NOT EXISTS idx_user_organizations_org_id ON admin.user_organizations(organization_id);
CREATE INDEX IF NOT EXISTS idx_invitations_org ON admin.invitations(organization_id);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON admin.invitations(email);
CREATE INDEX IF NOT EXISTS idx_invitations_token ON admin.invitations(token);

-- Create triggers (assuming function exists) - Drop first if they exist
DROP TRIGGER IF EXISTS update_org_settings_updated_at ON admin.organization_settings;
DROP TRIGGER IF EXISTS update_departments_updated_at ON admin.departments;
DROP TRIGGER IF EXISTS update_locations_updated_at ON admin.locations;
DROP TRIGGER IF EXISTS update_user_organizations_updated_at ON admin.user_organizations;

CREATE TRIGGER update_org_settings_updated_at BEFORE UPDATE ON admin.organization_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON admin.departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON admin.locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_organizations_updated_at BEFORE UPDATE ON admin.user_organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update organizations table with new columns
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS legal_name VARCHAR(255);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS logo VARCHAR(500);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS website VARCHAR(255);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS industry VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS business_type VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS tax_id VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS registration_number VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS fiscal_year_start DATE;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS timezone VARCHAR(100) DEFAULT 'UTC';
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS locale VARCHAR(10) DEFAULT 'en-US';
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS country VARCHAR(2);
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50) DEFAULT 'FREE';
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(50) DEFAULT 'TRIAL';
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS subscription_start_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS subscription_end_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS max_users INTEGER DEFAULT 10;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS max_storage BIGINT DEFAULT 1073741824;
ALTER TABLE admin.organizations ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
