--liquibase formatted sql

--changeset easyops:004-add-prescription-formulary-fields
--comment: Add missing formulary and cost-related fields to prescriptions table

-- Add formulary and insurance-related fields
ALTER TABLE ehr.prescriptions 
    ADD COLUMN IF NOT EXISTS formulary_checked BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS formulary_check_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS coverage_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS formulary_tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS requires_prior_authorization BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS prior_authorization_obtained BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS prior_authorization_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS patient_cost_estimate DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS copay_amount DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS insurance_id UUID,
    ADD COLUMN IF NOT EXISTS pbm_name VARCHAR(200);

-- Add check constraint for coverage_status
ALTER TABLE ehr.prescriptions
    DROP CONSTRAINT IF EXISTS chk_coverage_status;
    
ALTER TABLE ehr.prescriptions
    ADD CONSTRAINT chk_coverage_status CHECK (
        coverage_status IS NULL OR coverage_status IN 
        ('COVERED', 'NOT_COVERED', 'COVERED_WITH_RESTRICTIONS', 'NOT_CHECKED', 'ERROR')
    );

-- Add index for insurance_id
CREATE INDEX IF NOT EXISTS idx_prescriptions_insurance ON ehr.prescriptions(insurance_id) WHERE insurance_id IS NOT NULL;

COMMENT ON COLUMN ehr.prescriptions.formulary_checked IS 'Whether formulary check has been performed';
COMMENT ON COLUMN ehr.prescriptions.coverage_status IS 'Insurance coverage status: COVERED, NOT_COVERED, COVERED_WITH_RESTRICTIONS, NOT_CHECKED, ERROR';
COMMENT ON COLUMN ehr.prescriptions.formulary_tier IS 'Formulary tier (e.g., Tier 1, Tier 2, Tier 3)';
COMMENT ON COLUMN ehr.prescriptions.patient_cost_estimate IS 'Estimated patient cost for the prescription';
COMMENT ON COLUMN ehr.prescriptions.copay_amount IS 'Patient copay amount';
COMMENT ON COLUMN ehr.prescriptions.insurance_id IS 'Insurance plan used for formulary check';
COMMENT ON COLUMN ehr.prescriptions.pbm_name IS 'Pharmacy Benefit Manager name';
