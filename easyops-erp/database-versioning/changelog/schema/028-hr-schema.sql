--liquibase formatted sql

--changeset easyops:028-create-hr-schema splitStatements:false endDelimiter:GO
--comment: Create HR module database schema

-- Create HR schema
CREATE SCHEMA IF NOT EXISTS hr;

-- Set search path
SET search_path TO hr, admin, public;

-- Departments live in admin.departments (organization-service). HR references them by id only.

-- =============================================
-- 1. POSITIONS TABLE
-- =============================================
CREATE TABLE hr.positions (
    position_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    department_id UUID,
    level VARCHAR(50),
    salary_range_min DECIMAL(15,2),
    salary_range_max DECIMAL(15,2),
    currency VARCHAR(3) DEFAULT 'USD',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    CONSTRAINT fk_position_organization FOREIGN KEY (organization_id) 
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_position_department FOREIGN KEY (department_id) 
        REFERENCES admin.departments(id) ON DELETE SET NULL,
    CONSTRAINT uk_position_title_org UNIQUE (organization_id, title)
);

CREATE INDEX idx_positions_org ON hr.positions(organization_id);
CREATE INDEX idx_positions_department ON hr.positions(department_id);

COMMENT ON TABLE hr.positions IS 'Job positions and titles within the organization';

-- =============================================
-- 2. EMPLOYEES TABLE
-- =============================================
CREATE TABLE hr.employees (
    employee_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID,
    employee_number VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    date_of_birth DATE,
    gender VARCHAR(20),
    hire_date DATE NOT NULL,
    termination_date DATE,
    department_id UUID,
    position_id UUID,
    manager_id UUID,
    employment_type VARCHAR(50) DEFAULT 'FULL_TIME',
    employment_status VARCHAR(50) DEFAULT 'ACTIVE',
    
    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Emergency contact
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relationship VARCHAR(100),
    
    -- System fields
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    CONSTRAINT fk_employee_organization FOREIGN KEY (organization_id) 
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) 
        REFERENCES admin.departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_employee_position FOREIGN KEY (position_id) 
        REFERENCES hr.positions(position_id) ON DELETE SET NULL,
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) 
        REFERENCES hr.employees(employee_id) ON DELETE SET NULL,
    CONSTRAINT uk_employee_number_org UNIQUE (organization_id, employee_number),
    CONSTRAINT uk_employee_email_org UNIQUE (organization_id, email)
);

CREATE INDEX idx_employees_org ON hr.employees(organization_id);
CREATE INDEX idx_employees_user ON hr.employees(user_id);
CREATE INDEX idx_employees_department ON hr.employees(department_id);
CREATE INDEX idx_employees_position ON hr.employees(position_id);
CREATE INDEX idx_employees_manager ON hr.employees(manager_id);
CREATE INDEX idx_employees_status ON hr.employees(employment_status);
CREATE INDEX idx_employees_hire_date ON hr.employees(hire_date);
CREATE INDEX idx_employees_email ON hr.employees(email);

COMMENT ON TABLE hr.employees IS 'Employee master data and personal information';

-- =============================================
-- 4. EMPLOYEE DOCUMENTS TABLE
-- =============================================
CREATE TABLE hr.employee_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    upload_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    notes TEXT,
    uploaded_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_document_employee FOREIGN KEY (employee_id) 
        REFERENCES hr.employees(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_document_organization FOREIGN KEY (organization_id) 
        REFERENCES admin.organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_employee_documents_employee ON hr.employee_documents(employee_id);
CREATE INDEX idx_employee_documents_org ON hr.employee_documents(organization_id);
CREATE INDEX idx_employee_documents_type ON hr.employee_documents(document_type);
CREATE INDEX idx_employee_documents_status ON hr.employee_documents(status);

COMMENT ON TABLE hr.employee_documents IS 'Employee document management and tracking';

-- =============================================
-- 5. ONBOARDING CHECKLISTS TABLE
-- =============================================
CREATE TABLE hr.onboarding_checklists (
    checklist_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    task_description TEXT,
    task_category VARCHAR(100),
    due_date DATE,
    completed_date DATE,
    status VARCHAR(50) DEFAULT 'PENDING',
    assigned_to UUID,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_checklist_employee FOREIGN KEY (employee_id) 
        REFERENCES hr.employees(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_organization FOREIGN KEY (organization_id) 
        REFERENCES admin.organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_onboarding_employee ON hr.onboarding_checklists(employee_id);
CREATE INDEX idx_onboarding_org ON hr.onboarding_checklists(organization_id);
CREATE INDEX idx_onboarding_status ON hr.onboarding_checklists(status);

COMMENT ON TABLE hr.onboarding_checklists IS 'Onboarding tasks and checklist items for new employees';

-- =============================================
-- TRIGGERS FOR updated_at
-- =============================================

-- Positions trigger
CREATE OR REPLACE FUNCTION hr.update_positions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_positions_updated_at
    BEFORE UPDATE ON hr.positions
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_positions_updated_at();

-- Employees trigger
CREATE OR REPLACE FUNCTION hr.update_employees_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_employees_updated_at
    BEFORE UPDATE ON hr.employees
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_employees_updated_at();

-- Employee documents trigger
CREATE OR REPLACE FUNCTION hr.update_employee_documents_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_employee_documents_updated_at
    BEFORE UPDATE ON hr.employee_documents
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_employee_documents_updated_at();

-- Onboarding checklists trigger
CREATE OR REPLACE FUNCTION hr.update_onboarding_checklists_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_onboarding_checklists_updated_at
    BEFORE UPDATE ON hr.onboarding_checklists
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_onboarding_checklists_updated_at();
GO

--changeset easyops:028b-ensure-hr-onboarding-updated-trigger-function splitStatements:false endDelimiter:GO
--comment: Ensure hr.update_onboarding_checklists_updated_at exists for environments where HR schema was created without this function
CREATE SCHEMA IF NOT EXISTS hr;

CREATE OR REPLACE FUNCTION hr.update_onboarding_checklists_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
GO

--rollback DROP SCHEMA IF EXISTS hr CASCADE;

