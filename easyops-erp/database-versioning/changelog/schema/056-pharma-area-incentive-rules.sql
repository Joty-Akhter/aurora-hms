--liquibase formatted sql

--changeset easyops:074-create-pharma-area-incentive-rules context:pharma splitStatements:false
--comment: Create area-specific incentive rules table for Phase 5.2
CREATE TABLE IF NOT EXISTS pharma.area_incentive_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    area_id UUID NOT NULL REFERENCES pharma.areas(id) ON DELETE CASCADE,
    
    -- Rule Configuration
    incentive_percentage DECIMAL(5, 4) DEFAULT 0.0400, -- Default 4% (0.04), can be customized per area
    sr_share_percentage DECIMAL(5, 4) DEFAULT 0.1000, -- 10% of total incentive
    mpo_share_percentage DECIMAL(5, 4) DEFAULT 0.7200, -- 80% of remaining 90% = 72% of total (0.80 * 0.90)
    manager_share_percentage DECIMAL(5, 4) DEFAULT 0.1800, -- 20% of remaining 90% = 18% of total (0.20 * 0.90)
    expense_limit_percentage DECIMAL(5, 4) DEFAULT 0.3000, -- 30% of target
    
    -- Rule Metadata
    rule_version INTEGER DEFAULT 1, -- For rule versioning
    effective_from_date DATE, -- Rule effective from date
    effective_to_date DATE, -- Rule effective to date (NULL = active indefinitely)
    is_active BOOLEAN DEFAULT true,
    
    -- Additional Configuration
    description TEXT, -- Description of the rule and why it's customized
    notes TEXT, -- Additional notes
    
    -- Audit Trail
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);
-- Note: Unique constraint handled via partial unique index below

-- Partial unique index: only one active rule per area
CREATE UNIQUE INDEX IF NOT EXISTS idx_area_incentive_rules_unique_active
ON pharma.area_incentive_rules(area_id)
WHERE is_active = true;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_area_incentive_rules_org ON pharma.area_incentive_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_area_incentive_rules_area ON pharma.area_incentive_rules(area_id);
CREATE INDEX IF NOT EXISTS idx_area_incentive_rules_active ON pharma.area_incentive_rules(is_active, effective_from_date, effective_to_date);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION pharma.update_area_incentive_rules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_area_incentive_rules_updated_at ON pharma.area_incentive_rules;
CREATE TRIGGER trg_area_incentive_rules_updated_at
    BEFORE UPDATE ON pharma.area_incentive_rules
    FOR EACH ROW
    EXECUTE FUNCTION pharma.update_area_incentive_rules_updated_at();

-- Comment
COMMENT ON TABLE pharma.area_incentive_rules IS 'Area-specific incentive rules for Phase 5.2 - allows customization of incentive percentages and distribution rules per area';
