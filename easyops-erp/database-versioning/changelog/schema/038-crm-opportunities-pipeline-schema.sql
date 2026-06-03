--liquibase formatted sql

--changeset easyops:038-create-crm-opportunities-pipeline-schema splitStatements:false endDelimiter:GO

-- =====================================================
-- Opportunity Stages Table
-- =====================================================
CREATE TABLE crm.opportunity_stages (
    stage_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    stage_name VARCHAR(100) NOT NULL,
    stage_code VARCHAR(50) NOT NULL,
    stage_order INTEGER NOT NULL,
    probability DECIMAL(5,2) DEFAULT 0, -- Win probability (0-100)
    is_closed_won BOOLEAN DEFAULT false,
    is_closed_lost BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_opp_stage_code UNIQUE (organization_id, stage_code),
    CONSTRAINT uk_opp_stage_order UNIQUE (organization_id, stage_order)
);

CREATE INDEX idx_opp_stages_org ON crm.opportunity_stages(organization_id);
CREATE INDEX idx_opp_stages_order ON crm.opportunity_stages(stage_order);

-- =====================================================
-- Opportunities Table
-- =====================================================
CREATE TABLE crm.opportunities (
    opportunity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    opportunity_number VARCHAR(50) NOT NULL,
    
    -- Basic Info
    opportunity_name VARCHAR(255) NOT NULL,
    account_id UUID REFERENCES crm.accounts(account_id),
    contact_id UUID REFERENCES crm.contacts(contact_id),
    lead_id UUID REFERENCES crm.leads(lead_id), -- Source lead if converted
    
    -- Sales Details
    stage_id UUID REFERENCES crm.opportunity_stages(stage_id),
    type VARCHAR(50), -- NEW_BUSINESS, EXISTING_BUSINESS, RENEWAL, UPSELL, CROSS_SELL
    amount DECIMAL(15,2),
    currency VARCHAR(10) DEFAULT 'USD',
    probability DECIMAL(5,2), -- Override stage probability
    expected_revenue DECIMAL(15,2) GENERATED ALWAYS AS (amount * probability / 100) STORED,
    
    -- Timeline
    close_date DATE,
    expected_close_date DATE,
    created_date DATE DEFAULT CURRENT_DATE,
    
    -- Assignment
    owner_id UUID,
    
    -- Status
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, WON, LOST, ABANDONED
    
    -- Lead Source
    lead_source_id UUID REFERENCES crm.lead_sources(source_id),
    source_campaign VARCHAR(255),
    
    -- Additional Info
    description TEXT,
    next_step TEXT,
    competitors TEXT,
    
    -- Win/Loss Analysis
    loss_reason VARCHAR(255),
    loss_description TEXT,
    win_description TEXT,
    
    -- Sales Cycle Tracking
    days_in_stage INTEGER DEFAULT 0,
    total_days_open INTEGER,
    
    -- Tags and Classification
    tags TEXT[], -- Array of tags
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_opportunity_number UNIQUE (organization_id, opportunity_number)
);

CREATE INDEX idx_opportunities_org ON crm.opportunities(organization_id);
CREATE INDEX idx_opportunities_account ON crm.opportunities(account_id);
CREATE INDEX idx_opportunities_contact ON crm.opportunities(contact_id);
CREATE INDEX idx_opportunities_stage ON crm.opportunities(stage_id);
CREATE INDEX idx_opportunities_owner ON crm.opportunities(owner_id);
CREATE INDEX idx_opportunities_status ON crm.opportunities(status);
CREATE INDEX idx_opportunities_close_date ON crm.opportunities(close_date);
CREATE INDEX idx_opportunities_lead_source ON crm.opportunities(lead_source_id);

-- =====================================================
-- Opportunity Products Table
-- =====================================================
CREATE TABLE crm.opportunity_products (
    opp_product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES crm.opportunities(opportunity_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Product Details
    product_code VARCHAR(100),
    product_name VARCHAR(255) NOT NULL,
    product_description TEXT,
    
    -- Pricing
    quantity DECIMAL(15,3) DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    tax_percent DECIMAL(5,2) DEFAULT 0,
    
    -- Calculations
    subtotal DECIMAL(15,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    discount_total DECIMAL(15,2) GENERATED ALWAYS AS (
        CASE 
            WHEN discount_amount > 0 THEN discount_amount
            ELSE (quantity * unit_price * discount_percent / 100)
        END
    ) STORED,
    tax_amount DECIMAL(15,2) GENERATED ALWAYS AS (
        ((quantity * unit_price) - 
        CASE 
            WHEN discount_amount > 0 THEN discount_amount
            ELSE (quantity * unit_price * discount_percent / 100)
        END) * tax_percent / 100
    ) STORED,
    line_total DECIMAL(15,2) GENERATED ALWAYS AS (
        (quantity * unit_price) - 
        CASE 
            WHEN discount_amount > 0 THEN discount_amount
            ELSE (quantity * unit_price * discount_percent / 100)
        END +
        ((quantity * unit_price) - 
        CASE 
            WHEN discount_amount > 0 THEN discount_amount
            ELSE (quantity * unit_price * discount_percent / 100)
        END) * tax_percent / 100
    ) STORED,
    
    -- Additional Info
    notes TEXT,
    line_order INTEGER DEFAULT 1,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_opp_products_opportunity ON crm.opportunity_products(opportunity_id);
CREATE INDEX idx_opp_products_org ON crm.opportunity_products(organization_id);
CREATE INDEX idx_opp_products_code ON crm.opportunity_products(product_code);

-- =====================================================
-- Opportunity Activities Table
-- =====================================================
CREATE TABLE crm.opportunity_activities (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES crm.opportunities(opportunity_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    
    -- Activity Details
    activity_type VARCHAR(50) NOT NULL, -- CALL, EMAIL, MEETING, TASK, NOTE, DEMO, PROPOSAL, NEGOTIATION
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Timing
    activity_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_minutes INTEGER,
    
    -- Status for Tasks
    status VARCHAR(50), -- PLANNED, COMPLETED, CANCELLED
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    
    -- Assignment
    assigned_to UUID,
    completed_by UUID,
    
    -- Outcome
    outcome VARCHAR(50), -- SUCCESSFUL, UNSUCCESSFUL, NO_RESPONSE, FOLLOW_UP_REQUIRED
    next_action TEXT,
    
    -- Metadata
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_opp_activities_opportunity ON crm.opportunity_activities(opportunity_id);
CREATE INDEX idx_opp_activities_org ON crm.opportunity_activities(organization_id);
CREATE INDEX idx_opp_activities_type ON crm.opportunity_activities(activity_type);
CREATE INDEX idx_opp_activities_date ON crm.opportunity_activities(activity_date);
CREATE INDEX idx_opp_activities_assigned ON crm.opportunity_activities(assigned_to);

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_opportunity_stages_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_opportunity_stages_timestamp
BEFORE UPDATE ON crm.opportunity_stages
FOR EACH ROW
EXECUTE FUNCTION update_opportunity_stages_timestamp();

CREATE OR REPLACE FUNCTION update_opportunities_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_opportunities_timestamp
BEFORE UPDATE ON crm.opportunities
FOR EACH ROW
EXECUTE FUNCTION update_opportunities_timestamp();

CREATE OR REPLACE FUNCTION update_opportunity_products_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_opportunity_products_timestamp
BEFORE UPDATE ON crm.opportunity_products
FOR EACH ROW
EXECUTE FUNCTION update_opportunity_products_timestamp();

CREATE OR REPLACE FUNCTION update_opportunity_activities_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_opportunity_activities_timestamp
BEFORE UPDATE ON crm.opportunity_activities
FOR EACH ROW
EXECUTE FUNCTION update_opportunity_activities_timestamp();

-- =====================================================
-- Insert Default Opportunity Stages
-- =====================================================
-- Note: These will be inserted per organization
-- This is just a template showing standard sales stages

COMMENT ON TABLE crm.opportunity_stages IS 'Sales pipeline stages configuration';
COMMENT ON TABLE crm.opportunities IS 'Sales opportunities/deals tracking';
COMMENT ON TABLE crm.opportunity_products IS 'Products/services in opportunities';
COMMENT ON TABLE crm.opportunity_activities IS 'Activities related to opportunities';

GO

