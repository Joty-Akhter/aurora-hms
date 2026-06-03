--liquibase formatted sql

--changeset easyops:003-create-medical-code-tables
--comment: Create tables for storing ICD-10, ICD-11, and SNOMED CT codes

-- ICD-10 Codes Table
CREATE TABLE IF NOT EXISTS ehr.icd10_codes (
    code VARCHAR(20) PRIMARY KEY,
    description TEXT NOT NULL,
    category TEXT,
    chapter VARCHAR(100),
    is_valid BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_icd10_description ON ehr.icd10_codes USING gin(to_tsvector('english', description));
CREATE INDEX IF NOT EXISTS idx_icd10_code_prefix ON ehr.icd10_codes(code varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_icd10_category ON ehr.icd10_codes(category) WHERE category IS NOT NULL;

-- ICD-11 Codes Table
CREATE TABLE IF NOT EXISTS ehr.icd11_codes (
    code VARCHAR(20) PRIMARY KEY,
    description TEXT NOT NULL,
    category VARCHAR(100),
    chapter VARCHAR(100),
    is_valid BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_icd11_description ON ehr.icd11_codes USING gin(to_tsvector('english', description));
CREATE INDEX IF NOT EXISTS idx_icd11_code_prefix ON ehr.icd11_codes(code varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_icd11_category ON ehr.icd11_codes(category) WHERE category IS NOT NULL;

-- SNOMED CT Codes Table
CREATE TABLE IF NOT EXISTS ehr.snomed_codes (
    code VARCHAR(50) PRIMARY KEY,
    description TEXT NOT NULL,
    concept_id VARCHAR(50),
    fully_specified_name TEXT,
    semantic_tag VARCHAR(100),
    is_valid BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_snomed_description ON ehr.snomed_codes USING gin(to_tsvector('english', description));
CREATE INDEX IF NOT EXISTS idx_snomed_code_prefix ON ehr.snomed_codes(code varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_snomed_concept_id ON ehr.snomed_codes(concept_id) WHERE concept_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_snomed_semantic_tag ON ehr.snomed_codes(semantic_tag) WHERE semantic_tag IS NOT NULL;

COMMENT ON TABLE ehr.icd10_codes IS 'ICD-10-CM diagnosis codes for billing and reporting';
COMMENT ON TABLE ehr.icd11_codes IS 'ICD-11 diagnosis codes - updated international classification';
COMMENT ON TABLE ehr.snomed_codes IS 'SNOMED CT clinical terminology codes for detailed clinical documentation';

--changeset easyops:003-seed-common-snomed-codes context:data
--comment: Seed common SNOMED CT codes

INSERT INTO ehr.snomed_codes (code, description, concept_id, fully_specified_name, semantic_tag) VALUES
('73211009', 'Diabetes mellitus type 2 (disorder)', '73211009', 'Diabetes mellitus type 2 (disorder)', 'disorder'),
('44054006', 'Diabetes mellitus type 1 (disorder)', '44054006', 'Diabetes mellitus type 1 (disorder)', 'disorder'),
('38341003', 'Hypertensive disorder (disorder)', '38341003', 'Hypertensive disorder (disorder)', 'disorder'),
('195967001', 'Asthma (disorder)', '195967001', 'Asthma (disorder)', 'disorder'),
('13645005', 'Chronic obstructive lung disease (disorder)', '13645005', 'Chronic obstructive lung disease (disorder)', 'disorder'),
('35489007', 'Depressive disorder (disorder)', '35489007', 'Depressive disorder (disorder)', 'disorder'),
('48694002', 'Anxiety disorder (disorder)', '48694002', 'Anxiety disorder (disorder)', 'disorder'),
('42399005', 'Essential hypertension (disorder)', '42399005', 'Essential hypertension (disorder)', 'disorder'),
('84114007', 'Heart failure (disorder)', '84114007', 'Heart failure (disorder)', 'disorder'),
('414545008', 'Ischemic heart disease (disorder)', '414545008', 'Ischemic heart disease (disorder)', 'disorder'),
('46635009', 'Type 1 diabetes mellitus without complication (disorder)', '46635009', 'Type 1 diabetes mellitus without complication (disorder)', 'disorder')
ON CONFLICT (code) DO NOTHING;
