--liquibase formatted sql

--changeset easyops:036-create-crm-schema splitStatements:false endDelimiter:GO

-- Create CRM schema
CREATE SCHEMA IF NOT EXISTS crm;

-- =====================================================
-- Lead Sources Table
-- =====================================================
CREATE TABLE crm.lead_sources (
    source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    source_name VARCHAR(100) NOT NULL,
    source_code VARCHAR(50) NOT NULL,
    source_type VARCHAR(50), -- WEB, REFERRAL, CAMPAIGN, IMPORT, API, MANUAL, EVENT, PARTNER
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lead_source_code UNIQUE (organization_id, source_code)
);

CREATE INDEX idx_lead_sources_org ON crm.lead_sources(organization_id);

-- =====================================================
-- Leads Table
-- =====================================================
CREATE TABLE crm.leads (
    lead_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    lead_number VARCHAR(50) NOT NULL,
    
    -- Basic Info
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    company VARCHAR(255),
    job_title VARCHAR(100),
    
    -- Lead Details
    lead_source_id UUID REFERENCES crm.lead_sources(source_id),
    source_campaign VARCHAR(255),
    status VARCHAR(50) DEFAULT 'NEW', -- NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED, LOST
    rating VARCHAR(20), -- HOT, WARM, COLD
    lead_score INTEGER DEFAULT 0,
    
    -- Address
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Business Info
    industry VARCHAR(100),
    company_size VARCHAR(50), -- 1-10, 11-50, 51-200, 201-500, 500+
    annual_revenue DECIMAL(15,2),
    website VARCHAR(255),
    
    -- Assignment
    owner_id UUID, -- Assigned sales rep
    assigned_at TIMESTAMP,
    
    -- Qualification
    qualified_at TIMESTAMP,
    qualified_by UUID,
    disqualified_reason TEXT,
    
    -- Conversion
    converted_at TIMESTAMP,
    converted_by UUID,
    converted_account_id UUID,
    converted_contact_id UUID,
    converted_opportunity_id UUID,
    
    -- Marketing
    utm_source VARCHAR(255),
    utm_medium VARCHAR(255),
    utm_campaign VARCHAR(255),
    utm_term VARCHAR(255),
    utm_content VARCHAR(255),
    
    -- Additional
    notes TEXT,
    tags TEXT[], -- Array of tags
    consent_email BOOLEAN DEFAULT false,
    consent_phone BOOLEAN DEFAULT false,
    do_not_contact BOOLEAN DEFAULT false,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_lead_number UNIQUE (organization_id, lead_number)
);

CREATE INDEX idx_leads_org ON crm.leads(organization_id);
CREATE INDEX idx_leads_status ON crm.leads(status);
CREATE INDEX idx_leads_owner ON crm.leads(owner_id);
CREATE INDEX idx_leads_email ON crm.leads(email);
CREATE INDEX idx_leads_company ON crm.leads(company);
CREATE INDEX idx_leads_source ON crm.leads(lead_source_id);
CREATE INDEX idx_leads_created ON crm.leads(created_at DESC);

-- =====================================================
-- Lead Activities Table
-- =====================================================
CREATE TABLE crm.lead_activities (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES crm.leads(lead_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    activity_type VARCHAR(50) NOT NULL, -- CALL, EMAIL, MEETING, TASK, NOTE
    subject VARCHAR(255),
    description TEXT,
    activity_date TIMESTAMP,
    
    -- Status
    status VARCHAR(50) DEFAULT 'PLANNED', -- PLANNED, COMPLETED, CANCELLED
    outcome VARCHAR(50), -- INTERESTED, NOT_INTERESTED, CALLBACK, NO_ANSWER, MEETING_SCHEDULED
    
    -- Task fields
    due_date TIMESTAMP,
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    
    -- Communication fields
    duration_minutes INTEGER,
    next_action VARCHAR(255),
    next_action_date TIMESTAMP,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lead_activities_lead ON crm.lead_activities(lead_id);
CREATE INDEX idx_lead_activities_org ON crm.lead_activities(organization_id);
CREATE INDEX idx_lead_activities_date ON crm.lead_activities(activity_date DESC);
CREATE INDEX idx_lead_activities_due ON crm.lead_activities(due_date);

-- =====================================================
-- Accounts Table (Customers/Companies)
-- =====================================================
CREATE TABLE crm.accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    account_number VARCHAR(50) NOT NULL,
    
    -- Basic Info
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50), -- CUSTOMER, PROSPECT, PARTNER, COMPETITOR
    industry VARCHAR(100),
    
    -- Contact Info
    phone VARCHAR(50),
    fax VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    
    -- Address
    billing_street VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(100),
    
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100),
    
    -- Business Info
    company_size VARCHAR(50),
    annual_revenue DECIMAL(15,2),
    number_of_employees INTEGER,
    
    -- Relationship
    parent_account_id UUID REFERENCES crm.accounts(account_id),
    owner_id UUID, -- Account manager
    rating VARCHAR(20), -- HOT, WARM, COLD
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    customer_since DATE,
    
    -- Tax & Legal
    tax_id VARCHAR(100),
    registration_number VARCHAR(100),
    
    -- Additional
    description TEXT,
    notes TEXT,
    tags TEXT[],
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_account_number UNIQUE (organization_id, account_number)
);

CREATE INDEX idx_accounts_org ON crm.accounts(organization_id);
CREATE INDEX idx_accounts_name ON crm.accounts(account_name);
CREATE INDEX idx_accounts_type ON crm.accounts(account_type);
CREATE INDEX idx_accounts_owner ON crm.accounts(owner_id);
CREATE INDEX idx_accounts_parent ON crm.accounts(parent_account_id);

-- =====================================================
-- Contacts Table
-- =====================================================
CREATE TABLE crm.contacts (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    account_id UUID REFERENCES crm.accounts(account_id) ON DELETE SET NULL,
    
    -- Basic Info
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    salutation VARCHAR(20), -- Mr., Ms., Dr., etc.
    job_title VARCHAR(100),
    department VARCHAR(100),
    
    -- Contact Info
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    
    -- Address (if different from account)
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Social
    linkedin_url VARCHAR(255),
    twitter_handle VARCHAR(100),
    
    -- Relationship
    reports_to_id UUID REFERENCES crm.contacts(contact_id),
    is_primary BOOLEAN DEFAULT false,
    
    -- Preferences
    preferred_contact_method VARCHAR(50), -- EMAIL, PHONE, MOBILE
    do_not_email BOOLEAN DEFAULT false,
    do_not_call BOOLEAN DEFAULT false,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    -- Additional
    birthdate DATE,
    notes TEXT,
    tags TEXT[],
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contacts_org ON crm.contacts(organization_id);
CREATE INDEX idx_contacts_account ON crm.contacts(account_id);
CREATE INDEX idx_contacts_email ON crm.contacts(email);
CREATE INDEX idx_contacts_name ON crm.contacts(last_name, first_name);
CREATE INDEX idx_contacts_primary ON crm.contacts(account_id, is_primary) WHERE is_primary = true;

-- =====================================================
-- Triggers for updated_at
-- =====================================================

CREATE OR REPLACE FUNCTION crm.update_lead_sources_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_lead_sources_updated_at
    BEFORE UPDATE ON crm.lead_sources
    FOR EACH ROW
    EXECUTE FUNCTION crm.update_lead_sources_updated_at();

CREATE OR REPLACE FUNCTION crm.update_leads_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_leads_updated_at
    BEFORE UPDATE ON crm.leads
    FOR EACH ROW
    EXECUTE FUNCTION crm.update_leads_updated_at();

CREATE OR REPLACE FUNCTION crm.update_lead_activities_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_lead_activities_updated_at
    BEFORE UPDATE ON crm.lead_activities
    FOR EACH ROW
    EXECUTE FUNCTION crm.update_lead_activities_updated_at();

CREATE OR REPLACE FUNCTION crm.update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_accounts_updated_at
    BEFORE UPDATE ON crm.accounts
    FOR EACH ROW
    EXECUTE FUNCTION crm.update_accounts_updated_at();

CREATE OR REPLACE FUNCTION crm.update_contacts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_contacts_updated_at
    BEFORE UPDATE ON crm.contacts
    FOR EACH ROW
    EXECUTE FUNCTION crm.update_contacts_updated_at();

GO

--rollback DROP SCHEMA crm CASCADE;


