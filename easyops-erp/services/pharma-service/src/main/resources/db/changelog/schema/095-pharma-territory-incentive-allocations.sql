--liquibase formatted sql

--changeset easyops:095-create-territory-incentive-allocations context:pharma
--comment: Per-employee allocation percentages for Manager/MPO share of remaining pool (must sum to 100%)
CREATE TABLE IF NOT EXISTS pharma.territory_incentive_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    territory_incentive_rule_id UUID NOT NULL REFERENCES pharma.territory_incentive_rules(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL,
    role_in_territory VARCHAR(50),
    allocation_percentage DECIMAL(5, 2) NOT NULL CHECK (allocation_percentage >= 0 AND allocation_percentage <= 100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    UNIQUE(territory_incentive_rule_id, employee_id)
);

CREATE INDEX IF NOT EXISTS idx_territory_incentive_allocations_rule ON pharma.territory_incentive_allocations(territory_incentive_rule_id);
CREATE INDEX IF NOT EXISTS idx_territory_incentive_allocations_employee ON pharma.territory_incentive_allocations(employee_id);

COMMENT ON TABLE pharma.territory_incentive_allocations IS 'Per-employee % of remaining pool (after SR + Dev Fund); sum must equal 100% per rule';
