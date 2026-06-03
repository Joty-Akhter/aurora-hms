-- Initialize EasyOps ERP Database
-- This script sets up the initial database structure for Phase 0

-- Create development databases (only if they don't exist)
SELECT 'CREATE DATABASE easyops'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'easyops')\gexec

SELECT 'CREATE DATABASE easyops_test'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'easyops_test')\gexec

-- Create development user (only if it doesn't exist)
DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'easyops_dev') THEN
      CREATE USER easyops_dev WITH PASSWORD 'easyops123';
   END IF;
END
$do$;

GRANT ALL PRIVILEGES ON DATABASE easyops TO easyops_dev;
GRANT ALL PRIVILEGES ON DATABASE easyops_test TO easyops_dev;

-- Connect to main database
\c easyops;

-- Create schemas for different modules
CREATE SCHEMA IF NOT EXISTS admin;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS rbac;
CREATE SCHEMA IF NOT EXISTS system;
CREATE SCHEMA IF NOT EXISTS audit;

-- Grant permissions to development user
GRANT ALL PRIVILEGES ON SCHEMA admin TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA users TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA auth TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA rbac TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA system TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA audit TO easyops_dev;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
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

-- Organizations table (Enhanced for Phase 0.2)
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
    -- Business Information
    industry VARCHAR(100),
    business_type VARCHAR(100),
    tax_id VARCHAR(100),
    registration_number VARCHAR(100),
    fiscal_year_start DATE,
    currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(100) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    -- Address Information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),
    -- Subscription Information
    subscription_plan VARCHAR(50) DEFAULT 'FREE',
    subscription_status VARCHAR(50) DEFAULT 'TRIAL',
    subscription_start_date TIMESTAMP WITH TIME ZONE,
    subscription_end_date TIMESTAMP WITH TIME ZONE,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    max_users INTEGER DEFAULT 10,
    max_storage BIGINT DEFAULT 1073741824, -- 1GB in bytes
    -- Status & Metadata
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Organization settings table
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

-- Departments table
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

-- Locations table
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

-- Invitations table
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

-- User organizations (many-to-many) - Enhanced
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

-- Roles table
CREATE TABLE rbac.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Permissions table
CREATE TABLE rbac.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Role permissions (many-to-many)
CREATE TABLE rbac.role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES rbac.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES rbac.permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- User roles (many-to-many)
CREATE TABLE rbac.user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES rbac.roles(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES admin.organizations(id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    expires_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, role_id, organization_id)
);

-- System configuration table
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

-- Audit logs table
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

-- Create indexes for performance
CREATE INDEX idx_users_email ON users.users(email);
CREATE INDEX idx_users_username ON users.users(username);
CREATE INDEX idx_users_active ON users.users(is_active);
CREATE INDEX idx_organizations_code ON admin.organizations(code);
CREATE INDEX idx_organizations_status ON admin.organizations(status);
CREATE INDEX idx_organizations_subscription ON admin.organizations(subscription_status);
CREATE INDEX idx_org_settings_org ON admin.organization_settings(organization_id);
CREATE INDEX idx_departments_org ON admin.departments(organization_id);
CREATE INDEX idx_departments_parent ON admin.departments(parent_department_id);
CREATE INDEX idx_departments_manager ON admin.departments(manager_id);
CREATE INDEX idx_locations_org ON admin.locations(organization_id);
CREATE INDEX idx_locations_manager ON admin.locations(manager_id);
CREATE INDEX idx_user_organizations_user_id ON admin.user_organizations(user_id);
CREATE INDEX idx_user_organizations_org_id ON admin.user_organizations(organization_id);
CREATE INDEX idx_invitations_org ON admin.invitations(organization_id);
CREATE INDEX idx_invitations_email ON admin.invitations(email);
CREATE INDEX idx_invitations_token ON admin.invitations(token);
CREATE INDEX idx_roles_code ON rbac.roles(code);
CREATE INDEX idx_permissions_code ON rbac.permissions(code);
CREATE INDEX idx_permissions_resource ON rbac.permissions(resource);
CREATE INDEX idx_user_roles_user_id ON rbac.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON rbac.user_roles(role_id);
CREATE INDEX idx_configurations_key ON system.configurations(key);
CREATE INDEX idx_audit_logs_user_id ON audit.audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit.audit_logs(created_at);
CREATE INDEX idx_audit_logs_action ON audit.audit_logs(action);

-- Insert default system roles
INSERT INTO rbac.roles (name, code, description, is_system_role) VALUES
('System Administrator', 'SYSTEM_ADMIN', 'Full system access', true),
('Organization Administrator', 'ORG_ADMIN', 'Organization-level administration', true),
('User', 'USER', 'Standard user access', true),
('Guest', 'GUEST', 'Limited guest access', true);

-- User sessions table (for authentication service)
CREATE TABLE auth.user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    token VARCHAR(4096) UNIQUE NOT NULL,
    refresh_token VARCHAR(4096) UNIQUE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(1024),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE
);

-- Password reset tokens table
CREATE TABLE auth.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    is_used BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

-- Login attempts table
CREATE TABLE auth.login_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    successful BOOLEAN DEFAULT false,
    failure_reason VARCHAR(255),
    attempted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for auth tables
CREATE INDEX idx_user_sessions_user_id ON auth.user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON auth.user_sessions(token);
CREATE INDEX idx_user_sessions_refresh_token ON auth.user_sessions(refresh_token);
CREATE INDEX idx_user_sessions_is_active ON auth.user_sessions(is_active);
CREATE INDEX idx_password_reset_tokens_user_id ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON auth.password_reset_tokens(token);
CREATE INDEX idx_login_attempts_username ON auth.login_attempts(username);
CREATE INDEX idx_login_attempts_attempted_at ON auth.login_attempts(attempted_at);

-- Insert default permissions
INSERT INTO rbac.permissions (name, code, resource, action, description) VALUES
('User Management', 'USER_MANAGE', 'users', 'manage', 'Manage user accounts'),
('User View', 'USER_VIEW', 'users', 'view', 'View user information'),
('Role Management', 'ROLE_MANAGE', 'roles', 'manage', 'Manage roles and permissions'),
('Role View', 'ROLE_VIEW', 'roles', 'view', 'View roles and permissions'),
('System Configuration', 'SYSTEM_CONFIG', 'system', 'configure', 'Configure system settings'),
('System View', 'SYSTEM_VIEW', 'system', 'view', 'View system information'),
('Audit Logs', 'AUDIT_VIEW', 'audit', 'view', 'View audit logs'),
('Organization Management', 'ORG_MANAGE', 'organizations', 'manage', 'Manage organizations');

-- Assign permissions to System Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'SYSTEM_ADMIN';

-- Assign basic permissions to Organization Administrator role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'ORG_ADMIN'
AND p.code IN ('USER_MANAGE', 'USER_VIEW', 'ROLE_VIEW', 'ROLE_MANAGE', 'SYSTEM_VIEW', 'AUDIT_VIEW', 'ORG_MANAGE');

-- Assign view permissions to User role
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r, rbac.permissions p
WHERE r.code = 'USER' 
AND p.code IN ('USER_VIEW', 'SYSTEM_VIEW');

-- Insert default system configuration
INSERT INTO system.configurations (key, value, description, category) VALUES
('system.name', 'EasyOps ERP', 'System name', 'general'),
('system.version', '1.0.0', 'System version', 'general'),
('system.timezone', 'UTC', 'Default timezone', 'general'),
('auth.password.min_length', '8', 'Minimum password length', 'security'),
('auth.password.require_uppercase', 'true', 'Require uppercase letters in password', 'security'),
('auth.password.require_lowercase', 'true', 'Require lowercase letters in password', 'security'),
('auth.password.require_numbers', 'true', 'Require numbers in password', 'security'),
('auth.password.require_symbols', 'true', 'Require symbols in password', 'security'),
('auth.session.timeout', '3600', 'Session timeout in seconds', 'security'),
('auth.max_login_attempts', '5', 'Maximum login attempts before lockout', 'security'),
('auth.lockout.duration', '900', 'Account lockout duration in seconds', 'security');

-- Create a default organization
INSERT INTO admin.organizations (name, code, description) VALUES
('EasyOps Demo Organization', 'DEMO_ORG', 'Default demo organization for testing');

-- Create a default system administrator user  
-- Password: Admin123! (hashed with bcrypt)
-- Temporarily using plain text for testing - MUST CHANGE IN PRODUCTION
INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified) VALUES
('admin', 'admin@easyops.com', 'TEMP_PLAIN_TEXT_Admin123!', 'System', 'Administrator', true, true) 
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- Assign system administrator role to admin user
INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u, rbac.roles r
WHERE u.username = 'admin' AND r.code = 'SYSTEM_ADMIN';

-- Assign admin user to default organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'OWNER', true
FROM users.users u, admin.organizations o
WHERE u.username = 'admin' AND o.code = 'DEMO_ORG';

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users.users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON admin.organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_org_settings_updated_at BEFORE UPDATE ON admin.organization_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON admin.departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON admin.locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_organizations_updated_at BEFORE UPDATE ON admin.user_organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON rbac.roles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_permissions_updated_at BEFORE UPDATE ON rbac.permissions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_configurations_updated_at BEFORE UPDATE ON system.configurations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant all privileges to easyops_dev user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA admin TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA rbac TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA system TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO easyops_dev;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA admin TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA users TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA rbac TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA system TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO easyops_dev;

-- ============================================================
-- PHASE 0.3: Notifications, Monitoring & Integration Schemas
-- ============================================================

-- Create schemas for Phase 0.3
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE SCHEMA IF NOT EXISTS integration;

-- Grant permissions to development user
GRANT ALL PRIVILEGES ON SCHEMA notifications TO easyops_dev;
GRANT ALL PRIVILEGES ON SCHEMA integration TO easyops_dev;

-- ============================================================
-- NOTIFICATIONS SCHEMA
-- ============================================================

-- In-app notifications table
CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES admin.organizations(id) ON DELETE CASCADE,
    type VARCHAR(50) DEFAULT 'INFO', -- INFO, WARNING, ERROR, SUCCESS
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Email logs table
CREATE TABLE notifications.email_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    to_email VARCHAR(255) NOT NULL,
    from_email VARCHAR(255),
    subject VARCHAR(255),
    template_name VARCHAR(100),
    template_variables JSONB,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, SENT, FAILED, BOUNCED
    sent_at TIMESTAMP WITH TIME ZONE,
    failed_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    organization_id UUID REFERENCES admin.organizations(id),
    user_id UUID REFERENCES users.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Email templates table
CREATE TABLE notifications.email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_html TEXT NOT NULL,
    body_text TEXT,
    variables TEXT[],
    locale VARCHAR(10) DEFAULT 'en-US',
    organization_id UUID REFERENCES admin.organizations(id), -- NULL for global templates
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Notification preferences table
CREATE TABLE notifications.notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN DEFAULT true,
    in_app_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT false,
    notification_types JSONB, -- Which notification types to receive
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- ============================================================
-- INTEGRATION SCHEMA (Webhooks)
-- ============================================================

-- Webhooks table
CREATE TABLE integration.webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255), -- For HMAC signature
    events TEXT[], -- Array of event types
    is_active BOOLEAN DEFAULT true,
    retry_count INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Webhook deliveries table
CREATE TABLE integration.webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID NOT NULL REFERENCES integration.webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    delivered_at TIMESTAMP WITH TIME ZONE,
    retry_attempt INTEGER DEFAULT 0,
    success BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SYSTEM MONITORING & METRICS
-- ============================================================

-- System metrics table
CREATE TABLE system.metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION,
    metric_type VARCHAR(50), -- COUNTER, GAUGE, HISTOGRAM, TIMER
    tags JSONB,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Alerts table
CREATE TABLE system.alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(50), -- CRITICAL, WARNING, INFO
    service_name VARCHAR(100),
    message TEXT NOT NULL,
    details JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED
    acknowledged_by UUID REFERENCES users.users(id),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Service health tracking table
CREATE TABLE system.service_health (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL, -- UP, DOWN, DEGRADED, UNKNOWN
    last_check_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    error_message TEXT,
    metadata JSONB
);

-- System settings table
CREATE TABLE system.settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(100),
    key VARCHAR(100) UNIQUE NOT NULL,
    value TEXT,
    data_type VARCHAR(50),
    is_encrypted BOOLEAN DEFAULT false,
    description TEXT,
    is_readonly BOOLEAN DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users.users(id)
);

-- ============================================================
-- INDEXES FOR PHASE 0.3
-- ============================================================

-- Notifications indexes
CREATE INDEX idx_notifications_user_id ON notifications.notifications(user_id);
CREATE INDEX idx_notifications_org_id ON notifications.notifications(organization_id);
CREATE INDEX idx_notifications_is_read ON notifications.notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications.notifications(created_at);
CREATE INDEX idx_notifications_priority ON notifications.notifications(priority);

-- Email logs indexes
CREATE INDEX idx_email_logs_to_email ON notifications.email_logs(to_email);
CREATE INDEX idx_email_logs_status ON notifications.email_logs(status);
CREATE INDEX idx_email_logs_user_id ON notifications.email_logs(user_id);
CREATE INDEX idx_email_logs_org_id ON notifications.email_logs(organization_id);
CREATE INDEX idx_email_logs_created_at ON notifications.email_logs(created_at);

-- Email templates indexes
CREATE INDEX idx_email_templates_name ON notifications.email_templates(name);
CREATE INDEX idx_email_templates_org_id ON notifications.email_templates(organization_id);
CREATE INDEX idx_email_templates_is_active ON notifications.email_templates(is_active);

-- Webhooks indexes
CREATE INDEX idx_webhooks_org_id ON integration.webhooks(organization_id);
CREATE INDEX idx_webhooks_is_active ON integration.webhooks(is_active);
CREATE INDEX idx_webhook_deliveries_webhook_id ON integration.webhook_deliveries(webhook_id);
CREATE INDEX idx_webhook_deliveries_created_at ON integration.webhook_deliveries(created_at);
CREATE INDEX idx_webhook_deliveries_success ON integration.webhook_deliveries(success);

-- Metrics indexes
CREATE INDEX idx_metrics_service_name ON system.metrics(service_name);
CREATE INDEX idx_metrics_metric_name ON system.metrics(metric_name);
CREATE INDEX idx_metrics_recorded_at ON system.metrics(recorded_at);

-- Alerts indexes
CREATE INDEX idx_alerts_status ON system.alerts(status);
CREATE INDEX idx_alerts_service_name ON system.alerts(service_name);
CREATE INDEX idx_alerts_alert_type ON system.alerts(alert_type);
CREATE INDEX idx_alerts_created_at ON system.alerts(created_at);

-- Service health indexes
CREATE INDEX idx_service_health_service_name ON system.service_health(service_name);
CREATE INDEX idx_service_health_status ON system.service_health(status);
CREATE INDEX idx_service_health_last_check ON system.service_health(last_check_at);

-- Settings indexes
CREATE INDEX idx_settings_key ON system.settings(key);
CREATE INDEX idx_settings_category ON system.settings(category);

-- ============================================================
-- DEFAULT EMAIL TEMPLATES FOR PHASE 0.3
-- ============================================================

-- Insert default email templates
INSERT INTO notifications.email_templates (name, subject, body_html, body_text, variables) VALUES
('user_invitation', 
 'You''re invited to join {{organization.name}}', 
 '<html><body><h2>You''re Invited!</h2><p>{{invited_by.name}} has invited you to join {{organization.name}} as a {{invitation.role}}.</p><p><a href="{{invitation.link}}" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Accept Invitation</a></p><p>This invitation expires on {{invitation.expiresAt}}.</p></body></html>',
 'You''re invited to join {{organization.name}} by {{invited_by.name}}. Click here to accept: {{invitation.link}}. This invitation expires on {{invitation.expiresAt}}.',
 ARRAY['organization.name', 'invited_by.name', 'invitation.link', 'invitation.role', 'invitation.expiresAt']),

('password_reset', 
 'Reset your password for {{system.name}}', 
 '<html><body><h2>Password Reset Request</h2><p>Hello {{user.name}},</p><p>We received a request to reset your password. Click the button below to reset it:</p><p><a href="{{reset.link}}" style="background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Reset Password</a></p><p>This link expires in 1 hour ({{reset.expiresAt}}).</p><p>If you didn''t request this, please ignore this email or contact support.</p></body></html>',
 'Hello {{user.name}}, click here to reset your password: {{reset.link}}. This link expires at {{reset.expiresAt}}. If you didn''t request this, please ignore this email.',
 ARRAY['user.name', 'reset.link', 'reset.expiresAt', 'system.name']),

('welcome_email', 
 'Welcome to {{organization.name}}!', 
 '<html><body><h2>Welcome to {{organization.name}}!</h2><p>Hello {{user.name}},</p><p>We''re excited to have you on board! Here are some quick links to get you started:</p><ul><li><a href="{{getting_started.link}}">Getting Started Guide</a></li><li>Support Email: {{support.email}}</li></ul><p>If you have any questions, don''t hesitate to reach out!</p></body></html>',
 'Welcome to {{organization.name}}, {{user.name}}! Get started here: {{getting_started.link}}. Need help? Contact us at {{support.email}}.',
 ARRAY['user.name', 'organization.name', 'getting_started.link', 'support.email']),

('organization_suspended', 
 'Your organization has been suspended', 
 '<html><body><h2>Organization Suspended</h2><p>Your organization {{organization.name}} has been suspended.</p><p><strong>Reason:</strong> {{suspension.reason}}</p><p><strong>Date:</strong> {{suspension.date}}</p><p>To resolve this issue, please contact us at {{contact.email}}.</p></body></html>',
 'Your organization {{organization.name}} has been suspended. Reason: {{suspension.reason}}. Contact: {{contact.email}}.',
 ARRAY['organization.name', 'suspension.reason', 'suspension.date', 'contact.email']);

-- Insert default notification preferences for existing users
INSERT INTO notifications.notification_preferences (user_id, email_enabled, in_app_enabled)
SELECT id, true, true FROM users.users
ON CONFLICT (user_id) DO NOTHING;

-- Insert default system settings for Phase 0.3
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('email', 'smtp.host', 'smtp.gmail.com', 'string', 'SMTP server host'),
('email', 'smtp.port', '587', 'integer', 'SMTP server port'),
('email', 'smtp.username', '', 'string', 'SMTP username'),
('email', 'smtp.password', '', 'string', 'SMTP password (encrypted)'),
('email', 'smtp.from', 'noreply@easyops.com', 'string', 'Default from email address'),
('email', 'smtp.use_tls', 'true', 'boolean', 'Use TLS for SMTP'),
('monitoring', 'health_check.interval', '30', 'integer', 'Health check interval in seconds'),
('monitoring', 'metrics.retention_days', '30', 'integer', 'Metrics retention period in days'),
('monitoring', 'alerts.enabled', 'true', 'boolean', 'Enable system alerts'),
('notifications', 'max_notifications_per_user', '1000', 'integer', 'Maximum notifications per user'),
('notifications', 'retention_days', '90', 'integer', 'Notification retention period in days'),
('webhooks', 'max_webhooks_per_org', '10', 'integer', 'Maximum webhooks per organization'),
('webhooks', 'retry_max_attempts', '3', 'integer', 'Maximum webhook retry attempts');

-- ============================================================
-- TRIGGERS FOR PHASE 0.3
-- ============================================================

-- Create triggers for updated_at columns
CREATE TRIGGER update_email_templates_updated_at BEFORE UPDATE ON notifications.email_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notification_preferences_updated_at BEFORE UPDATE ON notifications.notification_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_webhooks_updated_at BEFORE UPDATE ON integration.webhooks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_settings_updated_at BEFORE UPDATE ON system.settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant all privileges on Phase 0.3 tables
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notifications TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA integration TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notifications TO easyops_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA integration TO easyops_dev;

-- ============================================================
-- PHASE 1.1: Accounting Module - CoA & GL Foundation
-- ============================================================

\i /docker-entrypoint-initdb.d/_phase_1.1_accounting_schema.sql.inc

-- ============================================================
-- PHASE 1.2: AR, AP, and Bank Management
-- ============================================================

\i /docker-entrypoint-initdb.d/_phase_1.2_ar_ap_bank_schema.sql.inc

-- ============================================================
-- PHASE 2: Sales Module
-- ============================================================

\i /docker-entrypoint-initdb.d/_phase_2_sales_schema.sql.inc

-- ============================================================
-- Apply Schema Updates
-- ============================================================

\i /docker-entrypoint-initdb.d/_update_schema.sql.inc

-- ============================================================
-- Note: COA Template Data (_coa_template_standard.sql.template) should
-- be loaded per organization, not during database initialization
-- ============================================================

-- ============================================================
-- LOAD TEST DATA FOR DEVELOPMENT
-- ============================================================

\i /docker-entrypoint-initdb.d/_test_data.sql.inc