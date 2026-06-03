--liquibase formatted sql

--changeset easyops:021-create-roles-table context:rbac
--comment: Create roles table
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

CREATE INDEX idx_roles_code ON rbac.roles(code);

--changeset easyops:022-create-permissions-table context:rbac
--comment: Create permissions table
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

CREATE INDEX idx_permissions_code ON rbac.permissions(code);
CREATE INDEX idx_permissions_resource ON rbac.permissions(resource);

--changeset easyops:023-create-role-permissions-table context:rbac
--comment: Create role permissions table (many-to-many)
CREATE TABLE rbac.role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES rbac.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES rbac.permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

--changeset easyops:024-create-user-roles-table context:rbac
--comment: Create user roles table (many-to-many)
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

CREATE INDEX idx_user_roles_user_id ON rbac.user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON rbac.user_roles(role_id);

--changeset easyops:025-create-rbac-triggers context:rbac
--comment: Create updated_at triggers for RBAC tables
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON rbac.roles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_permissions_updated_at BEFORE UPDATE ON rbac.permissions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:026-grant-rbac-permissions context:rbac
--comment: Grant permissions on RBAC tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA rbac TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA rbac TO easyops;
