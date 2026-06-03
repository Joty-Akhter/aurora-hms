--liquibase formatted sql

--changeset easyops:040-create-crm-activities-campaigns-schema splitStatements:false

-- =====================================================
-- Email Templates Table
-- =====================================================
CREATE TABLE crm.email_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    template_name VARCHAR(255) NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_html TEXT,
    body_text TEXT,
    template_type VARCHAR(50), -- CAMPAIGN, FOLLOW_UP, THANK_YOU, REMINDER, CUSTOM
    is_active BOOLEAN DEFAULT true,
    category VARCHAR(100),
    placeholders TEXT[], -- Array of available placeholders
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_template_code UNIQUE (organization_id, template_code)
);

CREATE INDEX idx_email_templates_org ON crm.email_templates(organization_id);
CREATE INDEX idx_email_templates_type ON crm.email_templates(template_type);

-- =====================================================
-- Campaigns Table
-- =====================================================
CREATE TABLE crm.campaigns (
    campaign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    campaign_number VARCHAR(50) NOT NULL,
    
    -- Basic Info
    campaign_name VARCHAR(255) NOT NULL,
    campaign_type VARCHAR(50), -- EMAIL, SOCIAL, EVENT, WEBINAR, DIRECT_MAIL, TELEMARKETING
    status VARCHAR(50) DEFAULT 'PLANNING', -- PLANNING, ACTIVE, COMPLETED, CANCELLED
    
    -- Dates
    start_date DATE,
    end_date DATE,
    
    -- Budget and Cost
    budgeted_cost DECIMAL(15,2),
    actual_cost DECIMAL(15,2),
    currency VARCHAR(10) DEFAULT 'USD',
    
    -- Expected Results
    expected_response_count INTEGER,
    expected_revenue DECIMAL(15,2),
    
    -- Owner and Assignment
    owner_id UUID,
    
    -- Description
    description TEXT,
    objectives TEXT,
    target_audience TEXT,
    
    -- UTM Parameters for Tracking
    utm_source VARCHAR(255),
    utm_medium VARCHAR(255),
    utm_campaign VARCHAR(255),
    utm_term VARCHAR(255),
    utm_content VARCHAR(255),
    
    -- Email Campaign Specific
    email_template_id UUID REFERENCES crm.email_templates(template_id),
    
    -- Tags and Classification
    tags TEXT[], -- Array of tags
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_campaign_number UNIQUE (organization_id, campaign_number)
);

CREATE INDEX idx_campaigns_org ON crm.campaigns(organization_id);
CREATE INDEX idx_campaigns_status ON crm.campaigns(status);
CREATE INDEX idx_campaigns_type ON crm.campaigns(campaign_type);
CREATE INDEX idx_campaigns_owner ON crm.campaigns(owner_id);
CREATE INDEX idx_campaigns_dates ON crm.campaigns(start_date, end_date);

-- =====================================================
-- Campaign Members Table
-- =====================================================
CREATE TABLE crm.campaign_members (
    member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES crm.campaigns(campaign_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Member Reference (can be lead, contact, or account)
    lead_id UUID REFERENCES crm.leads(lead_id),
    contact_id UUID REFERENCES crm.contacts(contact_id),
    account_id UUID REFERENCES crm.accounts(account_id),
    
    -- Member Status
    member_status VARCHAR(50) DEFAULT 'SENT', -- SENT, RESPONDED, CONVERTED, UNSUBSCRIBED, BOUNCED
    
    -- Response Tracking
    responded BOOLEAN DEFAULT false,
    response_date TIMESTAMP,
    converted BOOLEAN DEFAULT false,
    conversion_date TIMESTAMP,
    
    -- Email Specific
    email_sent BOOLEAN DEFAULT false,
    email_opened BOOLEAN DEFAULT false,
    email_clicked BOOLEAN DEFAULT false,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_campaign_members_campaign ON crm.campaign_members(campaign_id);
CREATE INDEX idx_campaign_members_lead ON crm.campaign_members(lead_id);
CREATE INDEX idx_campaign_members_contact ON crm.campaign_members(contact_id);
CREATE INDEX idx_campaign_members_account ON crm.campaign_members(account_id);
CREATE INDEX idx_campaign_members_status ON crm.campaign_members(member_status);

-- =====================================================
-- Tasks Table
-- =====================================================
CREATE TABLE crm.tasks (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Task Details
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    task_type VARCHAR(50), -- CALL, EMAIL, MEETING, TODO, FOLLOW_UP, DEMO
    
    -- Status and Priority
    status VARCHAR(50) DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, COMPLETED, WAITING, DEFERRED, CANCELLED
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- HIGH, MEDIUM, LOW
    
    -- Dates
    due_date DATE,
    reminder_date TIMESTAMP,
    completed_date DATE,
    
    -- Assignment
    assigned_to UUID,
    assigned_by UUID,
    
    -- Related Records
    lead_id UUID REFERENCES crm.leads(lead_id),
    contact_id UUID REFERENCES crm.contacts(contact_id),
    account_id UUID REFERENCES crm.accounts(account_id),
    opportunity_id UUID REFERENCES crm.opportunities(opportunity_id),
    campaign_id UUID REFERENCES crm.campaigns(campaign_id),
    
    -- Completion Info
    completed_by UUID,
    completion_notes TEXT,
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern VARCHAR(50), -- DAILY, WEEKLY, MONTHLY, YEARLY
    recurrence_end_date DATE,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_org ON crm.tasks(organization_id);
CREATE INDEX idx_tasks_assigned ON crm.tasks(assigned_to);
CREATE INDEX idx_tasks_status ON crm.tasks(status);
CREATE INDEX idx_tasks_due_date ON crm.tasks(due_date);
CREATE INDEX idx_tasks_lead ON crm.tasks(lead_id);
CREATE INDEX idx_tasks_contact ON crm.tasks(contact_id);
CREATE INDEX idx_tasks_account ON crm.tasks(account_id);
CREATE INDEX idx_tasks_opportunity ON crm.tasks(opportunity_id);
CREATE INDEX idx_tasks_campaign ON crm.tasks(campaign_id);

-- =====================================================
-- Events Table (Meetings, Calls, etc.)
-- =====================================================
CREATE TABLE crm.events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Event Details
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(50), -- MEETING, CALL, WEBINAR, CONFERENCE, TRAINING, DEMO
    
    -- Date and Time
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,
    all_day BOOLEAN DEFAULT false,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Location
    location VARCHAR(255),
    meeting_url VARCHAR(500), -- For virtual meetings
    
    -- Status
    status VARCHAR(50) DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    
    -- Organizer and Attendees
    organizer_id UUID,
    attendees TEXT[], -- Array of user/contact IDs
    
    -- Related Records
    lead_id UUID REFERENCES crm.leads(lead_id),
    contact_id UUID REFERENCES crm.contacts(contact_id),
    account_id UUID REFERENCES crm.accounts(account_id),
    opportunity_id UUID REFERENCES crm.opportunities(opportunity_id),
    campaign_id UUID REFERENCES crm.campaigns(campaign_id),
    
    -- Outcome
    outcome VARCHAR(50), -- SUCCESSFUL, UNSUCCESSFUL, RESCHEDULED
    outcome_notes TEXT,
    
    -- Reminders
    reminder_minutes INTEGER DEFAULT 15,
    reminder_sent BOOLEAN DEFAULT false,
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern VARCHAR(50), -- DAILY, WEEKLY, MONTHLY, YEARLY
    recurrence_end_date DATE,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_org ON crm.events(organization_id);
CREATE INDEX idx_events_organizer ON crm.events(organizer_id);
CREATE INDEX idx_events_start ON crm.events(start_datetime);
CREATE INDEX idx_events_status ON crm.events(status);
CREATE INDEX idx_events_lead ON crm.events(lead_id);
CREATE INDEX idx_events_contact ON crm.events(contact_id);
CREATE INDEX idx_events_account ON crm.events(account_id);
CREATE INDEX idx_events_opportunity ON crm.events(opportunity_id);
CREATE INDEX idx_events_campaign ON crm.events(campaign_id);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_email_templates_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_email_templates_timestamp
BEFORE UPDATE ON crm.email_templates
FOR EACH ROW
EXECUTE FUNCTION update_email_templates_timestamp();

CREATE OR REPLACE FUNCTION update_campaigns_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_campaigns_timestamp
BEFORE UPDATE ON crm.campaigns
FOR EACH ROW
EXECUTE FUNCTION update_campaigns_timestamp();

CREATE OR REPLACE FUNCTION update_campaign_members_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_campaign_members_timestamp
BEFORE UPDATE ON crm.campaign_members
FOR EACH ROW
EXECUTE FUNCTION update_campaign_members_timestamp();

CREATE OR REPLACE FUNCTION update_tasks_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_tasks_timestamp
BEFORE UPDATE ON crm.tasks
FOR EACH ROW
EXECUTE FUNCTION update_tasks_timestamp();

CREATE OR REPLACE FUNCTION update_events_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_events_timestamp
BEFORE UPDATE ON crm.events
FOR EACH ROW
EXECUTE FUNCTION update_events_timestamp();

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE crm.email_templates IS 'Email templates for campaigns and communications';
COMMENT ON TABLE crm.campaigns IS 'Marketing campaigns and promotional activities';
COMMENT ON TABLE crm.campaign_members IS 'Campaign participants and their responses';
COMMENT ON TABLE crm.tasks IS 'Tasks, to-dos, and follow-ups';
COMMENT ON TABLE crm.events IS 'Events, meetings, calls, and calendar items';


