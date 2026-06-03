--liquibase formatted sql

--changeset easyops:109-hr-phase-c-shift-roster-ot-master-data splitStatements:false

-- =====================================================
-- HMS Phase C (HR-AT-01/02/05, HR-MD-03/04): shift master,
-- roster FK to shift definitions, employee OT overrides,
-- designation hierarchy rank, employee bank fields.
-- =====================================================

CREATE TABLE hr.shift_definitions (
    shift_definition_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    shift_type VARCHAR(30) NOT NULL DEFAULT 'DAY',
    grace_minutes INTEGER NOT NULL DEFAULT 0,
    expected_hours DECIMAL(5, 2) NOT NULL DEFAULT 8.00,
    overtime_rate_multiplier DECIMAL(5, 2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hr_shift_definitions_org_code UNIQUE (organization_id, code)
);

CREATE INDEX idx_shift_definitions_org ON hr.shift_definitions(organization_id);
CREATE INDEX idx_shift_definitions_org_active ON hr.shift_definitions(organization_id, is_active);

ALTER TABLE hr.shift_schedules
    ADD COLUMN shift_definition_id UUID REFERENCES hr.shift_definitions(shift_definition_id);

CREATE INDEX idx_shift_schedules_definition ON hr.shift_schedules(shift_definition_id);

ALTER TABLE hr.positions
    ADD COLUMN hierarchy_rank INTEGER;

COMMENT ON COLUMN hr.positions.hierarchy_rank IS 'HR-MD-03: numeric ordering for designation hierarchy / approval sorting.';

ALTER TABLE hr.employees
    ADD COLUMN bank_name VARCHAR(200),
    ADD COLUMN bank_branch VARCHAR(200),
    ADD COLUMN bank_account_number VARCHAR(100),
    ADD COLUMN bank_routing_or_iban VARCHAR(64),
    ADD COLUMN payroll_overtime_rate_multiplier DECIMAL(5, 2),
    ADD COLUMN payroll_standard_hours_per_day DECIMAL(5, 2);

COMMENT ON COLUMN hr.employees.bank_account_number IS 'HR-MD-04: sensitive — protect at DB/storage layer per org policy.';
COMMENT ON COLUMN hr.employees.payroll_overtime_rate_multiplier IS 'Phase C HR-AT-05: optional employee-level OT multiplier override.';
COMMENT ON COLUMN hr.employees.payroll_standard_hours_per_day IS 'Phase C HR-AT-05: optional employee-level standard hours for OT rate derivation.';

CREATE OR REPLACE FUNCTION hr.update_shift_definitions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_shift_definitions_updated_at
    BEFORE UPDATE ON hr.shift_definitions
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_shift_definitions_updated_at();
