-- Add columns to ehr.prescription_interactions that are mapped by the JPA entity
-- but were absent from the original CREATE TABLE in hospital.sql.

ALTER TABLE ehr.prescription_interactions
    ADD COLUMN IF NOT EXISTS clinical_significance_level VARCHAR(50),
    ADD COLUMN IF NOT EXISTS interaction_category         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS interacting_substance        VARCHAR(500),
    ADD COLUMN IF NOT EXISTS interacting_substance_type   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS mechanism                    TEXT,
    ADD COLUMN IF NOT EXISTS onset_time                   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS evidence_level               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS documentation_references     TEXT;

ALTER TABLE ehr.prescription_interactions
    DROP CONSTRAINT IF EXISTS chk_interaction_clinical_significance_level,
    DROP CONSTRAINT IF EXISTS chk_interaction_category;

ALTER TABLE ehr.prescription_interactions
    ADD CONSTRAINT chk_interaction_clinical_significance_level
        CHECK (clinical_significance_level IN ('CRITICAL','SIGNIFICANT','MODERATE','MINOR','UNKNOWN')),
    ADD CONSTRAINT chk_interaction_category
        CHECK (interaction_category IN ('DRUG_DRUG','DRUG_FOOD','DRUG_LAB','DRUG_HERBAL','DRUG_ALCOHOL','DRUG_DISEASE','OTHER'));

COMMENT ON COLUMN ehr.prescription_interactions.clinical_significance_level IS 'Clinical significance level: CRITICAL, SIGNIFICANT, MODERATE, MINOR, UNKNOWN';
COMMENT ON COLUMN ehr.prescription_interactions.interaction_category IS 'Category of interaction: DRUG_DRUG, DRUG_FOOD, DRUG_LAB, DRUG_HERBAL, DRUG_ALCOHOL, DRUG_DISEASE, OTHER';
COMMENT ON COLUMN ehr.prescription_interactions.interacting_substance IS 'Name of interacting substance (food, herbal, lab analyte, etc.)';
COMMENT ON COLUMN ehr.prescription_interactions.interacting_substance_type IS 'Type of interacting substance: FOOD, LAB_TEST, HERBAL, etc.';
COMMENT ON COLUMN ehr.prescription_interactions.mechanism IS 'Pharmacological mechanism of the interaction';
COMMENT ON COLUMN ehr.prescription_interactions.onset_time IS 'Estimated time to onset of the interaction (e.g. "within 24 hours")';
COMMENT ON COLUMN ehr.prescription_interactions.evidence_level IS 'Quality of evidence supporting this interaction (e.g. Established, Theoretical)';
COMMENT ON COLUMN ehr.prescription_interactions.documentation_references IS 'References to supporting clinical documentation';
