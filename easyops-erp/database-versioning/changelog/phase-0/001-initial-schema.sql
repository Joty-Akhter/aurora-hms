--liquibase formatted sql

--changeset easyops:001-create-databases context:initial runOnChange:false
--comment: Database creation is handled by Docker postgres initialization
--comment: The easyops database is automatically created via POSTGRES_DB environment variable
--comment: Skipping this changeset as it's not needed
--rollback: -- No rollback needed

--changeset easyops:002-create-dev-user context:initial runOnChange:false
--comment: User creation - using default easyops user from Docker configuration
--comment: Additional users can be created here if needed in the future
--rollback: -- No rollback needed

--changeset easyops:003-create-core-schemas context:initial
--comment: Create core schemas for different modules
CREATE SCHEMA IF NOT EXISTS admin;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS rbac;
CREATE SCHEMA IF NOT EXISTS system;
CREATE SCHEMA IF NOT EXISTS audit;

--changeset easyops:004-create-extensions context:initial
--comment: Create required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

--changeset easyops:005-create-users-table context:initial
--comment: Create users table
CREATE TABLE users.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_users_email ON users.users(email);
CREATE INDEX idx_users_username ON users.users(username);
CREATE INDEX idx_users_active ON users.users(is_active);

--changeset easyops:006-create-organizations-table context:initial
--comment: Create organizations table
CREATE TABLE admin.organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    description TEXT,
    logo VARCHAR(500),
    website VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    industry VARCHAR(100),
    business_type VARCHAR(100),
    tax_id VARCHAR(100),
    registration_number VARCHAR(100),
    fiscal_year_start DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(100) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    subscription_plan VARCHAR(50) DEFAULT 'FREE',
    subscription_status VARCHAR(50) DEFAULT 'TRIAL',
    subscription_start_date TIMESTAMP WITH TIME ZONE,
    subscription_end_date TIMESTAMP WITH TIME ZONE,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    max_users INTEGER DEFAULT 10,
    max_storage BIGINT DEFAULT 1073741824,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_organizations_code ON admin.organizations(code);
CREATE INDEX idx_organizations_status ON admin.organizations(status);
CREATE INDEX idx_organizations_subscription ON admin.organizations(subscription_status);

--changeset easyops:007-create-organization-settings-table context:initial
--comment: Create organization settings table
CREATE TABLE admin.organization_settings (
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

CREATE INDEX idx_org_settings_org ON admin.organization_settings(organization_id);

--changeset easyops:008-create-departments-table context:initial
--comment: Create departments table
CREATE TABLE admin.departments (
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

CREATE INDEX idx_departments_org ON admin.departments(organization_id);
CREATE INDEX idx_departments_parent ON admin.departments(parent_department_id);
CREATE INDEX idx_departments_manager ON admin.departments(manager_id);

--changeset easyops:009-create-locations-table context:initial
--comment: Create locations table
CREATE TABLE admin.locations (
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

CREATE INDEX idx_locations_org ON admin.locations(organization_id);
CREATE INDEX idx_locations_manager ON admin.locations(manager_id);

--changeset easyops:010-create-invitations-table context:initial
--comment: Create invitations table
CREATE TABLE admin.invitations (
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

CREATE INDEX idx_invitations_org ON admin.invitations(organization_id);
CREATE INDEX idx_invitations_email ON admin.invitations(email);
CREATE INDEX idx_invitations_token ON admin.invitations(token);

--changeset easyops:011-create-user-organizations-table context:initial
--comment: Create user organizations table
CREATE TABLE admin.user_organizations (
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

CREATE INDEX idx_user_organizations_user_id ON admin.user_organizations(user_id);
CREATE INDEX idx_user_organizations_org_id ON admin.user_organizations(organization_id);

--changeset easyops:012-create-system-configurations-table context:initial
--comment: Create system configurations table
CREATE TABLE system.configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(255) UNIQUE NOT NULL,
    value TEXT,
    description TEXT,
    category VARCHAR(100),
    is_encrypted BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_configurations_key ON system.configurations(key);

--changeset easyops:013-create-audit-logs-table context:initial
--comment: Create audit logs table
CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit.audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit.audit_logs(created_at);
CREATE INDEX idx_audit_logs_action ON audit.audit_logs(action);

--changeset easyops:014-create-updated-at-trigger-function context:initial splitStatements:false
--comment: Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

--changeset easyops:014b-ensure-updated-at-trigger-function splitStatements:false
--comment: Ensure updated_at trigger function exists in public schema for environments that did not run 'initial' context
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

--changeset easyops:015-create-updated-at-triggers context:initial
--comment: Create updated_at triggers for tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users.users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON admin.organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_org_settings_updated_at BEFORE UPDATE ON admin.organization_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON admin.departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON admin.locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_organizations_updated_at BEFORE UPDATE ON admin.user_organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_configurations_updated_at BEFORE UPDATE ON system.configurations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:016-grant-permissions context:initial splitStatements:false
--comment: Grant all privileges to easyops user (default user from Docker configuration), if role exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'easyops') THEN
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA admin TO easyops;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO easyops;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO easyops;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA rbac TO easyops;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA system TO easyops;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO easyops;

        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA admin TO easyops;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA users TO easyops;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth TO easyops;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA rbac TO easyops;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA system TO easyops;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO easyops;
    END IF;
END;
$$
