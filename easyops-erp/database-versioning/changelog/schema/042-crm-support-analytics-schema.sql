--liquibase formatted sql

--changeset easyops:042-create-crm-support-analytics-schema splitStatements:false

-- =====================================================
-- SLA Policies Table
-- =====================================================
CREATE TABLE crm.sla_policies (
    sla_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    policy_name VARCHAR(255) NOT NULL,
    policy_code VARCHAR(100) NOT NULL,
    
    -- Priority-based SLAs
    priority VARCHAR(20) NOT NULL, -- HIGH, MEDIUM, LOW, CRITICAL
    
    -- Response Times (in minutes)
    first_response_time INTEGER NOT NULL, -- Minutes to first response
    resolution_time INTEGER NOT NULL, -- Minutes to resolution
    
    -- Business Hours
    business_hours_only BOOLEAN DEFAULT true,
    business_hours_start TIME DEFAULT '09:00:00',
    business_hours_end TIME DEFAULT '17:00:00',
    
    -- Active Status
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_sla_policy_code UNIQUE (organization_id, policy_code)
);

CREATE INDEX idx_sla_policies_org ON crm.sla_policies(organization_id);
CREATE INDEX idx_sla_policies_priority ON crm.sla_policies(priority);

-- =====================================================
-- Cases (Support Tickets) Table
-- =====================================================
CREATE TABLE crm.cases (
    case_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    case_number VARCHAR(50) NOT NULL,
    
    -- Basic Info
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    case_type VARCHAR(50), -- QUESTION, PROBLEM, FEATURE_REQUEST, BUG, COMPLAINT
    
    -- Status and Priority
    status VARCHAR(50) DEFAULT 'NEW', -- NEW, OPEN, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- CRITICAL, HIGH, MEDIUM, LOW
    
    -- Related Records
    contact_id UUID REFERENCES crm.contacts(contact_id),
    account_id UUID REFERENCES crm.accounts(account_id),
    
    -- Assignment
    assigned_to UUID,
    assigned_at TIMESTAMP,
    
    -- Origin
    origin VARCHAR(50), -- EMAIL, PHONE, WEB, CHAT, SOCIAL
    
    -- Resolution
    resolution TEXT,
    resolved_by UUID,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    
    -- SLA Tracking
    sla_id UUID REFERENCES crm.sla_policies(sla_id),
    first_response_at TIMESTAMP,
    first_response_due TIMESTAMP,
    resolution_due TIMESTAMP,
    sla_breached BOOLEAN DEFAULT false,
    
    -- Satisfaction
    customer_rating INTEGER, -- 1-5 stars
    customer_feedback TEXT,
    
    -- Tags and Classification
    tags TEXT[], -- Array of tags
    category VARCHAR(100),
    subcategory VARCHAR(100),
    
    -- Related Cases
    parent_case_id UUID REFERENCES crm.cases(case_id),
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_case_number UNIQUE (organization_id, case_number)
);

CREATE INDEX idx_cases_org ON crm.cases(organization_id);
CREATE INDEX idx_cases_status ON crm.cases(status);
CREATE INDEX idx_cases_priority ON crm.cases(priority);
CREATE INDEX idx_cases_assigned ON crm.cases(assigned_to);
CREATE INDEX idx_cases_contact ON crm.cases(contact_id);
CREATE INDEX idx_cases_account ON crm.cases(account_id);
CREATE INDEX idx_cases_sla ON crm.cases(sla_id);
CREATE INDEX idx_cases_created_at ON crm.cases(created_at);

-- =====================================================
-- Case Comments Table
-- =====================================================
CREATE TABLE crm.case_comments (
    comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES crm.cases(case_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Comment Details
    comment_text TEXT NOT NULL,
    comment_type VARCHAR(50), -- INTERNAL, EXTERNAL, SYSTEM
    
    -- Visibility
    is_public BOOLEAN DEFAULT false, -- Visible to customer
    
    -- Author
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Attachments (optional - could reference file storage)
    has_attachments BOOLEAN DEFAULT false,
    attachment_count INTEGER DEFAULT 0
);

CREATE INDEX idx_case_comments_case ON crm.case_comments(case_id);
CREATE INDEX idx_case_comments_created ON crm.case_comments(created_at);

-- =====================================================
-- Knowledge Base Table
-- =====================================================
CREATE TABLE crm.knowledge_base (
    article_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    article_number VARCHAR(50) NOT NULL,
    
    -- Article Content
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content TEXT NOT NULL,
    
    -- Classification
    category VARCHAR(100),
    subcategory VARCHAR(100),
    tags TEXT[], -- Array of tags
    
    -- Status
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    
    -- Visibility
    is_public BOOLEAN DEFAULT false, -- Visible to customers
    is_featured BOOLEAN DEFAULT false,
    
    -- SEO
    keywords TEXT[],
    slug VARCHAR(255),
    
    -- Usage Stats
    view_count INTEGER DEFAULT 0,
    helpful_count INTEGER DEFAULT 0,
    not_helpful_count INTEGER DEFAULT 0,
    
    -- Version Control
    version INTEGER DEFAULT 1,
    last_reviewed_at TIMESTAMP,
    last_reviewed_by UUID,
    
    -- Related Articles
    related_articles UUID[],
    
    -- Authoring
    author_id UUID,
    published_at TIMESTAMP,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_article_number UNIQUE (organization_id, article_number),
    CONSTRAINT uk_article_slug UNIQUE (organization_id, slug)
);

CREATE INDEX idx_kb_org ON crm.knowledge_base(organization_id);
CREATE INDEX idx_kb_status ON crm.knowledge_base(status);
CREATE INDEX idx_kb_category ON crm.knowledge_base(category);
CREATE INDEX idx_kb_public ON crm.knowledge_base(is_public);
CREATE INDEX idx_kb_featured ON crm.knowledge_base(is_featured);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_sla_policies_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_sla_policies_timestamp
BEFORE UPDATE ON crm.sla_policies
FOR EACH ROW
EXECUTE FUNCTION update_sla_policies_timestamp();

CREATE OR REPLACE FUNCTION update_cases_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_cases_timestamp
BEFORE UPDATE ON crm.cases
FOR EACH ROW
EXECUTE FUNCTION update_cases_timestamp();

CREATE OR REPLACE FUNCTION update_knowledge_base_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_knowledge_base_timestamp
BEFORE UPDATE ON crm.knowledge_base
FOR EACH ROW
EXECUTE FUNCTION update_knowledge_base_timestamp();

-- =====================================================
-- Trigger: Auto-calculate SLA due times
-- =====================================================
CREATE OR REPLACE FUNCTION calculate_sla_due_times()
RETURNS TRIGGER AS $$
DECLARE
    sla_record RECORD;
BEGIN
    IF NEW.sla_id IS NOT NULL THEN
        SELECT * INTO sla_record FROM crm.sla_policies WHERE sla_id = NEW.sla_id;
        
        IF FOUND THEN
            -- Calculate first response due time
            NEW.first_response_due = NEW.created_at + (sla_record.first_response_time || ' minutes')::INTERVAL;
            
            -- Calculate resolution due time
            NEW.resolution_due = NEW.created_at + (sla_record.resolution_time || ' minutes')::INTERVAL;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calculate_sla_due_times
BEFORE INSERT OR UPDATE ON crm.cases
FOR EACH ROW
EXECUTE FUNCTION calculate_sla_due_times();

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE crm.sla_policies IS 'Service Level Agreement policies';
COMMENT ON TABLE crm.cases IS 'Customer support cases/tickets';
COMMENT ON TABLE crm.case_comments IS 'Comments and interactions on cases';
COMMENT ON TABLE crm.knowledge_base IS 'Knowledge base articles for self-service';


