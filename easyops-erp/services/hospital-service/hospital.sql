-- Hospital EHR Module Database Schema
-- File: easyops-erp/services/hospital-service/hospital.sql
-- Note: This file is NOT managed by Liquibase
-- Phase EHR.1: Patient Registration & Demographics

-- Schema Creation
CREATE SCHEMA IF NOT EXISTS ehr;

-- ============================================
-- PHASE EHR.1: PATIENT REGISTRATION TABLES
-- ============================================

-- Patients Table
CREATE TABLE IF NOT EXISTS ehr.patients (
    patient_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mrn VARCHAR(50) UNIQUE NOT NULL,
    organization_id UUID,
    
    -- Personal Identification (single legal/display name)
    full_name VARCHAR(255) NOT NULL,
    preferred_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    sex_at_birth VARCHAR(20),
    id_no VARCHAR(20),
    id_type VARCHAR(50),
    race VARCHAR(50),
    ethnicity VARCHAR(50),
    marital_status VARCHAR(20),
    father_name VARCHAR(200),
    mother_name VARCHAR(200),
    spouse_name VARCHAR(200),
    blood_group VARCHAR(20),
    religion VARCHAR(100),
    occupation VARCHAR(150),
    introduced_by VARCHAR(255),
    
    -- Contact Information
    primary_address_line1 VARCHAR(255),
    primary_address_line2 VARCHAR(255),
    primary_city VARCHAR(100),
    primary_state VARCHAR(50),
    primary_zip VARCHAR(20),
    primary_country VARCHAR(50) DEFAULT 'USA',
    mailing_address_line1 VARCHAR(255),
    mailing_address_line2 VARCHAR(255),
    mailing_city VARCHAR(100),
    mailing_state VARCHAR(50),
    mailing_zip VARCHAR(20),
    mailing_country VARCHAR(50),
    primary_phone VARCHAR(50),
    primary_phone_type VARCHAR(20),
    secondary_phone VARCHAR(50),
    secondary_phone_type VARCHAR(20),
    primary_email VARCHAR(255),
    secondary_email VARCHAR(255),
    preferred_contact_method VARCHAR(20),
    consent_text_messaging BOOLEAN DEFAULT false,
    consent_email_communication BOOLEAN DEFAULT false,
    
    -- Clinical Assignment
    primary_care_provider_id UUID,
    primary_care_location_id UUID,
    referring_physician_id UUID,
    patient_status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- Clinical Information
    preferred_language VARCHAR(50) DEFAULT 'English',
    interpreter_needed BOOLEAN DEFAULT false,
    special_needs TEXT,
    
    -- Administrative Information
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registered_by UUID,
    registration_location_id UUID,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT chk_patient_status CHECK (patient_status IN ('ACTIVE', 'INACTIVE', 'DECEASED', 'ARCHIVED')),
    CONSTRAINT chk_gender CHECK (gender IN ('Male', 'Female', 'Other', 'Prefer not to answer')),
    CONSTRAINT chk_preferred_contact CHECK (preferred_contact_method IN ('Phone', 'Email', 'Mail', 'Text Message'))
);

-- Indexes for Patients
CREATE INDEX IF NOT EXISTS idx_patients_mrn ON ehr.patients(mrn);
CREATE INDEX IF NOT EXISTS idx_patients_full_name ON ehr.patients(full_name);
CREATE INDEX IF NOT EXISTS idx_patients_dob ON ehr.patients(date_of_birth);
CREATE INDEX IF NOT EXISTS idx_patients_dup_name_dob ON ehr.patients(full_name, date_of_birth);
CREATE INDEX IF NOT EXISTS idx_patients_id_no ON ehr.patients(id_no) WHERE id_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patients_phone ON ehr.patients(primary_phone) WHERE primary_phone IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patients_email ON ehr.patients(primary_email) WHERE primary_email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patients_status ON ehr.patients(patient_status);
CREATE INDEX IF NOT EXISTS idx_patients_organization ON ehr.patients(organization_id) WHERE organization_id IS NOT NULL;

-- Patient Emergency Contacts Table
CREATE TABLE IF NOT EXISTS ehr.patient_emergency_contacts (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    primary_phone VARCHAR(50) NOT NULL,
    secondary_phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    zip VARCHAR(20),
    country VARCHAR(50),
    email VARCHAR(255),
    is_primary BOOLEAN DEFAULT false,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_emergency_contact_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_relationship CHECK (relationship IN ('Spouse', 'Parent', 'Child', 'Sibling', 'Friend', 'Other'))
);

-- Indexes for Emergency Contacts
CREATE INDEX IF NOT EXISTS idx_emergency_contacts_patient ON ehr.patient_emergency_contacts(patient_id);
CREATE INDEX IF NOT EXISTS idx_emergency_contacts_primary ON ehr.patient_emergency_contacts(patient_id, is_primary) WHERE is_primary = true;

-- Patient Insurance Table
CREATE TABLE IF NOT EXISTS ehr.patient_insurance (
    insurance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    insurance_type VARCHAR(20) NOT NULL,
    insurance_company_name VARCHAR(200),
    policy_number VARCHAR(100),
    group_number VARCHAR(100),
    subscriber_name VARCHAR(200),
    subscriber_dob DATE,
    subscriber_relationship VARCHAR(20),
    effective_date DATE,
    expiration_date DATE,
    copay_amount DECIMAL(10, 2),
    verification_status VARCHAR(20) DEFAULT 'Not_Verified',
    verified_date DATE,
    insurance_phone VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_insurance_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_insurance_type CHECK (insurance_type IN ('PRIMARY', 'SECONDARY', 'TERTIARY')),
    CONSTRAINT chk_patient_insurance_verification_status CHECK (verification_status IN ('Verified', 'Pending', 'Not_Verified', 'Not_Applicable')),
    CONSTRAINT chk_subscriber_relationship CHECK (subscriber_relationship IN ('Self', 'Spouse', 'Child', 'Other'))
);

-- Indexes for Insurance
CREATE INDEX IF NOT EXISTS idx_insurance_patient ON ehr.patient_insurance(patient_id);
CREATE INDEX IF NOT EXISTS idx_insurance_type ON ehr.patient_insurance(patient_id, insurance_type);
CREATE INDEX IF NOT EXISTS idx_insurance_policy ON ehr.patient_insurance(policy_number) WHERE policy_number IS NOT NULL;

-- Patient Consents Table
CREATE TABLE IF NOT EXISTS ehr.patient_consents (
    consent_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    consent_status VARCHAR(20) NOT NULL,
    consent_date DATE NOT NULL,
    signature TEXT,
    expires_date DATE,
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_consent_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_consent_type CHECK (consent_type IN ('HIPAA', 'FINANCIAL', 'MARKETING', 'TREATMENT')),
    CONSTRAINT chk_consent_status CHECK (consent_status IN ('GRANTED', 'DENIED', 'REVOKED'))
);

-- Indexes for Consents
CREATE INDEX IF NOT EXISTS idx_consents_patient ON ehr.patient_consents(patient_id);
CREATE INDEX IF NOT EXISTS idx_consents_type ON ehr.patient_consents(patient_id, consent_type);
CREATE INDEX IF NOT EXISTS idx_consents_status ON ehr.patient_consents(patient_id, consent_status);

CREATE OR REPLACE FUNCTION ehr.update_updated_at_column()
RETURNS TRIGGER AS '
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_patients_updated_at ON ehr.patients;
CREATE TRIGGER update_patients_updated_at
    BEFORE UPDATE ON ehr.patients
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_emergency_contacts_updated_at ON ehr.patient_emergency_contacts;
CREATE TRIGGER update_emergency_contacts_updated_at
    BEFORE UPDATE ON ehr.patient_emergency_contacts
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_insurance_updated_at ON ehr.patient_insurance;
CREATE TRIGGER update_insurance_updated_at
    BEFORE UPDATE ON ehr.patient_insurance
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_consents_updated_at ON ehr.patient_consents;
CREATE TRIGGER update_consents_updated_at
    BEFORE UPDATE ON ehr.patient_consents
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments for documentation
COMMENT ON SCHEMA ehr IS 'Hospital Electronic Health Record (EHR) Module Schema';
COMMENT ON TABLE ehr.patients IS 'Patient registration and demographic information';
COMMENT ON TABLE ehr.patient_emergency_contacts IS 'Emergency contact information for patients';
COMMENT ON TABLE ehr.patient_insurance IS 'Patient insurance information (primary, secondary, tertiary)';
COMMENT ON TABLE ehr.patient_consents IS 'Patient consent records (HIPAA, financial, marketing, treatment)';

COMMENT ON COLUMN ehr.patients.mrn IS 'Medical Record Number - unique identifier for patient';
COMMENT ON COLUMN ehr.patients.patient_status IS 'Patient status: ACTIVE, INACTIVE, DECEASED, ARCHIVED';
COMMENT ON COLUMN ehr.patient_emergency_contacts.is_primary IS 'Indicates if this is the primary emergency contact';

-- ============================================
-- PHASE EHR.2: MEDICAL HISTORY & ALLERGIES TABLES
-- ============================================

-- Patient Medical History Table
CREATE TABLE IF NOT EXISTS ehr.patient_medical_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    history_type VARCHAR(50) NOT NULL,
    condition_name VARCHAR(200) NOT NULL,
    icd10_code VARCHAR(20),
    icd11_code VARCHAR(20),
    snomed_code VARCHAR(50),
    onset_date DATE,
    resolution_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    severity VARCHAR(20),
    notes TEXT,
    documented_by UUID,
    documented_date DATE DEFAULT CURRENT_DATE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_medical_history_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_history_type CHECK (history_type IN ('PAST_MEDICAL', 'FAMILY', 'SOCIAL', 'IMMUNIZATION')),
    CONSTRAINT chk_history_status CHECK (status IN ('ACTIVE', 'RESOLVED', 'CHRONIC', 'INACTIVE'))
);

-- Indexes for Medical History
CREATE INDEX IF NOT EXISTS idx_medical_history_patient ON ehr.patient_medical_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_history_type ON ehr.patient_medical_history(patient_id, history_type);
CREATE INDEX IF NOT EXISTS idx_medical_history_status ON ehr.patient_medical_history(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_medical_history_icd10 ON ehr.patient_medical_history(icd10_code) WHERE icd10_code IS NOT NULL;

-- Family History Table
CREATE TABLE IF NOT EXISTS ehr.family_history (
    family_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    family_member_relationship VARCHAR(50) NOT NULL,
    condition_name VARCHAR(200) NOT NULL,
    icd10_code VARCHAR(20),
    icd11_code VARCHAR(20),
    snomed_code VARCHAR(50),
    age_at_onset INTEGER,
    age_at_death INTEGER,
    notes TEXT,
    documented_date DATE DEFAULT CURRENT_DATE,
    documented_by UUID,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_family_history_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_relationship CHECK (family_member_relationship IN 
        ('Mother', 'Father', 'Sister', 'Brother', 'Maternal Grandmother', 'Maternal Grandfather',
         'Paternal Grandmother', 'Paternal Grandfather', 'Aunt', 'Uncle', 'Cousin', 'Other'))
);

-- Indexes for Family History
CREATE INDEX IF NOT EXISTS idx_family_history_patient ON ehr.family_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_family_history_relationship ON ehr.family_history(patient_id, family_member_relationship);

-- Social History Table
CREATE TABLE IF NOT EXISTS ehr.social_history (
    social_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    frequency VARCHAR(100),
    quantity VARCHAR(100),
    duration_years INTEGER,
    start_date DATE,
    end_date DATE,
    notes TEXT,
    documented_date DATE DEFAULT CURRENT_DATE,
    documented_by UUID,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_social_history_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_social_category CHECK (category IN 
        ('SMOKING', 'ALCOHOL', 'DRUGS', 'OCCUPATION', 'LIFESTYLE', 'EXERCISE', 'DIET', 'OTHER')),
    CONSTRAINT chk_social_status CHECK (status IN ('CURRENT', 'PAST', 'NEVER'))
);

-- Indexes for Social History
CREATE INDEX IF NOT EXISTS idx_social_history_patient ON ehr.social_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_social_history_category ON ehr.social_history(patient_id, category);
CREATE INDEX IF NOT EXISTS idx_social_history_status ON ehr.social_history(patient_id, status);

-- Immunizations Table
CREATE TABLE IF NOT EXISTS ehr.immunizations (
    immunization_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    vaccine_name VARCHAR(200) NOT NULL,
    cvx_code VARCHAR(10),
    administration_date DATE NOT NULL,
    lot_number VARCHAR(100),
    manufacturer VARCHAR(200),
    route VARCHAR(50),
    site VARCHAR(100),
    dose VARCHAR(100),
    administered_by UUID,
    administered_location_id UUID,
    reaction TEXT,
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_immunization_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_route CHECK (route IN 
        ('IM', 'SC', 'ID', 'IN', 'PO', 'IV', 'NASAL', 'OPHTHALMIC', 'OTIC', 'OTHER'))
);

-- Indexes for Immunizations
CREATE INDEX IF NOT EXISTS idx_immunizations_patient ON ehr.immunizations(patient_id);
CREATE INDEX IF NOT EXISTS idx_immunizations_date ON ehr.immunizations(patient_id, administration_date);
CREATE INDEX IF NOT EXISTS idx_immunizations_cvx ON ehr.immunizations(cvx_code) WHERE cvx_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_immunizations_vaccine ON ehr.immunizations(patient_id, vaccine_name);

-- Allergies Table
CREATE TABLE IF NOT EXISTS ehr.allergies (
    allergy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    allergen_name VARCHAR(200) NOT NULL,
    allergen_type VARCHAR(50) NOT NULL,
    allergen_code VARCHAR(50),
    reaction_type VARCHAR(100),
    severity VARCHAR(50) NOT NULL,
    onset_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    verification_status VARCHAR(20) DEFAULT 'UNCONFIRMED',
    documented_by UUID,
    documented_date DATE DEFAULT CURRENT_DATE,
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_allergy_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_allergen_type CHECK (allergen_type IN 
        ('DRUG', 'FOOD', 'ENVIRONMENTAL', 'LATEX', 'OTHER')),
    CONSTRAINT chk_allergy_severity CHECK (severity IN 
        ('MILD', 'MODERATE', 'SEVERE', 'LIFE_THREATENING')),
    CONSTRAINT chk_allergy_status CHECK (status IN ('ACTIVE', 'RESOLVED', 'UNKNOWN')),
    CONSTRAINT chk_allergy_verification_status CHECK (verification_status IN 
        ('CONFIRMED', 'UNCONFIRMED', 'REFUTED'))
);

-- Indexes for Allergies
CREATE INDEX IF NOT EXISTS idx_allergies_patient ON ehr.allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_allergies_status ON ehr.allergies(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_allergies_type ON ehr.allergies(patient_id, allergen_type);
CREATE INDEX IF NOT EXISTS idx_allergies_severity ON ehr.allergies(patient_id, severity);
CREATE INDEX IF NOT EXISTS idx_allergies_allergen_name ON ehr.allergies(allergen_name);

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_medical_history_updated_at ON ehr.patient_medical_history;
CREATE TRIGGER update_medical_history_updated_at
    BEFORE UPDATE ON ehr.patient_medical_history
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_family_history_updated_at ON ehr.family_history;
CREATE TRIGGER update_family_history_updated_at
    BEFORE UPDATE ON ehr.family_history
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_social_history_updated_at ON ehr.social_history;
CREATE TRIGGER update_social_history_updated_at
    BEFORE UPDATE ON ehr.social_history
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_immunizations_updated_at ON ehr.immunizations;
CREATE TRIGGER update_immunizations_updated_at
    BEFORE UPDATE ON ehr.immunizations
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_allergies_updated_at ON ehr.allergies;
CREATE TRIGGER update_allergies_updated_at
    BEFORE UPDATE ON ehr.allergies
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments for Phase EHR.2 tables
COMMENT ON TABLE ehr.patient_medical_history IS 'Patient past medical history, conditions, and diagnoses';
COMMENT ON TABLE ehr.family_history IS 'Family medical history with relationship mapping';
COMMENT ON TABLE ehr.social_history IS 'Social history including lifestyle factors and risk behaviors';
COMMENT ON TABLE ehr.immunizations IS 'Complete immunization/vaccination records';
COMMENT ON TABLE ehr.allergies IS 'Allergy and adverse reaction tracking';

COMMENT ON COLUMN ehr.patient_medical_history.history_type IS 'Type: PAST_MEDICAL, FAMILY, SOCIAL, IMMUNIZATION';
COMMENT ON COLUMN ehr.allergies.verification_status IS 'Allergy verification: CONFIRMED, UNCONFIRMED, REFUTED';
COMMENT ON COLUMN ehr.immunizations.cvx_code IS 'CVX (Vaccine Codes) standard code for vaccine identification';

-- ============================================
-- PHASE EHR.3: VITAL SIGNS & CLINICAL MEASUREMENTS TABLES
-- ============================================

-- Vital Signs Table
CREATE TABLE IF NOT EXISTS ehr.vital_signs (
    vital_sign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    measurement_date DATE NOT NULL,
    measurement_time TIME NOT NULL,
    
    -- Blood Pressure
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    
    -- Heart Rate and Respiratory
    heart_rate INTEGER,
    respiratory_rate INTEGER,
    
    -- Temperature
    temperature DECIMAL(5, 2),
    temperature_unit VARCHAR(1) DEFAULT 'F',
    
    -- Oxygen Saturation
    oxygen_saturation DECIMAL(5, 2),
    
    -- Weight and Height
    weight DECIMAL(6, 2),
    weight_unit VARCHAR(10) DEFAULT 'lbs',
    height DECIMAL(6, 2),
    height_unit VARCHAR(10) DEFAULT 'in',
    
    -- BMI (calculated)
    bmi DECIMAL(5, 2),
    
    -- Additional Measurements
    pain_scale INTEGER,
    blood_glucose DECIMAL(6, 2),
    head_circumference DECIMAL(6, 2),
    
    -- Context Information
    measured_by UUID,
    measurement_location_id UUID,
    device_used VARCHAR(200),
    patient_position VARCHAR(50),
    notes TEXT,
    
    -- Status Flags
    is_abnormal BOOLEAN DEFAULT false,
    is_critical BOOLEAN DEFAULT false,
    abnormal_reason TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_vital_signs_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_temperature_unit CHECK (temperature_unit IN ('C', 'F')),
    CONSTRAINT chk_weight_unit CHECK (weight_unit IN ('lbs', 'kg')),
    CONSTRAINT chk_height_unit CHECK (height_unit IN ('in', 'cm', 'm')),
    CONSTRAINT chk_pain_scale CHECK (pain_scale IS NULL OR (pain_scale >= 0 AND pain_scale <= 10)),
    CONSTRAINT chk_systolic_bp CHECK (systolic_bp IS NULL OR (systolic_bp >= 0 AND systolic_bp <= 300)),
    CONSTRAINT chk_diastolic_bp CHECK (diastolic_bp IS NULL OR (diastolic_bp >= 0 AND diastolic_bp <= 200)),
    CONSTRAINT chk_heart_rate CHECK (heart_rate IS NULL OR (heart_rate >= 0 AND heart_rate <= 300)),
    CONSTRAINT chk_respiratory_rate CHECK (respiratory_rate IS NULL OR (respiratory_rate >= 0 AND respiratory_rate <= 100)),
    CONSTRAINT chk_oxygen_saturation CHECK (oxygen_saturation IS NULL OR (oxygen_saturation >= 0 AND oxygen_saturation <= 100))
);

-- Indexes for Vital Signs
CREATE INDEX IF NOT EXISTS idx_vital_signs_patient ON ehr.vital_signs(patient_id);
CREATE INDEX IF NOT EXISTS idx_vital_signs_date ON ehr.vital_signs(patient_id, measurement_date DESC);
CREATE INDEX IF NOT EXISTS idx_vital_signs_encounter ON ehr.vital_signs(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_vital_signs_abnormal ON ehr.vital_signs(patient_id, is_abnormal) WHERE is_abnormal = true;
CREATE INDEX IF NOT EXISTS idx_vital_signs_critical ON ehr.vital_signs(patient_id, is_critical) WHERE is_critical = true;
CREATE INDEX IF NOT EXISTS idx_vital_signs_datetime ON ehr.vital_signs(patient_id, measurement_date, measurement_time);

-- Vital Signs Trends View (for analytics)
CREATE OR REPLACE VIEW ehr.v_vital_signs_trends AS
SELECT 
    patient_id,
    measurement_date,
    AVG(systolic_bp) as avg_systolic_bp,
    AVG(diastolic_bp) as avg_diastolic_bp,
    AVG(heart_rate) as avg_heart_rate,
    AVG(respiratory_rate) as avg_respiratory_rate,
    AVG(temperature) as avg_temperature,
    AVG(oxygen_saturation) as avg_oxygen_saturation,
    AVG(weight) as avg_weight,
    AVG(bmi) as avg_bmi,
    COUNT(*) as measurement_count
FROM ehr.vital_signs
GROUP BY patient_id, measurement_date;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS update_vital_signs_updated_at ON ehr.vital_signs;
CREATE TRIGGER update_vital_signs_updated_at
    BEFORE UPDATE ON ehr.vital_signs
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Function to calculate BMI
CREATE OR REPLACE FUNCTION ehr.calculate_bmi(weight_val DECIMAL, weight_unit_val VARCHAR, 
                                               height_val DECIMAL, height_unit_val VARCHAR)
RETURNS DECIMAL AS '
DECLARE
    weight_kg DECIMAL;
    height_m DECIMAL;
BEGIN
    IF weight_unit_val = ''lbs'' THEN
        weight_kg := weight_val * 0.453592;
    ELSE
        weight_kg := weight_val;
    END IF;
    IF height_unit_val = ''in'' THEN
        height_m := height_val * 0.0254;
    ELSIF height_unit_val = ''cm'' THEN
        height_m := height_val / 100.0;
    ELSE
        height_m := height_val;
    END IF;
    IF height_m > 0 THEN
        RETURN ROUND((weight_kg / (height_m * height_m))::DECIMAL, 2);
    ELSE
        RETURN NULL;
    END IF;
END;
' LANGUAGE plpgsql;

-- Trigger to auto-calculate BMI
CREATE OR REPLACE FUNCTION ehr.auto_calculate_bmi()
RETURNS TRIGGER AS '
BEGIN
    IF NEW.weight IS NOT NULL AND NEW.height IS NOT NULL THEN
        NEW.bmi := ehr.calculate_bmi(NEW.weight, NEW.weight_unit, NEW.height, NEW.height_unit);
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS calculate_bmi_trigger ON ehr.vital_signs;
CREATE TRIGGER calculate_bmi_trigger
    BEFORE INSERT OR UPDATE ON ehr.vital_signs
    FOR EACH ROW
    WHEN (NEW.weight IS NOT NULL AND NEW.height IS NOT NULL)
    EXECUTE FUNCTION ehr.auto_calculate_bmi();

-- Comments for Phase EHR.3 tables
COMMENT ON TABLE ehr.vital_signs IS 'Vital signs and clinical measurements for patients';
COMMENT ON COLUMN ehr.vital_signs.bmi IS 'Body Mass Index - automatically calculated from weight and height';
COMMENT ON COLUMN ehr.vital_signs.is_abnormal IS 'Flag indicating if vital sign values are outside normal range';
COMMENT ON COLUMN ehr.vital_signs.is_critical IS 'Flag indicating if vital sign values are critical and require immediate attention';
COMMENT ON VIEW ehr.v_vital_signs_trends IS 'Daily aggregated vital signs trends for analytics and reporting';

-- ============================================
-- PHASE EHR.4: CLINICAL NOTES & DOCUMENTATION TABLES
-- ============================================

-- Clinical Notes Table
CREATE TABLE IF NOT EXISTS ehr.clinical_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Note Classification
    note_type VARCHAR(50) NOT NULL,
    
    -- Date and Time
    note_date DATE NOT NULL,
    note_time TIME NOT NULL,
    
    -- SOAP Note Components
    subjective TEXT,
    objective TEXT,
    assessment TEXT,
    plan TEXT,
    
    -- Additional Note Fields
    chief_complaint TEXT,
    review_of_systems TEXT,
    physical_examination TEXT,
    clinical_impression TEXT,
    treatment_plan TEXT,
    follow_up_instructions TEXT,
    
    -- Note Status and Workflow
    note_status VARCHAR(20) DEFAULT 'DRAFT',
    
    -- Authoring Information
    created_by UUID NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Signing Information
    signed_by UUID,
    signed_date TIMESTAMP,
    signature_method VARCHAR(50),
    
    -- Amendment Information
    amended_by UUID,
    amended_date TIMESTAMP,
    amendment_reason TEXT,
    original_note_id UUID,
    
    -- Versioning
    version_number INTEGER DEFAULT 1,
    is_current_version BOOLEAN DEFAULT true,
    
    -- Additional Metadata
    specialty VARCHAR(100),
    department_id UUID,
    location_id UUID,
    visit_type VARCHAR(50),
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    
    CONSTRAINT fk_clinical_note_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_clinical_note_original FOREIGN KEY (original_note_id) 
        REFERENCES ehr.clinical_notes(note_id) ON DELETE SET NULL,
    CONSTRAINT chk_note_type CHECK (note_type IN 
        ('SOAP', 'PROGRESS', 'CONSULTATION', 'DISCHARGE', 'PROCEDURE', 'ADMISSION', 'OPERATIVE', 'DOCTOR_NOTE', 'OTHER')),
    CONSTRAINT chk_note_status CHECK (note_status IN 
        ('DRAFT', 'FINAL', 'AMENDED', 'CORRECTED', 'VOIDED', 'SIGNED')),
    CONSTRAINT chk_signature_method CHECK (signature_method IS NULL OR signature_method IN 
        ('ELECTRONIC', 'DIGITAL', 'TYPED', 'VOICE', 'OTHER'))
);

-- Indexes for Clinical Notes
CREATE INDEX IF NOT EXISTS idx_clinical_notes_patient ON ehr.clinical_notes(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_date ON ehr.clinical_notes(patient_id, note_date DESC);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_encounter ON ehr.clinical_notes(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_clinical_notes_type ON ehr.clinical_notes(patient_id, note_type);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_status ON ehr.clinical_notes(patient_id, note_status);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_created_by ON ehr.clinical_notes(created_by);
CREATE INDEX IF NOT EXISTS idx_clinical_notes_signed_by ON ehr.clinical_notes(signed_by) WHERE signed_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_clinical_notes_version ON ehr.clinical_notes(original_note_id, version_number) WHERE original_note_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_clinical_notes_current_version ON ehr.clinical_notes(patient_id, is_current_version) WHERE is_current_version = true;
CREATE INDEX IF NOT EXISTS idx_clinical_notes_datetime ON ehr.clinical_notes(patient_id, note_date, note_time);

-- Note Attachments Table
CREATE TABLE IF NOT EXISTS ehr.note_attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL,
    
    -- File Information
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    file_path VARCHAR(500) NOT NULL,
    file_hash VARCHAR(255),
    mime_type VARCHAR(100),
    
    -- Metadata
    description TEXT,
    attachment_type VARCHAR(50),
    uploaded_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID NOT NULL,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_attachment_note FOREIGN KEY (note_id) 
        REFERENCES ehr.clinical_notes(note_id) ON DELETE CASCADE,
    CONSTRAINT chk_attachment_type CHECK (attachment_type IS NULL OR attachment_type IN 
        ('IMAGE', 'DOCUMENT', 'LAB_RESULT', 'IMAGING', 'AUDIO', 'VIDEO', 'OTHER'))
);

-- Indexes for Note Attachments
CREATE INDEX IF NOT EXISTS idx_note_attachments_note ON ehr.note_attachments(note_id);
CREATE INDEX IF NOT EXISTS idx_note_attachments_active ON ehr.note_attachments(note_id, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_note_attachments_type ON ehr.note_attachments(attachment_type) WHERE attachment_type IS NOT NULL;

-- Medications Table (Current Medication List)
-- NOTE: Defined here so that foreign keys from other tables (e.g. clinical_note_medications)
-- can safely reference ehr.medications.
CREATE TABLE IF NOT EXISTS ehr.medications (
    medication_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Medication Identification
    medication_name VARCHAR(500) NOT NULL,
    generic_name VARCHAR(500),
    medication_code VARCHAR(100), -- RxNorm or NDC code
    medication_code_type VARCHAR(20), -- RXNORM, NDC, OTHER
    ndc_code VARCHAR(20), -- National Drug Code
    rxnorm_code VARCHAR(20), -- RxNorm code
    
    -- Dosage Information
    dosage_strength DECIMAL(10, 3),
    dosage_unit VARCHAR(50),
    dosage_form VARCHAR(50), -- TABLET, CAPSULE, LIQUID, etc.
    quantity DECIMAL(10, 2),
    quantity_unit VARCHAR(50),
    
    -- Administration Instructions
    route VARCHAR(50), -- ORAL, IV, IM, TOPICAL, etc.
    frequency VARCHAR(200),
    timing VARCHAR(200), -- e.g., "with meals", "before bedtime"
    instructions TEXT,
    
    -- Prescription Information (if from prescription)
    prescription_id UUID,
    prescribing_provider_id UUID,
    prescribing_provider_name VARCHAR(200),
    prescribing_provider_npi VARCHAR(20),
    prescription_date DATE,
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    refills_authorized INTEGER DEFAULT 0,
    refills_remaining INTEGER DEFAULT 0,
    
    -- Medication Status
    medication_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DISCONTINUED, ON_HOLD, COMPLETED
    status_date DATE,
    status_changed_by UUID,
    
    -- Indication/Reason
    indication TEXT, -- Reason for medication
    diagnosis_code VARCHAR(20), -- ICD-10 code
    
    -- Medication Source
    medication_source VARCHAR(50) NOT NULL, -- PRESCRIPTION, PATIENT_REPORTED, PHARMACY, CLINICAL_DOCUMENTATION, EXTERNAL_IMPORT, OTHER
    
    -- Date Information
    start_date DATE NOT NULL,
    end_date DATE,
    last_filled_date DATE,
    
    -- Additional Information
    notes TEXT,
    special_instructions TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Clinical Note - Medication Links Table
CREATE TABLE IF NOT EXISTS ehr.clinical_note_medications (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL,
    medication_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'DOCUMENTED', -- DOCUMENTED, PRESCRIBED, DISCONTINUED, MODIFIED, MONITORED, OTHER
    link_strength VARCHAR(20) DEFAULT 'MODERATE', -- WEAK, MODERATE, STRONG
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_clinical_note_medication_note FOREIGN KEY (note_id) 
        REFERENCES ehr.clinical_notes(note_id) ON DELETE CASCADE,
    CONSTRAINT fk_clinical_note_medication_medication FOREIGN KEY (medication_id) 
        REFERENCES ehr.medications(medication_id) ON DELETE CASCADE,
    CONSTRAINT chk_clinical_note_med_link_type CHECK (link_type IN ('DOCUMENTED', 'PRESCRIBED', 'DISCONTINUED', 'MODIFIED', 'MONITORED', 'OTHER')),
    CONSTRAINT chk_clinical_note_med_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT uk_clinical_note_medication UNIQUE (note_id, medication_id)
);

-- Indexes for Clinical Note Medications
CREATE INDEX IF NOT EXISTS idx_clinical_note_medications_note ON ehr.clinical_note_medications(note_id);
CREATE INDEX IF NOT EXISTS idx_clinical_note_medications_medication ON ehr.clinical_note_medications(medication_id);
CREATE INDEX IF NOT EXISTS idx_clinical_note_medications_org ON ehr.clinical_note_medications(organization_id) WHERE organization_id IS NOT NULL;

-- Note Templates Table
CREATE TABLE IF NOT EXISTS ehr.note_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Template Identification
    template_name VARCHAR(200) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    specialty VARCHAR(100),
    department_id UUID,
    
    -- Template Content
    template_content JSONB,
    
    -- Template Metadata
    description TEXT,
    is_system_template BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    is_public BOOLEAN DEFAULT false,
    
    -- Ownership
    created_by UUID,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Usage Tracking
    usage_count INTEGER DEFAULT 0,
    last_used_date TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    
    CONSTRAINT chk_template_type CHECK (template_type IN 
        ('SOAP', 'PROGRESS', 'CONSULTATION', 'DISCHARGE', 'PROCEDURE', 'ADMISSION', 'OPERATIVE', 'OTHER'))
);

-- Indexes for Note Templates
CREATE INDEX IF NOT EXISTS idx_note_templates_type ON ehr.note_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_note_templates_specialty ON ehr.note_templates(specialty) WHERE specialty IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_note_templates_active ON ehr.note_templates(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_note_templates_system ON ehr.note_templates(is_system_template) WHERE is_system_template = true;
CREATE INDEX IF NOT EXISTS idx_note_templates_public ON ehr.note_templates(is_public) WHERE is_public = true;
CREATE INDEX IF NOT EXISTS idx_note_templates_created_by ON ehr.note_templates(created_by) WHERE created_by IS NOT NULL;

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_clinical_notes_updated_at ON ehr.clinical_notes;
CREATE TRIGGER update_clinical_notes_updated_at
    BEFORE UPDATE ON ehr.clinical_notes
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_note_attachments_updated_at ON ehr.note_attachments;
CREATE TRIGGER update_note_attachments_updated_at
    BEFORE UPDATE ON ehr.note_attachments
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_note_templates_updated_at ON ehr.note_templates;
CREATE TRIGGER update_note_templates_updated_at
    BEFORE UPDATE ON ehr.note_templates
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_clinical_note_medications_updated_at ON ehr.clinical_note_medications;
CREATE TRIGGER update_clinical_note_medications_updated_at
    BEFORE UPDATE ON ehr.clinical_note_medications
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments for Phase EHR.4 tables
COMMENT ON TABLE ehr.clinical_notes IS 'Clinical documentation including SOAP notes, progress notes, consultations, and discharge summaries';
COMMENT ON TABLE ehr.note_attachments IS 'File attachments associated with clinical notes (images, documents, lab results, etc.)';
COMMENT ON TABLE ehr.note_templates IS 'Reusable note templates for standardized clinical documentation';

COMMENT ON COLUMN ehr.clinical_notes.note_type IS 'Type of note: SOAP, PROGRESS, CONSULTATION, DISCHARGE, PROCEDURE, ADMISSION, OPERATIVE, OTHER';
COMMENT ON COLUMN ehr.clinical_notes.note_status IS 'Status: DRAFT, FINAL, AMENDED, CORRECTED, VOIDED, SIGNED';
COMMENT ON COLUMN ehr.clinical_notes.version_number IS 'Version number for note amendments and corrections';
COMMENT ON COLUMN ehr.clinical_notes.is_current_version IS 'Indicates if this is the current version of the note';
COMMENT ON COLUMN ehr.clinical_notes.original_note_id IS 'Reference to original note for amendments and corrections';
COMMENT ON COLUMN ehr.note_templates.template_content IS 'JSON structure containing template fields and default values';
COMMENT ON COLUMN ehr.note_templates.is_system_template IS 'Indicates if this is a system-provided template (cannot be deleted)';

-- ============================================
-- PHASE EHR.5: DIAGNOSES & PROBLEM LISTS TABLES
-- ============================================

-- Patient Problems Table
CREATE TABLE IF NOT EXISTS ehr.patient_problems (
    problem_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Problem Identification
    problem_name VARCHAR(500) NOT NULL,
    icd10_code VARCHAR(20),
    icd11_code VARCHAR(20),
    snomed_code VARCHAR(50),
    
    -- Problem Classification
    problem_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- Dates
    onset_date DATE,
    resolution_date DATE,
    
    -- Clinical Details
    severity VARCHAR(50),
    chronicity VARCHAR(50),
    priority VARCHAR(20),
    
    -- Documentation
    documented_by UUID NOT NULL,
    documented_date DATE DEFAULT CURRENT_DATE,
    
    -- Resolution Information
    resolved_by UUID,
    resolved_date DATE,
    resolution_notes TEXT,
    
    -- Additional Information
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_problem_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_problem_type CHECK (problem_type IN 
        ('DIAGNOSIS', 'SYMPTOM', 'FINDING', 'CONDITION', 'ALLERGY', 'OTHER')),
    CONSTRAINT chk_problem_status CHECK (status IN 
        ('ACTIVE', 'RESOLVED', 'INACTIVE', 'RULED_OUT', 'CHRONIC', 'REMISSION')),
    CONSTRAINT chk_problem_severity CHECK (severity IS NULL OR severity IN 
        ('MILD', 'MODERATE', 'SEVERE', 'CRITICAL')),
    CONSTRAINT chk_problem_priority CHECK (priority IS NULL OR priority IN 
        ('HIGH', 'MEDIUM', 'LOW'))
);

-- Indexes for Patient Problems
CREATE INDEX IF NOT EXISTS idx_patient_problems_patient ON ehr.patient_problems(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_problems_status ON ehr.patient_problems(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_patient_problems_type ON ehr.patient_problems(patient_id, problem_type);
CREATE INDEX IF NOT EXISTS idx_patient_problems_active ON ehr.patient_problems(patient_id, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_patient_problems_resolved ON ehr.patient_problems(patient_id, status) WHERE status = 'RESOLVED';
CREATE INDEX IF NOT EXISTS idx_patient_problems_encounter ON ehr.patient_problems(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_problems_icd10 ON ehr.patient_problems(icd10_code) WHERE icd10_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_problems_icd11 ON ehr.patient_problems(icd11_code) WHERE icd11_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_problems_snomed ON ehr.patient_problems(snomed_code) WHERE snomed_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_problems_priority ON ehr.patient_problems(patient_id, priority) WHERE priority IS NOT NULL;

-- Problem History Table (Audit Trail)
CREATE TABLE IF NOT EXISTS ehr.problem_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL,
    
    -- Change Information
    change_type VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    changed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Change Details
    previous_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    
    -- Additional Context
    field_name VARCHAR(100),
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_problem_history_problem FOREIGN KEY (problem_id) 
        REFERENCES ehr.patient_problems(problem_id) ON DELETE CASCADE,
    CONSTRAINT chk_change_type CHECK (change_type IN 
        ('CREATED', 'UPDATED', 'RESOLVED', 'REACTIVATED', 'STATUS_CHANGED', 'CODE_UPDATED', 'OTHER'))
);

-- Indexes for Problem History
CREATE INDEX IF NOT EXISTS idx_problem_history_problem ON ehr.problem_history(problem_id);
CREATE INDEX IF NOT EXISTS idx_problem_history_date ON ehr.problem_history(problem_id, changed_date DESC);
CREATE INDEX IF NOT EXISTS idx_problem_history_type ON ehr.problem_history(problem_id, change_type);
CREATE INDEX IF NOT EXISTS idx_problem_history_changed_by ON ehr.problem_history(changed_by);

-- Problem - Medication Links Table
CREATE TABLE IF NOT EXISTS ehr.problem_medications (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL,
    medication_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'TREATS', -- TREATS, PREVENTS, CONTRAINDICATED, MONITORS, CAUSES, OTHER
    link_strength VARCHAR(20) DEFAULT 'MODERATE', -- WEAK, MODERATE, STRONG
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_problem_medication_problem FOREIGN KEY (problem_id) 
        REFERENCES ehr.patient_problems(problem_id) ON DELETE CASCADE,
    CONSTRAINT fk_problem_medication_medication FOREIGN KEY (medication_id) 
        REFERENCES ehr.medications(medication_id) ON DELETE CASCADE,
    CONSTRAINT chk_problem_med_link_type CHECK (link_type IN ('TREATS', 'PREVENTS', 'CONTRAINDICATED', 'MONITORS', 'CAUSES', 'OTHER')),
    CONSTRAINT chk_problem_med_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT uk_problem_medication UNIQUE (problem_id, medication_id)
);

-- Indexes for Problem Medications
CREATE INDEX IF NOT EXISTS idx_problem_medications_problem ON ehr.problem_medications(problem_id);
CREATE INDEX IF NOT EXISTS idx_problem_medications_medication ON ehr.problem_medications(medication_id);
CREATE INDEX IF NOT EXISTS idx_problem_medications_org ON ehr.problem_medications(organization_id) WHERE organization_id IS NOT NULL;

DROP TRIGGER IF EXISTS update_problem_medications_updated_at ON ehr.problem_medications;
CREATE TRIGGER update_problem_medications_updated_at
    BEFORE UPDATE ON ehr.problem_medications
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_patient_problems_updated_at ON ehr.patient_problems;
CREATE TRIGGER update_patient_problems_updated_at
    BEFORE UPDATE ON ehr.patient_problems
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Trigger to create history record on problem changes
CREATE OR REPLACE FUNCTION ehr.create_problem_history()
RETURNS TRIGGER AS '
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO ehr.problem_history (problem_id, change_type, changed_by, previous_value, new_value, field_name)
        VALUES (NEW.problem_id, 
                CASE 
                    WHEN NEW.status = ''RESOLVED'' THEN ''RESOLVED''
                    WHEN OLD.status = ''RESOLVED'' AND NEW.status = ''ACTIVE'' THEN ''REACTIVATED''
                    ELSE ''STATUS_CHANGED''
                END,
                COALESCE(NEW.updated_by, NEW.documented_by),
                OLD.status,
                NEW.status,
                ''status'');
    END IF;
    IF OLD.problem_name IS DISTINCT FROM NEW.problem_name THEN
        INSERT INTO ehr.problem_history (problem_id, change_type, changed_by, previous_value, new_value, field_name)
        VALUES (NEW.problem_id, ''UPDATED'', COALESCE(NEW.updated_by, NEW.documented_by), 
                OLD.problem_name, NEW.problem_name, ''problem_name'');
    END IF;
    IF OLD.icd10_code IS DISTINCT FROM NEW.icd10_code OR OLD.icd11_code IS DISTINCT FROM NEW.icd11_code OR 
       OLD.snomed_code IS DISTINCT FROM NEW.snomed_code THEN
        INSERT INTO ehr.problem_history (problem_id, change_type, changed_by, previous_value, new_value, field_name)
        VALUES (NEW.problem_id, ''CODE_UPDATED'', COALESCE(NEW.updated_by, NEW.documented_by),
                COALESCE(OLD.icd10_code, OLD.icd11_code, OLD.snomed_code, ''''),
                COALESCE(NEW.icd10_code, NEW.icd11_code, NEW.snomed_code, ''''),
                ''diagnosis_code'');
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS problem_history_trigger ON ehr.patient_problems;
CREATE TRIGGER problem_history_trigger
    AFTER UPDATE ON ehr.patient_problems
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status OR 
          OLD.problem_name IS DISTINCT FROM NEW.problem_name OR
          OLD.icd10_code IS DISTINCT FROM NEW.icd10_code OR
          OLD.icd11_code IS DISTINCT FROM NEW.icd11_code OR
          OLD.snomed_code IS DISTINCT FROM NEW.snomed_code)
    EXECUTE FUNCTION ehr.create_problem_history();

-- Trigger to create history record on problem creation
CREATE OR REPLACE FUNCTION ehr.create_problem_history_on_insert()
RETURNS TRIGGER AS '
BEGIN
    INSERT INTO ehr.problem_history (problem_id, change_type, changed_by, new_value, field_name)
    VALUES (NEW.problem_id, ''CREATED'', NEW.documented_by, NEW.problem_name, ''problem_name'');
    RETURN NEW;
END;
' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS problem_history_insert_trigger ON ehr.patient_problems;
CREATE TRIGGER problem_history_insert_trigger
    AFTER INSERT ON ehr.patient_problems
    FOR EACH ROW
    EXECUTE FUNCTION ehr.create_problem_history_on_insert();

-- Comments for Phase EHR.5 tables
COMMENT ON TABLE ehr.patient_problems IS 'Patient problem list including diagnoses, symptoms, findings, and conditions';
COMMENT ON TABLE ehr.problem_history IS 'Audit trail for all changes to patient problems (status changes, updates, resolutions)';

COMMENT ON COLUMN ehr.patient_problems.problem_type IS 'Type: DIAGNOSIS, SYMPTOM, FINDING, CONDITION, ALLERGY, OTHER';
COMMENT ON COLUMN ehr.patient_problems.status IS 'Status: ACTIVE, RESOLVED, INACTIVE, RULED_OUT, CHRONIC, REMISSION';
COMMENT ON COLUMN ehr.patient_problems.icd10_code IS 'ICD-10-CM diagnosis code';
COMMENT ON COLUMN ehr.patient_problems.icd11_code IS 'ICD-11 diagnosis code';
COMMENT ON COLUMN ehr.patient_problems.snomed_code IS 'SNOMED CT code for clinical terminology';
COMMENT ON COLUMN ehr.problem_history.change_type IS 'Type of change: CREATED, UPDATED, RESOLVED, REACTIVATED, STATUS_CHANGED, CODE_UPDATED, OTHER';

-- ============================================
-- PHASE EHR.6: PRESCRIPTION CREATION & MANAGEMENT TABLES
-- ============================================

-- Prescriptions Table
CREATE TABLE IF NOT EXISTS ehr.prescriptions (
    prescription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Prescription Identification
    prescription_number VARCHAR(100) UNIQUE NOT NULL,
    prescription_type VARCHAR(50) DEFAULT 'ELECTRONIC',
    
    -- Medication Information
    medication_name VARCHAR(500) NOT NULL,
    medication_code VARCHAR(100), -- RxNorm or NDC code
    medication_code_type VARCHAR(20), -- RXNORM, NDC, OTHER
    
    -- Dosage Information
    dosage_strength DECIMAL(10, 3),
    dosage_unit VARCHAR(50),
    dosage_form VARCHAR(50) NOT NULL,
    
    -- Quantity and Administration
    quantity DECIMAL(10, 2),
    quantity_unit VARCHAR(50),
    route VARCHAR(50) NOT NULL,
    frequency VARCHAR(200),
    instructions TEXT,
    
    -- Duration
    start_date DATE NOT NULL,
    end_date DATE,
    duration_days INTEGER,
    
    -- Refills
    refills_authorized INTEGER DEFAULT 0,
    refills_remaining INTEGER DEFAULT 0,
    
    -- Substitution
    substitution_allowed BOOLEAN DEFAULT true,
    daw_code VARCHAR(10), -- Dispense As Written code
    
    -- Controlled Substance Information
    is_controlled_substance BOOLEAN DEFAULT false,
    schedule VARCHAR(10), -- II, III, IV, V
    dea_number VARCHAR(20),
    pdmp_queried BOOLEAN DEFAULT false,
    pdmp_query_date TIMESTAMP,
    
    -- Pharmacy Information
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    pharmacy_npi VARCHAR(20),
    pharmacy_address_line1 VARCHAR(255),
    pharmacy_address_line2 VARCHAR(255),
    pharmacy_city VARCHAR(100),
    pharmacy_state VARCHAR(50),
    pharmacy_zip VARCHAR(20),
    pharmacy_phone VARCHAR(50),
    
    -- Prescribing Provider Information
    prescribing_provider_id UUID NOT NULL,
    prescribing_provider_npi VARCHAR(20),
    prescribing_provider_name VARCHAR(200),
    
    -- Prescription Status
    prescription_status VARCHAR(20) DEFAULT 'DRAFT',
    
    -- Dates
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_date TIMESTAMP,
    filled_date TIMESTAMP,
    cancellation_date TIMESTAMP,
    expiration_date DATE,
    
    -- Cancellation
    cancellation_reason TEXT,
    cancelled_by UUID,
    
    -- Additional Information
    notes TEXT,
    special_instructions TEXT,
    diagnosis_code VARCHAR(20), -- ICD-10 code for diagnosis
    
    -- Validation Flags
    has_interactions BOOLEAN DEFAULT false,
    has_allergy_warnings BOOLEAN DEFAULT false,
    validation_status VARCHAR(50), -- VALID, WARNINGS, ERRORS
    validation_notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_prescription_type CHECK (prescription_type IN 
        ('ELECTRONIC', 'PAPER', 'PHONE', 'FAX', 'OTHER')),
    CONSTRAINT chk_dosage_form CHECK (dosage_form IN 
        ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'TOPICAL', 'INHALATION', 
         'SUBLINGUAL', 'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER')),
    CONSTRAINT chk_route CHECK (route IN 
        ('ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALATION', 'SUBLINGUAL', 
         'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER')),
    CONSTRAINT chk_prescription_status CHECK (prescription_status IN 
        ('DRAFT', 'PENDING', 'SENT', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED', 
         'EXPIRED', 'REJECTED', 'ON_HOLD')),
    CONSTRAINT chk_schedule CHECK (schedule IS NULL OR schedule IN ('II', 'III', 'IV', 'V')),
    CONSTRAINT chk_medication_code_type CHECK (medication_code_type IS NULL OR medication_code_type IN 
        ('RXNORM', 'NDC', 'OTHER'))
);

-- Indexes for Prescriptions
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON ehr.prescriptions(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON ehr.prescriptions(patient_id, prescription_status);
CREATE INDEX IF NOT EXISTS idx_prescriptions_encounter ON ehr.prescriptions(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_provider ON ehr.prescriptions(prescribing_provider_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_pharmacy ON ehr.prescriptions(pharmacy_id) WHERE pharmacy_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_number ON ehr.prescriptions(prescription_number);
CREATE INDEX IF NOT EXISTS idx_prescriptions_controlled ON ehr.prescriptions(patient_id, is_controlled_substance) WHERE is_controlled_substance = true;
CREATE INDEX IF NOT EXISTS idx_prescriptions_active ON ehr.prescriptions(patient_id, prescription_status) WHERE prescription_status IN ('SENT', 'FILLED', 'PARTIALLY_FILLED');
CREATE INDEX IF NOT EXISTS idx_prescriptions_date_range ON ehr.prescriptions(patient_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_prescriptions_medication_code ON ehr.prescriptions(medication_code) WHERE medication_code IS NOT NULL;

-- Prescription Interactions Table
CREATE TABLE IF NOT EXISTS ehr.prescription_interactions (
    interaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    
    -- Interaction Information
    interacting_medication VARCHAR(500),
    interacting_medication_code VARCHAR(100),
    interaction_type VARCHAR(100),
    severity VARCHAR(50) NOT NULL,
    
    -- Clinical Details
    description TEXT,
    clinical_significance TEXT,
    action_required TEXT,
    management_guidance TEXT,
    
    -- Status
    is_acknowledged BOOLEAN DEFAULT false,
    acknowledged_by UUID,
    acknowledged_date TIMESTAMP,
    override_reason TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_interaction_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_interaction_severity CHECK (severity IN 
        ('CONTRAINDICATED', 'MAJOR', 'MODERATE', 'MINOR', 'UNKNOWN'))
);

-- Indexes for Prescription Interactions
CREATE INDEX IF NOT EXISTS idx_prescription_interactions_prescription ON ehr.prescription_interactions(prescription_id);
CREATE INDEX IF NOT EXISTS idx_prescription_interactions_severity ON ehr.prescription_interactions(prescription_id, severity);
CREATE INDEX IF NOT EXISTS idx_prescription_interactions_acknowledged ON ehr.prescription_interactions(prescription_id, is_acknowledged) WHERE is_acknowledged = false;

-- Prescription Allergy Checks Table
CREATE TABLE IF NOT EXISTS ehr.prescription_allergy_checks (
    check_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    
    -- Allergy Information
    allergen_name VARCHAR(500) NOT NULL,
    allergen_code VARCHAR(100),
    allergen_type VARCHAR(50),
    reaction_type VARCHAR(200),
    severity VARCHAR(50),
    
    -- Action Taken
    action_taken VARCHAR(100), -- OVERRIDDEN, CANCELLED, SUBSTITUTED, MONITORED
    override_reason TEXT,
    override_by UUID,
    override_date TIMESTAMP,
    
    -- Status
    is_acknowledged BOOLEAN DEFAULT false,
    acknowledged_by UUID,
    acknowledged_date TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_allergy_check_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_allergy_severity CHECK (severity IS NULL OR severity IN 
        ('MILD', 'MODERATE', 'SEVERE', 'LIFE_THREATENING')),
    CONSTRAINT chk_action_taken CHECK (action_taken IS NULL OR action_taken IN 
        ('OVERRIDDEN', 'CANCELLED', 'SUBSTITUTED', 'MONITORED', 'NO_ACTION'))
);

-- Indexes for Prescription Allergy Checks
CREATE INDEX IF NOT EXISTS idx_allergy_checks_prescription ON ehr.prescription_allergy_checks(prescription_id);
CREATE INDEX IF NOT EXISTS idx_allergy_checks_severity ON ehr.prescription_allergy_checks(prescription_id, severity);
CREATE INDEX IF NOT EXISTS idx_allergy_checks_acknowledged ON ehr.prescription_allergy_checks(prescription_id, is_acknowledged) WHERE is_acknowledged = false;

-- Prescription History Table
CREATE TABLE IF NOT EXISTS ehr.prescription_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    
    -- Change Information
    change_type VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    changed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Change Details
    previous_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    
    -- Additional Context
    field_name VARCHAR(100),
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_prescription_history_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_prescription_history_change_type CHECK (change_type IN 
        ('CREATED', 'UPDATED', 'STATUS_CHANGED', 'SENT', 'FILLED', 'CANCELLED', 
         'INTERACTION_ADDED', 'ALLERGY_WARNING_ADDED', 'VALIDATED', 'OTHER'))
);

-- Indexes for Prescription History
CREATE INDEX IF NOT EXISTS idx_prescription_history_prescription ON ehr.prescription_history(prescription_id);
CREATE INDEX IF NOT EXISTS idx_prescription_history_date ON ehr.prescription_history(prescription_id, changed_date DESC);
CREATE INDEX IF NOT EXISTS idx_prescription_history_type ON ehr.prescription_history(prescription_id, change_type);

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_prescriptions_updated_at ON ehr.prescriptions;
CREATE TRIGGER update_prescriptions_updated_at
    BEFORE UPDATE ON ehr.prescriptions
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Function to generate prescription number
CREATE OR REPLACE FUNCTION ehr.generate_prescription_number()
RETURNS VARCHAR AS '
DECLARE
    new_number VARCHAR;
BEGIN
    new_number := ''RX'' || TO_CHAR(CURRENT_TIMESTAMP, ''YYYYMMDDHH24MISS'') || 
                  LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, ''0'');
    RETURN new_number;
END;
' LANGUAGE plpgsql;

-- Trigger to auto-generate prescription number if not provided
CREATE OR REPLACE FUNCTION ehr.auto_generate_prescription_number()
RETURNS TRIGGER AS '
BEGIN
    IF NEW.prescription_number IS NULL OR NEW.prescription_number = '''' THEN
        NEW.prescription_number := ehr.generate_prescription_number();
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS auto_prescription_number_trigger ON ehr.prescriptions;
CREATE TRIGGER auto_prescription_number_trigger
    BEFORE INSERT ON ehr.prescriptions
    FOR EACH ROW
    WHEN (NEW.prescription_number IS NULL OR NEW.prescription_number = '')
    EXECUTE FUNCTION ehr.auto_generate_prescription_number();

-- Comments for Phase EHR.6 tables
COMMENT ON TABLE ehr.prescriptions IS 'Electronic prescriptions with medication details, dosage, refills, and status tracking';
COMMENT ON TABLE ehr.prescription_interactions IS 'Drug interaction warnings and checks for prescriptions';
COMMENT ON TABLE ehr.prescription_allergy_checks IS 'Allergy checks and warnings for prescriptions';
COMMENT ON TABLE ehr.prescription_history IS 'Audit trail for all changes to prescriptions';

COMMENT ON COLUMN ehr.prescriptions.prescription_status IS 'Status: DRAFT, PENDING, SENT, FILLED, PARTIALLY_FILLED, CANCELLED, EXPIRED, REJECTED, ON_HOLD';
COMMENT ON COLUMN ehr.prescriptions.medication_code_type IS 'Code type: RXNORM, NDC, OTHER';
COMMENT ON COLUMN ehr.prescriptions.dosage_form IS 'Form: TABLET, CAPSULE, LIQUID, INJECTION, TOPICAL, INHALATION, etc.';
COMMENT ON COLUMN ehr.prescriptions.route IS 'Route: ORAL, IV, IM, SC, TOPICAL, INHALATION, etc.';
COMMENT ON COLUMN ehr.prescriptions.schedule IS 'Controlled substance schedule: II, III, IV, V';
COMMENT ON COLUMN ehr.prescription_interactions.severity IS 'Severity: CONTRAINDICATED, MAJOR, MODERATE, MINOR, UNKNOWN';
COMMENT ON COLUMN ehr.prescription_allergy_checks.action_taken IS 'Action: OVERRIDDEN, CANCELLED, SUBSTITUTED, MONITORED, NO_ACTION';

-- ============================================
-- PHASE EHR.7: PRESCRIPTION REFILLS & ADVANCED FEATURES TABLES
-- ============================================

-- Prescription Refill Requests Table
CREATE TABLE IF NOT EXISTS ehr.prescription_refill_requests (
    refill_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    
    -- Request Information
    request_source VARCHAR(50) NOT NULL,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    requested_by UUID,
    requested_by_name VARCHAR(200),
    
    -- Pharmacy Information
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    pharmacy_npi VARCHAR(20),
    pharmacy_phone VARCHAR(50),
    
    -- Refill Details
    refills_requested INTEGER DEFAULT 1,
    refills_remaining INTEGER,
    last_fill_date DATE,
    days_since_last_fill INTEGER,
    
    -- Request Status
    request_status VARCHAR(20) DEFAULT 'PENDING',
    
    -- Approval Information
    approved_by UUID,
    approved_date TIMESTAMP,
    approval_notes TEXT,
    
    -- Denial Information
    denied_by UUID,
    denied_date TIMESTAMP,
    denial_reason TEXT,
    
    -- Modification Information
    modified_by UUID,
    modified_date TIMESTAMP,
    modification_notes TEXT,
    original_refills_requested INTEGER,
    
    -- Additional Information
    notes TEXT,
    urgency_level VARCHAR(20), -- LOW, MEDIUM, HIGH, URGENT
    
    -- Auto-approval Information
    was_auto_approved BOOLEAN DEFAULT false,
    auto_approval_rule_id UUID,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_refill_request_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_request_source CHECK (request_source IN 
        ('PHARMACY', 'PATIENT', 'PROVIDER', 'SYSTEM', 'OTHER')),
    CONSTRAINT chk_request_status CHECK (request_status IN 
        ('PENDING', 'APPROVED', 'DENIED', 'MODIFIED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_urgency_level CHECK (urgency_level IS NULL OR urgency_level IN 
        ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

-- Indexes for Prescription Refill Requests
CREATE INDEX IF NOT EXISTS idx_refill_requests_prescription ON ehr.prescription_refill_requests(prescription_id);
CREATE INDEX IF NOT EXISTS idx_refill_requests_status ON ehr.prescription_refill_requests(request_status);
CREATE INDEX IF NOT EXISTS idx_refill_requests_pending ON ehr.prescription_refill_requests(request_status) WHERE request_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_refill_requests_date ON ehr.prescription_refill_requests(request_date DESC);
CREATE INDEX IF NOT EXISTS idx_refill_requests_source ON ehr.prescription_refill_requests(request_source);
CREATE INDEX IF NOT EXISTS idx_refill_requests_pharmacy ON ehr.prescription_refill_requests(pharmacy_id) WHERE pharmacy_id IS NOT NULL;

-- Prescription Refills Table
CREATE TABLE IF NOT EXISTS ehr.prescription_refills (
    refill_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    refill_request_id UUID,
    
    -- Refill Identification
    refill_number INTEGER NOT NULL,
    refill_date DATE NOT NULL,
    
    -- Dispensing Information
    quantity_dispensed DECIMAL(10, 2),
    quantity_unit VARCHAR(50),
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    pharmacy_npi VARCHAR(20),
    
    -- Filling Information
    filled_by UUID,
    filled_by_name VARCHAR(200),
    filled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Additional Information
    notes TEXT,
    lot_number VARCHAR(100),
    expiration_date DATE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_refill_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT fk_refill_request FOREIGN KEY (refill_request_id) 
        REFERENCES ehr.prescription_refill_requests(refill_request_id) ON DELETE SET NULL,
    CONSTRAINT chk_refill_number CHECK (refill_number > 0)
);

-- Indexes for Prescription Refills
CREATE INDEX IF NOT EXISTS idx_refills_prescription ON ehr.prescription_refills(prescription_id);
CREATE INDEX IF NOT EXISTS idx_refills_request ON ehr.prescription_refills(refill_request_id) WHERE refill_request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_refills_date ON ehr.prescription_refills(prescription_id, refill_date DESC);
CREATE INDEX IF NOT EXISTS idx_refills_pharmacy ON ehr.prescription_refills(pharmacy_id) WHERE pharmacy_id IS NOT NULL;

-- Auto-Approval Rules Table (for future use)
CREATE TABLE IF NOT EXISTS ehr.refill_auto_approval_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Rule Identification
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    is_active BOOLEAN DEFAULT true,
    
    -- Rule Criteria
    medication_code VARCHAR(100),
    medication_name_pattern VARCHAR(500),
    max_refills_per_request INTEGER,
    max_days_since_last_fill INTEGER,
    min_days_between_refills INTEGER,
    max_total_refills INTEGER,
    exclude_controlled_substances BOOLEAN DEFAULT true,
    
    -- Rule Actions
    auto_approve BOOLEAN DEFAULT true,
    require_provider_review BOOLEAN DEFAULT false,
    notification_required BOOLEAN DEFAULT false,
    
    -- Rule Scope
    applies_to_all_patients BOOLEAN DEFAULT false,
    patient_ids UUID[], -- Array of specific patient IDs
    provider_ids UUID[], -- Array of specific provider IDs
    
    -- Rule Metadata
    created_by UUID,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

-- Indexes for Auto-Approval Rules
CREATE INDEX IF NOT EXISTS idx_auto_approval_rules_active ON ehr.refill_auto_approval_rules(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_auto_approval_rules_medication ON ehr.refill_auto_approval_rules(medication_code) WHERE medication_code IS NOT NULL;

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_refill_requests_updated_at ON ehr.prescription_refill_requests;
CREATE TRIGGER update_refill_requests_updated_at
    BEFORE UPDATE ON ehr.prescription_refill_requests
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_refills_updated_at ON ehr.prescription_refills;
CREATE TRIGGER update_refills_updated_at
    BEFORE UPDATE ON ehr.prescription_refills
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

DROP TRIGGER IF EXISTS update_auto_approval_rules_updated_at ON ehr.refill_auto_approval_rules;
CREATE TRIGGER update_auto_approval_rules_updated_at
    BEFORE UPDATE ON ehr.refill_auto_approval_rules
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Function to calculate days since last fill
CREATE OR REPLACE FUNCTION ehr.calculate_days_since_last_fill(p_prescription_id UUID)
RETURNS INTEGER AS '
DECLARE
    last_fill_date DATE;
BEGIN
    SELECT MAX(refill_date) INTO last_fill_date
    FROM ehr.prescription_refills
    WHERE prescription_id = p_prescription_id;
    IF last_fill_date IS NULL THEN
        SELECT filled_date::DATE INTO last_fill_date
        FROM ehr.prescriptions
        WHERE prescription_id = p_prescription_id AND filled_date IS NOT NULL;
    END IF;
    IF last_fill_date IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN CURRENT_DATE - last_fill_date;
END;
' LANGUAGE plpgsql;

-- Comments for Phase EHR.7 tables
COMMENT ON TABLE ehr.prescription_refill_requests IS 'Prescription refill requests from pharmacies, patients, or providers';
COMMENT ON TABLE ehr.prescription_refills IS 'Actual prescription refills that have been dispensed';
COMMENT ON TABLE ehr.refill_auto_approval_rules IS 'Rules for automatically approving prescription refill requests';

COMMENT ON COLUMN ehr.prescription_refill_requests.request_source IS 'Source: PHARMACY, PATIENT, PROVIDER, SYSTEM, OTHER';
COMMENT ON COLUMN ehr.prescription_refill_requests.request_status IS 'Status: PENDING, APPROVED, DENIED, MODIFIED, COMPLETED, CANCELLED';
COMMENT ON COLUMN ehr.prescription_refill_requests.was_auto_approved IS 'Indicates if this request was automatically approved by a rule';
COMMENT ON COLUMN ehr.prescription_refills.refill_number IS 'Sequential refill number (1, 2, 3, etc.)';

-- ============================================
-- PHASE EHR.8: PATIENT SUMMARY & REPORTING VIEWS
-- ============================================

-- View: Active Patient Problems Summary
CREATE OR REPLACE VIEW ehr.v_patient_problems_active AS
SELECT 
    p.patient_id,
    p.problem_id,
    p.problem_name,
    p.icd10_code,
    p.icd11_code,
    p.snomed_code,
    p.problem_type,
    p.status,
    p.onset_date,
    p.severity,
    p.priority,
    p.documented_date,
    pp.full_name as patient_name,
    pp.mrn
FROM ehr.patient_problems p
JOIN ehr.patients pp ON p.patient_id = pp.patient_id
WHERE p.status IN ('ACTIVE', 'CHRONIC')
ORDER BY p.patient_id, p.priority DESC, p.documented_date DESC;

-- View: Active Patient Medications Summary
CREATE OR REPLACE VIEW ehr.v_patient_medications_active AS
SELECT 
    pp.patient_id,
    pr.prescription_id,
    pr.prescription_number,
    pr.medication_name,
    pr.dosage_strength,
    pr.dosage_unit,
    pr.dosage_form,
    pr.route,
    pr.frequency,
    pr.start_date,
    pr.end_date,
    pr.refills_remaining,
    pr.prescription_status,
    pr.prescribing_provider_id,
    pp.full_name as patient_name,
    pp.mrn
FROM ehr.prescriptions pr
JOIN ehr.patients pp ON pr.patient_id = pp.patient_id
WHERE pr.prescription_status IN ('SENT', 'FILLED', 'PARTIALLY_FILLED')
    AND (pr.end_date IS NULL OR pr.end_date >= CURRENT_DATE)
    AND (pr.refills_remaining > 0 OR pr.refills_remaining IS NULL)
ORDER BY pr.patient_id, pr.start_date DESC;

-- View: Patient Summary Dashboard Data
CREATE OR REPLACE VIEW ehr.v_patient_summary AS
SELECT 
    p.patient_id,
    p.mrn,
    p.full_name as patient_name,
    p.date_of_birth,
    EXTRACT(YEAR FROM AGE(p.date_of_birth)) as age,
    p.gender,
    p.patient_status,
    p.registration_date,
    -- Counts
    (SELECT COUNT(*) FROM ehr.patient_problems pp 
     WHERE pp.patient_id = p.patient_id AND pp.status IN ('ACTIVE', 'CHRONIC')) as active_problems_count,
    (SELECT COUNT(*) FROM ehr.prescriptions pr 
     WHERE pr.patient_id = p.patient_id 
     AND pr.prescription_status IN ('SENT', 'FILLED', 'PARTIALLY_FILLED')
     AND (pr.end_date IS NULL OR pr.end_date >= CURRENT_DATE)) as active_prescriptions_count,
    (SELECT COUNT(*) FROM ehr.allergies a 
     WHERE a.patient_id = p.patient_id AND a.status = 'ACTIVE') as active_allergies_count,
    (SELECT COUNT(*) FROM ehr.clinical_notes cn 
     WHERE cn.patient_id = p.patient_id 
     AND cn.is_current_version = true
     AND cn.note_date >= CURRENT_DATE - INTERVAL '30 days') as recent_notes_count,
    (SELECT COUNT(*) FROM ehr.immunizations i 
     WHERE i.patient_id = p.patient_id 
     AND i.administration_date >= CURRENT_DATE - INTERVAL '12 months') as recent_immunizations_count,
    -- Latest vital signs date
    (SELECT MAX(vs.measurement_date) FROM ehr.vital_signs vs 
     WHERE vs.patient_id = p.patient_id) as latest_vital_signs_date,
    -- Last updated
    GREATEST(
        p.updated_at,
        (SELECT MAX(pp.updated_at) FROM ehr.patient_problems pp WHERE pp.patient_id = p.patient_id),
        (SELECT MAX(pr.updated_at) FROM ehr.prescriptions pr WHERE pr.patient_id = p.patient_id),
        (SELECT MAX(a.updated_at) FROM ehr.allergies a WHERE a.patient_id = p.patient_id),
        (SELECT MAX(cn.updated_at) FROM ehr.clinical_notes cn WHERE cn.patient_id = p.patient_id)
    ) as last_updated
FROM ehr.patients p
WHERE p.patient_status = 'ACTIVE';

-- View: Patient Clinical Activity Summary
CREATE OR REPLACE VIEW ehr.v_patient_clinical_activity AS
SELECT 
    p.patient_id,
    p.mrn,
    p.full_name as patient_name,
    -- Vital signs activity
    (SELECT COUNT(*) FROM ehr.vital_signs vs 
     WHERE vs.patient_id = p.patient_id 
     AND vs.measurement_date >= CURRENT_DATE - INTERVAL '30 days') as vital_signs_count_30d,
    (SELECT COUNT(*) FROM ehr.vital_signs vs 
     WHERE vs.patient_id = p.patient_id 
     AND vs.is_abnormal = true 
     AND vs.measurement_date >= CURRENT_DATE - INTERVAL '30 days') as abnormal_vital_signs_count_30d,
    (SELECT COUNT(*) FROM ehr.vital_signs vs 
     WHERE vs.patient_id = p.patient_id 
     AND vs.is_critical = true 
     AND vs.measurement_date >= CURRENT_DATE - INTERVAL '30 days') as critical_vital_signs_count_30d,
    -- Clinical notes activity
    (SELECT COUNT(*) FROM ehr.clinical_notes cn 
     WHERE cn.patient_id = p.patient_id 
     AND cn.is_current_version = true
     AND cn.note_date >= CURRENT_DATE - INTERVAL '30 days') as notes_count_30d,
    (SELECT COUNT(*) FROM ehr.clinical_notes cn 
     WHERE cn.patient_id = p.patient_id 
     AND cn.is_current_version = true
     AND cn.note_status = 'SIGNED'
     AND cn.note_date >= CURRENT_DATE - INTERVAL '30 days') as signed_notes_count_30d,
    -- Prescription activity
    (SELECT COUNT(*) FROM ehr.prescriptions pr 
     WHERE pr.patient_id = p.patient_id 
     AND pr.created_date >= CURRENT_DATE - INTERVAL '30 days') as prescriptions_created_30d,
    (SELECT COUNT(*) FROM ehr.prescription_refills prf 
     WHERE prf.prescription_id IN (SELECT prescription_id FROM ehr.prescriptions WHERE patient_id = p.patient_id)
     AND prf.refill_date >= CURRENT_DATE - INTERVAL '30 days') as refills_count_30d
FROM ehr.patients p
WHERE p.patient_status = 'ACTIVE';

-- View: Prescription Reporting Summary
CREATE OR REPLACE VIEW ehr.v_prescription_reporting AS
SELECT 
    pr.prescription_id,
    pr.prescription_number,
    pr.patient_id,
    pp.full_name as patient_name,
    pp.mrn,
    pr.medication_name,
    pr.dosage_strength || ' ' || pr.dosage_unit as dosage,
    pr.route,
    pr.frequency,
    pr.start_date,
    pr.end_date,
    pr.prescription_status,
    pr.prescribing_provider_id,
    pr.created_date,
    pr.sent_date,
    pr.filled_date,
    pr.refills_authorized,
    pr.refills_remaining,
    (SELECT COUNT(*) FROM ehr.prescription_refills prf 
     WHERE prf.prescription_id = pr.prescription_id) as refills_dispensed,
    (SELECT COUNT(*) FROM ehr.prescription_refill_requests prr 
     WHERE prr.prescription_id = pr.prescription_id 
     AND prr.request_status = 'PENDING') as pending_refill_requests,
    pr.is_controlled_substance,
    pr.schedule
FROM ehr.prescriptions pr
JOIN ehr.patients pp ON pr.patient_id = pp.patient_id;

-- Comments for Phase EHR.8 views
COMMENT ON VIEW ehr.v_patient_problems_active IS 'Active and chronic patient problems summary for reporting';
COMMENT ON VIEW ehr.v_patient_medications_active IS 'Active patient medications summary for reporting';
COMMENT ON VIEW ehr.v_patient_summary IS 'Comprehensive patient summary dashboard data';
COMMENT ON VIEW ehr.v_patient_clinical_activity IS 'Patient clinical activity metrics for reporting';
COMMENT ON VIEW ehr.v_prescription_reporting IS 'Prescription reporting summary with refill and status information';

-- ============================================
-- PHASE EHR.6: LABORATORY RESULTS MANAGEMENT
-- ============================================

-- Laboratory Orders Table
CREATE TABLE IF NOT EXISTS ehr.lab_orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    organization_id UUID,
    
    -- Order Information
    order_number VARCHAR(100) UNIQUE NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_date TIMESTAMP,
    ordering_provider_id UUID NOT NULL,
    ordering_provider_name VARCHAR(200),
    ordering_facility_id UUID,
    ordering_facility_name VARCHAR(200),
    
    -- Test Information
    test_name VARCHAR(500) NOT NULL,
    loinc_code VARCHAR(20),
    test_category VARCHAR(100),
    test_type VARCHAR(100),
    is_test_panel BOOLEAN DEFAULT false,
    panel_name VARCHAR(200),
    
    -- Clinical Information
    clinical_indication TEXT,
    priority VARCHAR(20) DEFAULT 'ROUTINE',
    special_instructions TEXT,
    fasting_required BOOLEAN DEFAULT false,
    patient_preparation_instructions TEXT,
    
    -- Order Status
    order_status VARCHAR(50) DEFAULT 'PENDING',
    sent_date TIMESTAMP,
    collected_date TIMESTAMP,
    cancelled_date TIMESTAMP,
    cancellation_reason TEXT,
    
    -- Transmission Information
    transmission_method VARCHAR(50),
    transmission_status VARCHAR(50),
    transmission_date TIMESTAMP,
    laboratory_id UUID,
    laboratory_name VARCHAR(200),
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_lab_order_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_order_status CHECK (order_status IN ('PENDING', 'SENT', 'COLLECTED', 'IN_PROCESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_priority CHECK (priority IN ('ROUTINE', 'STAT', 'ASAP', 'TIMED'))
);

-- Indexes for Lab Orders
CREATE INDEX IF NOT EXISTS idx_lab_orders_patient ON ehr.lab_orders(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_orders_encounter ON ehr.lab_orders(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lab_orders_status ON ehr.lab_orders(order_status);
CREATE INDEX IF NOT EXISTS idx_lab_orders_date ON ehr.lab_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_lab_orders_provider ON ehr.lab_orders(ordering_provider_id);
CREATE INDEX IF NOT EXISTS idx_lab_orders_loinc ON ehr.lab_orders(loinc_code) WHERE loinc_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lab_orders_number ON ehr.lab_orders(order_number);

-- Laboratory Results Table
CREATE TABLE IF NOT EXISTS ehr.lab_results (
    result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    encounter_id UUID,
    organization_id UUID,
    
    -- Result Identification
    result_number VARCHAR(100) UNIQUE NOT NULL,
    test_name VARCHAR(500) NOT NULL,
    loinc_code VARCHAR(20) NOT NULL,
    test_category VARCHAR(100),
    test_type VARCHAR(100),
    
    -- Result Values
    result_value VARCHAR(500),
    result_value_numeric DECIMAL(20, 6),
    result_units VARCHAR(50),
    result_type VARCHAR(50) NOT NULL,
    qualitative_result VARCHAR(100),
    quantitative_result DECIMAL(20, 6),
    result_status VARCHAR(50) NOT NULL DEFAULT 'FINAL',
    
    -- Reference Ranges
    reference_range_low DECIMAL(20, 6),
    reference_range_high DECIMAL(20, 6),
    reference_range_units VARCHAR(50),
    reference_range_text VARCHAR(500),
    reference_range_source VARCHAR(100),
    age_specific_range BOOLEAN DEFAULT false,
    gender_specific_range BOOLEAN DEFAULT false,
    
    -- Abnormal Flags
    abnormal_flag VARCHAR(10),
    is_critical_value BOOLEAN DEFAULT false,
    is_delta_check BOOLEAN DEFAULT false,
    is_panic_value BOOLEAN DEFAULT false,
    result_interpretation VARCHAR(100),
    
    -- Temporal Information
    order_date TIMESTAMP,
    specimen_collection_date TIMESTAMP NOT NULL,
    specimen_received_date TIMESTAMP,
    result_date TIMESTAMP NOT NULL,
    result_reported_date TIMESTAMP NOT NULL,
    result_verified_date TIMESTAMP,
    
    -- Specimen Information
    specimen_type VARCHAR(100),
    specimen_source VARCHAR(100),
    specimen_collection_method VARCHAR(200),
    specimen_id VARCHAR(100),
    specimen_volume VARCHAR(50),
    specimen_quality VARCHAR(50),
    
    -- Laboratory Information
    performing_laboratory_name VARCHAR(200) NOT NULL,
    laboratory_id VARCHAR(100),
    laboratory_npi VARCHAR(20),
    laboratory_address_line1 VARCHAR(255),
    laboratory_address_line2 VARCHAR(255),
    laboratory_city VARCHAR(100),
    laboratory_state VARCHAR(50),
    laboratory_zip VARCHAR(20),
    laboratory_phone VARCHAR(50),
    performing_technologist VARCHAR(200),
    reviewing_pathologist VARCHAR(200),
    reviewing_physician VARCHAR(200),
    laboratory_reference_number VARCHAR(100),
    
    -- Result Comments
    laboratory_comments TEXT,
    provider_comments TEXT,
    result_notes TEXT,
    method_used VARCHAR(200),
    
    -- Critical Value Management
    is_critical_value_acknowledged BOOLEAN DEFAULT false,
    critical_value_acknowledged_by UUID,
    critical_value_acknowledged_date TIMESTAMP,
    critical_value_response TEXT,
    
    -- Result Review
    is_reviewed BOOLEAN DEFAULT false,
    reviewed_by UUID,
    reviewed_date TIMESTAMP,
    review_notes TEXT,
    
    -- Result History
    is_corrected BOOLEAN DEFAULT false,
    is_amended BOOLEAN DEFAULT false,
    is_cancelled BOOLEAN DEFAULT false,
    original_result_id UUID,
    correction_reason TEXT,
    amendment_reason TEXT,
    cancellation_reason TEXT,
    correction_date TIMESTAMP,
    amendment_date TIMESTAMP,
    cancellation_date TIMESTAMP,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_lab_result_order FOREIGN KEY (order_id) 
        REFERENCES ehr.lab_orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_original FOREIGN KEY (original_result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE SET NULL,
    CONSTRAINT chk_result_type CHECK (result_type IN ('NUMERIC', 'TEXT', 'CODED', 'STRUCTURED')),
    CONSTRAINT chk_result_status CHECK (result_status IN ('FINAL', 'PRELIMINARY', 'CORRECTED', 'CANCELLED', 'AMENDED')),
    CONSTRAINT chk_abnormal_flag CHECK (abnormal_flag IN ('H', 'L', 'A', 'N', 'C', NULL))
);

-- Indexes for Lab Results
CREATE INDEX IF NOT EXISTS idx_lab_results_order ON ehr.lab_results(order_id);
CREATE INDEX IF NOT EXISTS idx_lab_results_patient ON ehr.lab_results(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_results_encounter ON ehr.lab_results(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lab_results_status ON ehr.lab_results(result_status);
CREATE INDEX IF NOT EXISTS idx_lab_results_date ON ehr.lab_results(result_date);
CREATE INDEX IF NOT EXISTS idx_lab_results_loinc ON ehr.lab_results(loinc_code);
CREATE INDEX IF NOT EXISTS idx_lab_results_critical ON ehr.lab_results(is_critical_value) WHERE is_critical_value = true;
CREATE INDEX IF NOT EXISTS idx_lab_results_reviewed ON ehr.lab_results(is_reviewed) WHERE is_reviewed = false;
CREATE INDEX IF NOT EXISTS idx_lab_results_collection_date ON ehr.lab_results(specimen_collection_date);

-- Laboratory Result History Table
CREATE TABLE IF NOT EXISTS ehr.lab_result_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    changed_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    field_name VARCHAR(100),
    previous_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    notes TEXT,
    
    CONSTRAINT fk_lab_result_history_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT chk_change_type CHECK (change_type IN ('CREATED', 'UPDATED', 'CORRECTED', 'AMENDED', 'CANCELLED', 'REVIEWED', 'ACKNOWLEDGED'))
);

-- Indexes for Lab Result History
CREATE INDEX IF NOT EXISTS idx_lab_result_history_result ON ehr.lab_result_history(result_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_history_date ON ehr.lab_result_history(changed_date);

-- Laboratory Critical Value Alerts Table
CREATE TABLE IF NOT EXISTS ehr.lab_critical_value_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    order_id UUID,
    
    -- Alert Information
    alert_status VARCHAR(50) DEFAULT 'PENDING',
    alert_priority VARCHAR(50) DEFAULT 'HIGH',
    alert_message TEXT NOT NULL,
    
    -- Notification Information
    notified_provider_id UUID,
    notified_provider_name VARCHAR(200),
    notification_method VARCHAR(50),
    notification_sent_date TIMESTAMP,
    notification_delivered BOOLEAN DEFAULT false,
    
    -- Acknowledgment Information
    is_acknowledged BOOLEAN DEFAULT false,
    acknowledged_by UUID,
    acknowledged_date TIMESTAMP,
    acknowledgment_notes TEXT,
    provider_response TEXT,
    
    -- Escalation Information
    escalation_level INTEGER DEFAULT 0,
    escalated_to UUID,
    escalation_date TIMESTAMP,
    escalation_reason TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_critical_alert_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_critical_alert_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_alert_status CHECK (alert_status IN ('PENDING', 'NOTIFIED', 'ACKNOWLEDGED', 'ESCALATED', 'RESOLVED')),
    CONSTRAINT chk_alert_priority CHECK (alert_priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Indexes for Critical Value Alerts
CREATE INDEX IF NOT EXISTS idx_critical_alerts_result ON ehr.lab_critical_value_alerts(result_id);
CREATE INDEX IF NOT EXISTS idx_critical_alerts_patient ON ehr.lab_critical_value_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_critical_alerts_status ON ehr.lab_critical_value_alerts(alert_status) WHERE alert_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_critical_alerts_acknowledged ON ehr.lab_critical_value_alerts(is_acknowledged) WHERE is_acknowledged = false;

-- Apply update triggers
DROP TRIGGER IF EXISTS update_lab_orders_updated_at ON ehr.lab_orders;
CREATE TRIGGER update_lab_orders_updated_at
    BEFORE UPDATE ON ehr.lab_orders
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_lab_results_updated_at
    BEFORE UPDATE ON ehr.lab_results
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_critical_alerts_updated_at
    BEFORE UPDATE ON ehr.lab_critical_value_alerts
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments
COMMENT ON TABLE ehr.lab_orders IS 'Laboratory test orders with comprehensive ordering information';
COMMENT ON TABLE ehr.lab_results IS 'Laboratory test results with comprehensive result data, reference ranges, and clinical context';
COMMENT ON TABLE ehr.lab_result_history IS 'Complete history of all changes to laboratory results';
COMMENT ON TABLE ehr.lab_critical_value_alerts IS 'Critical value alerts and acknowledgment tracking';

-- ============================================
-- LAB RESULT INTERPRETATION AND CLINICAL CONTEXT
-- ============================================

-- Add clinical significance field to lab_results table
ALTER TABLE ehr.lab_results
    ADD COLUMN IF NOT EXISTS clinical_significance VARCHAR(50),
    ADD COLUMN IF NOT EXISTS clinical_significance_level VARCHAR(20),
    ADD COLUMN IF NOT EXISTS interpretation_notes TEXT;

-- Add constraint for clinical significance
ALTER TABLE ehr.lab_results
    DROP CONSTRAINT IF EXISTS chk_clinical_significance;
ALTER TABLE ehr.lab_results
    ADD CONSTRAINT chk_clinical_significance 
    CHECK (clinical_significance IS NULL OR clinical_significance IN ('NORMAL', 'ABNORMAL', 'CRITICAL', 'SIGNIFICANT_CHANGE', 'TRENDING', 'STABLE'));

ALTER TABLE ehr.lab_results
    DROP CONSTRAINT IF EXISTS chk_clinical_significance_level;
ALTER TABLE ehr.lab_results
    ADD CONSTRAINT chk_clinical_significance_level 
    CHECK (clinical_significance_level IS NULL OR clinical_significance_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- Lab Result to Problem/Diagnosis Linking Table
CREATE TABLE IF NOT EXISTS ehr.lab_result_problems (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    problem_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'RELATED', -- RELATED, CONFIRMS, RULES_OUT, MONITORS, OTHER
    link_strength VARCHAR(20) DEFAULT 'MODERATE', -- WEAK, MODERATE, STRONG
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_lab_result_problem_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_problem_problem FOREIGN KEY (problem_id) 
        REFERENCES ehr.patient_problems(problem_id) ON DELETE CASCADE,
    CONSTRAINT chk_link_type CHECK (link_type IN ('RELATED', 'CONFIRMS', 'RULES_OUT', 'MONITORS', 'OTHER')),
    CONSTRAINT chk_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT uk_result_problem UNIQUE (result_id, problem_id)
);

-- Indexes for Lab Result Problems
CREATE INDEX IF NOT EXISTS idx_lab_result_problems_result ON ehr.lab_result_problems(result_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_problems_problem ON ehr.lab_result_problems(problem_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_problems_org ON ehr.lab_result_problems(organization_id) WHERE organization_id IS NOT NULL;

-- Lab Result to Medication Linking Table
CREATE TABLE IF NOT EXISTS ehr.lab_result_medications (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    prescription_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'MONITORS', -- MONITORS, AFFECTED_BY, AFFECTS, CONTRAINDICATED, OTHER
    link_strength VARCHAR(20) DEFAULT 'MODERATE', -- WEAK, MODERATE, STRONG
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_lab_result_medication_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_medication_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_med_link_type CHECK (link_type IN ('MONITORS', 'AFFECTED_BY', 'AFFECTS', 'CONTRAINDICATED', 'OTHER')),
    CONSTRAINT chk_med_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT uk_result_medication UNIQUE (result_id, prescription_id)
);

-- Indexes for Lab Result Medications
CREATE INDEX IF NOT EXISTS idx_lab_result_medications_result ON ehr.lab_result_medications(result_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_medications_prescription ON ehr.lab_result_medications(prescription_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_medications_org ON ehr.lab_result_medications(organization_id) WHERE organization_id IS NOT NULL;

-- Laboratory Result - Clinical Note Links Table
CREATE TABLE IF NOT EXISTS ehr.lab_result_clinical_notes (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    note_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'REFERENCED',
    link_strength VARCHAR(20) DEFAULT 'MODERATE',
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_lab_result_clinical_note_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_clinical_note_note FOREIGN KEY (note_id) 
        REFERENCES ehr.clinical_notes(note_id) ON DELETE CASCADE,
    CONSTRAINT chk_lab_result_clinical_note_link_type CHECK (link_type IN ('REFERENCED', 'DISCUSSED', 'INTERPRETED', 'DOCUMENTED', 'OTHER')),
    CONSTRAINT chk_lab_result_clinical_note_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT uq_lab_result_clinical_note UNIQUE (result_id, note_id)
);

-- Indexes for Lab Result - Clinical Note Links
CREATE INDEX IF NOT EXISTS idx_lab_result_clinical_notes_result ON ehr.lab_result_clinical_notes(result_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_clinical_notes_note ON ehr.lab_result_clinical_notes(note_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_clinical_notes_org ON ehr.lab_result_clinical_notes(organization_id) WHERE organization_id IS NOT NULL;

-- Laboratory Result Values Table (for Test Panels)
-- Stores individual test results within a test panel order
CREATE TABLE IF NOT EXISTS ehr.lab_result_values (
    value_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    order_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    organization_id UUID,
    
    -- Test Information
    test_name VARCHAR(500) NOT NULL,
    loinc_code VARCHAR(20),
    test_category VARCHAR(100),
    test_type VARCHAR(100),
    sequence_number INTEGER DEFAULT 1, -- Order within panel
    
    -- Result Values
    result_value VARCHAR(500),
    result_value_numeric DECIMAL(20, 6),
    result_units VARCHAR(50),
    result_type VARCHAR(50) NOT NULL, -- NUMERIC, TEXT, CODED, STRUCTURED
    qualitative_result VARCHAR(100),
    quantitative_result DECIMAL(20, 6),
    
    -- Reference Ranges
    reference_range_low DECIMAL(20, 6),
    reference_range_high DECIMAL(20, 6),
    reference_range_units VARCHAR(50),
    reference_range_text VARCHAR(500),
    
    -- Abnormal Flags
    abnormal_flag VARCHAR(10), -- H, L, A, N, C
    is_critical_value BOOLEAN DEFAULT false,
    is_panic_value BOOLEAN DEFAULT false,
    result_interpretation VARCHAR(100),
    
    -- Clinical Significance
    clinical_significance VARCHAR(50), -- NORMAL, ABNORMAL, CRITICAL, etc.
    clinical_significance_level VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- Laboratory Information
    performing_laboratory_name VARCHAR(200),
    laboratory_comments TEXT,
    method_used VARCHAR(200),
    
    -- Result Status
    result_status VARCHAR(50) DEFAULT 'FINAL', -- FINAL, PRELIMINARY, CORRECTED, AMENDED
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_lab_result_value_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_value_order FOREIGN KEY (order_id) 
        REFERENCES ehr.lab_orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_result_value_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_result_type CHECK (result_type IN ('NUMERIC', 'TEXT', 'CODED', 'STRUCTURED')),
    CONSTRAINT chk_result_status CHECK (result_status IN ('FINAL', 'PRELIMINARY', 'CORRECTED', 'AMENDED', 'CANCELLED'))
);

-- Indexes for Lab Result Values
CREATE INDEX IF NOT EXISTS idx_lab_result_values_result ON ehr.lab_result_values(result_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_values_order ON ehr.lab_result_values(order_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_values_patient ON ehr.lab_result_values(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_values_org ON ehr.lab_result_values(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lab_result_values_sequence ON ehr.lab_result_values(result_id, sequence_number);

-- Drug-Lab Interaction Alerts Table
CREATE TABLE IF NOT EXISTS ehr.drug_lab_interaction_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID NOT NULL,
    prescription_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    organization_id UUID,
    
    -- Interaction Information
    interaction_type VARCHAR(50) NOT NULL, -- FALSE_POSITIVE, FALSE_NEGATIVE, ALTERED_RESULT, INTERFERENCE, OTHER
    interaction_severity VARCHAR(20) NOT NULL DEFAULT 'MODERATE', -- LOW, MODERATE, HIGH, CRITICAL
    interaction_description TEXT NOT NULL,
    affected_test VARCHAR(500),
    affected_medication VARCHAR(500),
    
    -- Clinical Impact
    clinical_impact TEXT,
    recommended_action TEXT,
    monitoring_required BOOLEAN DEFAULT false,
    monitoring_frequency VARCHAR(100),
    
    -- Alert Status
    alert_status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED, DISMISSED
    acknowledged_by UUID,
    acknowledged_date TIMESTAMP,
    acknowledgment_notes TEXT,
    resolved_by UUID,
    resolved_date TIMESTAMP,
    resolution_notes TEXT,
    
    -- Notification
    notification_sent BOOLEAN DEFAULT false,
    notification_sent_date TIMESTAMP,
    notified_provider_id UUID,
    notified_provider_name VARCHAR(200),
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_drug_lab_alert_result FOREIGN KEY (result_id) 
        REFERENCES ehr.lab_results(result_id) ON DELETE CASCADE,
    CONSTRAINT fk_drug_lab_alert_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT fk_drug_lab_alert_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_interaction_type CHECK (interaction_type IN ('FALSE_POSITIVE', 'FALSE_NEGATIVE', 'ALTERED_RESULT', 'INTERFERENCE', 'OTHER')),
    CONSTRAINT chk_interaction_severity CHECK (interaction_severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (alert_status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED'))
);

-- Indexes for Drug-Lab Interaction Alerts
CREATE INDEX IF NOT EXISTS idx_drug_lab_alerts_result ON ehr.drug_lab_interaction_alerts(result_id);
CREATE INDEX IF NOT EXISTS idx_drug_lab_alerts_prescription ON ehr.drug_lab_interaction_alerts(prescription_id);
CREATE INDEX IF NOT EXISTS idx_drug_lab_alerts_patient ON ehr.drug_lab_interaction_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_drug_lab_alerts_status ON ehr.drug_lab_interaction_alerts(alert_status) WHERE alert_status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_drug_lab_alerts_severity ON ehr.drug_lab_interaction_alerts(interaction_severity) WHERE interaction_severity IN ('HIGH', 'CRITICAL');

-- Update triggers
CREATE TRIGGER update_lab_result_problems_updated_at
    BEFORE UPDATE ON ehr.lab_result_problems
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_lab_result_medications_updated_at
    BEFORE UPDATE ON ehr.lab_result_medications
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_lab_result_clinical_notes_updated_at
    BEFORE UPDATE ON ehr.lab_result_clinical_notes
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_lab_result_values_updated_at
    BEFORE UPDATE ON ehr.lab_result_values
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_drug_lab_alerts_updated_at
    BEFORE UPDATE ON ehr.drug_lab_interaction_alerts
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments
COMMENT ON COLUMN ehr.lab_results.clinical_significance IS 'Clinical significance of the result (NORMAL, ABNORMAL, CRITICAL, etc.)';
COMMENT ON COLUMN ehr.lab_results.clinical_significance_level IS 'Level of clinical significance (LOW, MEDIUM, HIGH, CRITICAL)';
COMMENT ON COLUMN ehr.lab_results.interpretation_notes IS 'Detailed interpretation notes for the result';
COMMENT ON TABLE ehr.lab_result_problems IS 'Links laboratory results to patient problems/diagnoses';
COMMENT ON TABLE ehr.lab_result_medications IS 'Links laboratory results to medications/prescriptions';
COMMENT ON TABLE ehr.lab_result_clinical_notes IS 'Links laboratory results to clinical notes';
COMMENT ON TABLE ehr.lab_result_values IS 'Individual test result values within a test panel order';
COMMENT ON TABLE ehr.drug_lab_interaction_alerts IS 'Alerts for drug-lab interactions that may affect test results';

-- ============================================
-- PHASE EHR.7: IMAGING AND DIAGNOSTIC STUDIES
-- ============================================

-- Imaging Orders Table
CREATE TABLE IF NOT EXISTS ehr.imaging_orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    organization_id UUID,
    
    -- Order Information
    order_number VARCHAR(100) UNIQUE NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ordering_provider_id UUID NOT NULL,
    ordering_provider_name VARCHAR(200),
    ordering_facility_id UUID,
    ordering_facility_name VARCHAR(200),
    
    -- Study Information
    study_type VARCHAR(100) NOT NULL,
    study_modality VARCHAR(50) NOT NULL,
    study_description VARCHAR(500) NOT NULL,
    cpt_code VARCHAR(20) NOT NULL,
    body_part VARCHAR(200) NOT NULL,
    laterality VARCHAR(50),
    specific_anatomical_site VARCHAR(200),
    view_projection VARCHAR(100),
    
    -- Clinical Information
    clinical_indication TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'ROUTINE',
    special_instructions TEXT,
    contrast_required BOOLEAN DEFAULT false,
    contrast_type VARCHAR(100),
    patient_preparation_required BOOLEAN DEFAULT false,
    patient_preparation_instructions TEXT,
    sedation_required BOOLEAN DEFAULT false,
    
    -- Order Status
    order_status VARCHAR(50) DEFAULT 'PENDING',
    sent_date TIMESTAMP,
    scheduled_date TIMESTAMP,
    scheduled_time TIME,
    cancelled_date TIMESTAMP,
    cancellation_reason TEXT,
    no_show BOOLEAN DEFAULT false,
    
    -- Transmission Information
    transmission_method VARCHAR(50),
    transmission_status VARCHAR(50),
    transmission_date TIMESTAMP,
    radiology_facility_id UUID,
    radiology_facility_name VARCHAR(200),
    order_confirmation_received BOOLEAN DEFAULT false,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_imaging_order_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_order_status CHECK (order_status IN ('PENDING', 'SENT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_imaging_priority CHECK (priority IN ('ROUTINE', 'STAT', 'URGENT')),
    CONSTRAINT chk_imaging_modality CHECK (study_modality IN ('XRAY', 'CT', 'MRI', 'ULTRASOUND', 'MAMMOGRAPHY', 'NUCLEAR_MEDICINE', 'PET', 'DEXA', 'FLUOROSCOPY', 'OTHER'))
);

-- Indexes for Imaging Orders
CREATE INDEX IF NOT EXISTS idx_imaging_orders_patient ON ehr.imaging_orders(patient_id);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_encounter ON ehr.imaging_orders(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_imaging_orders_status ON ehr.imaging_orders(order_status);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_date ON ehr.imaging_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_provider ON ehr.imaging_orders(ordering_provider_id);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_modality ON ehr.imaging_orders(study_modality);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_body_part ON ehr.imaging_orders(body_part);
CREATE INDEX IF NOT EXISTS idx_imaging_orders_number ON ehr.imaging_orders(order_number);

-- Imaging Studies Table
CREATE TABLE IF NOT EXISTS ehr.imaging_studies (
    study_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    encounter_id UUID,
    organization_id UUID,
    
    -- Study Identification
    study_number VARCHAR(100) UNIQUE NOT NULL,
    accession_number VARCHAR(100) UNIQUE NOT NULL,
    study_name VARCHAR(500) NOT NULL,
    study_modality VARCHAR(50) NOT NULL,
    cpt_code VARCHAR(20) NOT NULL,
    study_date TIMESTAMP NOT NULL,
    study_completion_date TIMESTAMP NOT NULL,
    study_status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    
    -- Study Details
    body_part_examined VARCHAR(200) NOT NULL,
    laterality VARCHAR(50),
    number_of_images INTEGER,
    number_of_series INTEGER,
    contrast_used BOOLEAN DEFAULT false,
    contrast_type VARCHAR(100),
    technique_protocol VARCHAR(500),
    equipment_used VARCHAR(200),
    equipment_model VARCHAR(200),
    radiation_dose VARCHAR(100),
    study_duration_minutes INTEGER,
    
    -- Radiologist Information
    interpreting_radiologist_name VARCHAR(200),
    interpreting_radiologist_npi VARCHAR(20),
    interpreting_radiologist_specialty VARCHAR(100),
    preliminary_reading_by VARCHAR(200),
    reviewing_radiologist VARCHAR(200),
    report_date TIMESTAMP,
    report_finalized_date TIMESTAMP,
    
    -- Report Content
    clinical_history TEXT,
    technique_description TEXT,
    findings TEXT,
    impression_conclusion TEXT,
    recommendations TEXT,
    urgency_indicator VARCHAR(50),
    
    -- Report Status
    is_preliminary BOOLEAN DEFAULT false,
    is_final BOOLEAN DEFAULT false,
    is_addendum BOOLEAN DEFAULT false,
    is_amended BOOLEAN DEFAULT false,
    is_cancelled BOOLEAN DEFAULT false,
    
    -- Critical Finding Management
    has_critical_findings BOOLEAN DEFAULT false,
    is_critical_finding_acknowledged BOOLEAN DEFAULT false,
    critical_finding_acknowledged_by UUID,
    critical_finding_acknowledged_date TIMESTAMP,
    critical_finding_response TEXT,
    
    -- Report Review
    is_reviewed BOOLEAN DEFAULT false,
    reviewed_by UUID,
    reviewed_date TIMESTAMP,
    review_notes TEXT,
    
    -- Report History
    original_study_id UUID,
    correction_reason TEXT,
    amendment_reason TEXT,
    addendum_reason TEXT,
    cancellation_reason TEXT,
    correction_date TIMESTAMP,
    amendment_date TIMESTAMP,
    addendum_date TIMESTAMP,
    cancellation_date TIMESTAMP,
    
    -- DICOM Information
    dicom_study_instance_uid VARCHAR(200),
    dicom_series_instance_uid VARCHAR(200),
    dicom_storage_location VARCHAR(500),
    pacs_integrated BOOLEAN DEFAULT false,
    images_available BOOLEAN DEFAULT false,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_imaging_study_order FOREIGN KEY (order_id) 
        REFERENCES ehr.imaging_orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_study_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_study_original FOREIGN KEY (original_study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE SET NULL,
    CONSTRAINT chk_imaging_study_status CHECK (study_status IN ('COMPLETED', 'PRELIMINARY', 'FINAL', 'CANCELLED', 'AMENDED'))
);

-- Indexes for Imaging Studies
CREATE INDEX IF NOT EXISTS idx_imaging_studies_order ON ehr.imaging_studies(order_id);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_patient ON ehr.imaging_studies(patient_id);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_encounter ON ehr.imaging_studies(encounter_id) WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_imaging_studies_status ON ehr.imaging_studies(study_status);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_date ON ehr.imaging_studies(study_date);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_modality ON ehr.imaging_studies(study_modality);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_body_part ON ehr.imaging_studies(body_part_examined);
CREATE INDEX IF NOT EXISTS idx_imaging_studies_critical ON ehr.imaging_studies(has_critical_findings) WHERE has_critical_findings = true;
CREATE INDEX IF NOT EXISTS idx_imaging_studies_reviewed ON ehr.imaging_studies(is_reviewed) WHERE is_reviewed = false;
CREATE INDEX IF NOT EXISTS idx_imaging_studies_accession ON ehr.imaging_studies(accession_number);

-- Imaging Study History Table
CREATE TABLE IF NOT EXISTS ehr.imaging_study_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    changed_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    field_name VARCHAR(100),
    previous_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    notes TEXT,
    
    CONSTRAINT fk_imaging_study_history_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_change_type CHECK (change_type IN ('CREATED', 'UPDATED', 'CORRECTED', 'AMENDED', 'ADDENDUM', 'CANCELLED', 'REVIEWED', 'ACKNOWLEDGED'))
);

-- Indexes for Imaging Study History
CREATE INDEX IF NOT EXISTS idx_imaging_study_history_study ON ehr.imaging_study_history(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_history_date ON ehr.imaging_study_history(changed_date);

-- Imaging Critical Finding Alerts Table
CREATE TABLE IF NOT EXISTS ehr.imaging_critical_finding_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    order_id UUID,
    
    -- Alert Information
    alert_status VARCHAR(50) DEFAULT 'PENDING',
    alert_priority VARCHAR(50) DEFAULT 'HIGH',
    alert_message TEXT NOT NULL,
    finding_keywords TEXT,
    
    -- Notification Information
    notified_provider_id UUID,
    notified_provider_name VARCHAR(200),
    notification_method VARCHAR(50),
    notification_sent_date TIMESTAMP,
    notification_delivered BOOLEAN DEFAULT false,
    
    -- Acknowledgment Information
    is_acknowledged BOOLEAN DEFAULT false,
    acknowledged_by UUID,
    acknowledged_date TIMESTAMP,
    acknowledgment_notes TEXT,
    provider_response TEXT,
    
    -- Escalation Information
    escalation_level INTEGER DEFAULT 0,
    escalated_to UUID,
    escalation_date TIMESTAMP,
    escalation_reason TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_imaging_critical_alert_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_critical_alert_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_alert_status CHECK (alert_status IN ('PENDING', 'NOTIFIED', 'ACKNOWLEDGED', 'ESCALATED', 'RESOLVED')),
    CONSTRAINT chk_imaging_alert_priority CHECK (alert_priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Indexes for Critical Finding Alerts
CREATE INDEX IF NOT EXISTS idx_imaging_critical_alerts_study ON ehr.imaging_critical_finding_alerts(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_critical_alerts_patient ON ehr.imaging_critical_finding_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_imaging_critical_alerts_status ON ehr.imaging_critical_finding_alerts(alert_status) WHERE alert_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_imaging_critical_alerts_acknowledged ON ehr.imaging_critical_finding_alerts(is_acknowledged) WHERE is_acknowledged = false;

-- Imaging Image Attachments Table
CREATE TABLE IF NOT EXISTS ehr.imaging_image_attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    
    -- File Information
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500) NOT NULL,
    file_url VARCHAR(500),
    
    -- Image Information
    image_type VARCHAR(50),
    is_dicom BOOLEAN DEFAULT false,
    dicom_series_instance_uid VARCHAR(200),
    dicom_sop_instance_uid VARCHAR(200),
    thumbnail_path VARCHAR(500),
    thumbnail_url VARCHAR(500),
    
    -- Metadata
    description TEXT,
    uploaded_by UUID NOT NULL,
    uploaded_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_imaging_attachment_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT chk_image_type CHECK (image_type IN ('DICOM', 'JPG', 'PNG', 'TIFF', 'PDF', 'OTHER'))
);

-- Indexes for Image Attachments
CREATE INDEX IF NOT EXISTS idx_imaging_attachments_study ON ehr.imaging_image_attachments(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_attachments_type ON ehr.imaging_image_attachments(image_type);

-- Apply update triggers
CREATE TRIGGER update_imaging_orders_updated_at
    BEFORE UPDATE ON ehr.imaging_orders
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_imaging_studies_updated_at
    BEFORE UPDATE ON ehr.imaging_studies
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_imaging_critical_alerts_updated_at
    BEFORE UPDATE ON ehr.imaging_critical_finding_alerts
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

CREATE TRIGGER update_imaging_attachments_updated_at
    BEFORE UPDATE ON ehr.imaging_image_attachments
    FOR EACH ROW
    EXECUTE FUNCTION ehr.update_updated_at_column();

-- Comments
-- Imaging Study Clinical Notes Link Table
CREATE TABLE IF NOT EXISTS ehr.imaging_study_clinical_notes (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    note_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'REFERENCED',
    link_strength VARCHAR(20) DEFAULT 'MODERATE',
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_imaging_study_clinical_note_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_study_clinical_note_note FOREIGN KEY (note_id)
        REFERENCES ehr.clinical_notes(note_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_study_note_link_type CHECK (link_type IN ('REFERENCED', 'DISCUSSED', 'INTERPRETED', 'DOCUMENTED', 'ORDERED', 'OTHER')),
    CONSTRAINT chk_imaging_study_note_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG'))
);

-- Imaging Study Problems Link Table
CREATE TABLE IF NOT EXISTS ehr.imaging_study_problems (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    problem_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'RELATED',
    link_strength VARCHAR(20) DEFAULT 'MODERATE',
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_imaging_study_problem_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_study_problem_problem FOREIGN KEY (problem_id)
        REFERENCES ehr.patient_problems(problem_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_study_problem_link_type CHECK (link_type IN ('RELATED', 'CONFIRMS', 'RULES_OUT', 'MONITORS', 'DIAGNOSES', 'OTHER')),
    CONSTRAINT chk_imaging_study_problem_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG'))
);

-- Imaging Study Medications Link Table
CREATE TABLE IF NOT EXISTS ehr.imaging_study_medications (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id UUID NOT NULL,
    prescription_id UUID NOT NULL,
    organization_id UUID,
    
    -- Link Information
    link_type VARCHAR(50) DEFAULT 'CONTRAST_AGENT',
    link_strength VARCHAR(20) DEFAULT 'MODERATE',
    clinical_relevance TEXT,
    linked_by UUID NOT NULL,
    linked_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_imaging_study_medication_study FOREIGN KEY (study_id) 
        REFERENCES ehr.imaging_studies(study_id) ON DELETE CASCADE,
    CONSTRAINT fk_imaging_study_medication_prescription FOREIGN KEY (prescription_id)
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_imaging_study_medication_link_type CHECK (link_type IN ('CONTRAST_AGENT', 'PRE_MEDICATION', 'POST_MEDICATION', 'RELATED', 'OTHER')),
    CONSTRAINT chk_imaging_study_medication_link_strength CHECK (link_strength IN ('WEAK', 'MODERATE', 'STRONG'))
);

-- Indexes for Imaging Study Links
CREATE INDEX IF NOT EXISTS idx_imaging_study_clinical_notes_study ON ehr.imaging_study_clinical_notes(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_clinical_notes_note ON ehr.imaging_study_clinical_notes(note_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_problems_study ON ehr.imaging_study_problems(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_problems_problem ON ehr.imaging_study_problems(problem_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_medications_study ON ehr.imaging_study_medications(study_id);
CREATE INDEX IF NOT EXISTS idx_imaging_study_medications_prescription ON ehr.imaging_study_medications(prescription_id);

-- Triggers for updated_at
CREATE TRIGGER update_imaging_study_clinical_notes_updated_at
    BEFORE UPDATE ON ehr.imaging_study_clinical_notes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_imaging_study_problems_updated_at
    BEFORE UPDATE ON ehr.imaging_study_problems
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_imaging_study_medications_updated_at
    BEFORE UPDATE ON ehr.imaging_study_medications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ehr.imaging_orders IS 'Imaging study orders with comprehensive ordering information';
COMMENT ON TABLE ehr.imaging_studies IS 'Imaging study results and reports with comprehensive study data';
COMMENT ON TABLE ehr.imaging_study_history IS 'Complete history of all changes to imaging studies';
COMMENT ON TABLE ehr.imaging_critical_finding_alerts IS 'Critical finding alerts and acknowledgment tracking';
COMMENT ON TABLE ehr.imaging_image_attachments IS 'Image attachments for imaging studies (DICOM and non-DICOM)';
COMMENT ON TABLE ehr.imaging_study_clinical_notes IS 'Links imaging studies to clinical notes';
COMMENT ON TABLE ehr.imaging_study_problems IS 'Links imaging studies to problems/diagnoses';
COMMENT ON TABLE ehr.imaging_study_medications IS 'Links imaging studies to medications (e.g., contrast agents)';

-- ============================================
-- PHASE EHR.9: MEDICATION HISTORY INTEGRATION
-- ============================================

-- Medications Table (Current Medication List)
CREATE TABLE IF NOT EXISTS ehr.medications (
    medication_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Medication Identification
    medication_name VARCHAR(500) NOT NULL,
    generic_name VARCHAR(500),
    medication_code VARCHAR(100), -- RxNorm or NDC code
    medication_code_type VARCHAR(20), -- RXNORM, NDC, OTHER
    ndc_code VARCHAR(20), -- National Drug Code
    rxnorm_code VARCHAR(20), -- RxNorm code
    
    -- Dosage Information
    dosage_strength DECIMAL(10, 3),
    dosage_unit VARCHAR(50),
    dosage_form VARCHAR(50), -- TABLET, CAPSULE, LIQUID, etc.
    quantity DECIMAL(10, 2),
    quantity_unit VARCHAR(50),
    
    -- Administration Instructions
    route VARCHAR(50), -- ORAL, IV, IM, TOPICAL, etc.
    frequency VARCHAR(200),
    timing VARCHAR(200), -- e.g., "with meals", "before bedtime"
    instructions TEXT,
    
    -- Prescription Information (if from prescription)
    prescription_id UUID,
    prescribing_provider_id UUID,
    prescribing_provider_name VARCHAR(200),
    prescribing_provider_npi VARCHAR(20),
    prescription_date DATE,
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    refills_authorized INTEGER DEFAULT 0,
    refills_remaining INTEGER DEFAULT 0,
    
    -- Medication Status
    medication_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DISCONTINUED, ON_HOLD, COMPLETED
    status_date DATE,
    status_changed_by UUID,
    
    -- Indication/Reason
    indication TEXT, -- Reason for medication
    diagnosis_code VARCHAR(20), -- ICD-10 code
    
    -- Medication Source
    medication_source VARCHAR(50) NOT NULL, -- PRESCRIPTION, PATIENT_REPORTED, PHARMACY, CLINICAL_DOCUMENTATION, EXTERNAL_IMPORT, OTHER
    
    -- Date Information
    start_date DATE NOT NULL,
    end_date DATE,
    last_filled_date DATE,
    
    -- Additional Information
    notes TEXT,
    special_instructions TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_medication_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE SET NULL,
    CONSTRAINT chk_medication_status CHECK (medication_status IN 
        ('ACTIVE', 'DISCONTINUED', 'ON_HOLD', 'COMPLETED')),
    CONSTRAINT chk_medication_source CHECK (medication_source IN 
        ('PRESCRIPTION', 'PATIENT_REPORTED', 'PHARMACY', 'CLINICAL_DOCUMENTATION', 'EXTERNAL_IMPORT', 'OTHER')),
    CONSTRAINT chk_medication_code_type CHECK (medication_code_type IN ('RXNORM', 'NDC', 'OTHER') OR medication_code_type IS NULL),
    CONSTRAINT chk_dosage_form_med CHECK (dosage_form IN 
        ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'TOPICAL', 'INHALATION', 
         'SUBLINGUAL', 'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER') OR dosage_form IS NULL),
    CONSTRAINT chk_route_med CHECK (route IN 
        ('ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALATION', 'SUBLINGUAL', 
         'BUCCAL', 'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL', 'OTHER') OR route IS NULL)
);

-- Medication History Table
CREATE TABLE IF NOT EXISTS ehr.medication_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    
    -- Historical Medication Information (snapshot at time of change)
    medication_name VARCHAR(500) NOT NULL,
    generic_name VARCHAR(500),
    medication_code VARCHAR(100),
    medication_code_type VARCHAR(20),
    dosage_strength DECIMAL(10, 3),
    dosage_unit VARCHAR(50),
    dosage_form VARCHAR(50),
    route VARCHAR(50),
    frequency VARCHAR(200),
    instructions TEXT,
    
    -- Date Range
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Status Information
    medication_status VARCHAR(20) NOT NULL, -- ACTIVE, DISCONTINUED, ON_HOLD, COMPLETED
    status_date DATE NOT NULL,
    discontinuation_reason TEXT,
    
    -- Source Information
    medication_source VARCHAR(50),
    prescription_id UUID,
    prescribing_provider_name VARCHAR(200),
    
    -- Indication
    indication TEXT,
    diagnosis_code VARCHAR(20),
    
    -- Additional Information
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    
    CONSTRAINT fk_medication_history_medication FOREIGN KEY (medication_id) 
        REFERENCES ehr.medications(medication_id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_history_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_history_status CHECK (medication_status IN 
        ('ACTIVE', 'DISCONTINUED', 'ON_HOLD', 'COMPLETED'))
);

-- Indexes for Medications
CREATE INDEX IF NOT EXISTS idx_medications_patient ON ehr.medications(patient_id);
CREATE INDEX IF NOT EXISTS idx_medications_status ON ehr.medications(patient_id, medication_status);
CREATE INDEX IF NOT EXISTS idx_medications_active ON ehr.medications(patient_id, medication_status) 
    WHERE medication_status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_medications_prescription ON ehr.medications(prescription_id) 
    WHERE prescription_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medications_encounter ON ehr.medications(encounter_id) 
    WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medications_code ON ehr.medications(medication_code) 
    WHERE medication_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medications_rxnorm ON ehr.medications(rxnorm_code) 
    WHERE rxnorm_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medications_start_date ON ehr.medications(patient_id, start_date);

-- Indexes for Medication History
CREATE INDEX IF NOT EXISTS idx_medication_history_medication ON ehr.medication_history(medication_id);
CREATE INDEX IF NOT EXISTS idx_medication_history_patient ON ehr.medication_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_medication_history_dates ON ehr.medication_history(patient_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_medication_history_status ON ehr.medication_history(patient_id, medication_status);

-- Triggers for updated_at
CREATE TRIGGER update_medications_updated_at
    BEFORE UPDATE ON ehr.medications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ehr.medications IS 'Current medication list for patients - comprehensive medication management';
COMMENT ON TABLE ehr.medication_history IS 'Historical medication records - complete medication history tracking';

-- Medication Reconciliation Tables
CREATE TABLE IF NOT EXISTS ehr.medication_reconciliation (
    reconciliation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    encounter_id UUID,
    
    -- Reconciliation Information
    reconciliation_date DATE NOT NULL,
    reconciliation_type VARCHAR(50) NOT NULL, -- ADMISSION, DISCHARGE, TRANSFER, ENCOUNTER, MANUAL
    reconciliation_status VARCHAR(20) DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, CANCELLED
    
    -- Reconciliation Context
    performed_by UUID,
    performed_by_name VARCHAR(200),
    verified_by UUID,
    verified_by_name VARCHAR(200),
    verification_date TIMESTAMP,
    
    -- Reconciliation Summary
    total_medications_before INTEGER DEFAULT 0,
    total_medications_after INTEGER DEFAULT 0,
    medications_added INTEGER DEFAULT 0,
    medications_modified INTEGER DEFAULT 0,
    medications_discontinued INTEGER DEFAULT 0,
    medications_unchanged INTEGER DEFAULT 0,
    
    -- Notes and Documentation
    notes TEXT,
    reconciliation_summary TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_reconciliation_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_reconciliation_type CHECK (reconciliation_type IN 
        ('ADMISSION', 'DISCHARGE', 'TRANSFER', 'ENCOUNTER', 'MANUAL')),
    CONSTRAINT chk_reconciliation_status CHECK (reconciliation_status IN 
        ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Medication Reconciliation Sources Table
CREATE TABLE IF NOT EXISTS ehr.medication_reconciliation_sources (
    source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_id UUID NOT NULL,
    
    -- Source Information
    source_type VARCHAR(50) NOT NULL, -- EHR_CURRENT, EHR_PREVIOUS, PATIENT_REPORTED, PHARMACY, 
                                      -- DISCHARGE_LIST, EXTERNAL_PROVIDER, OTHER_EHR
    source_name VARCHAR(200),
    source_description TEXT,
    
    -- Source Data (stored as JSON for flexibility)
    source_data JSONB,
    
    -- Source Metadata
    source_date DATE,
    source_provider_name VARCHAR(200),
    source_facility_name VARCHAR(200),
    source_contact_info TEXT,
    
    -- Import Information
    imported_at TIMESTAMP,
    imported_by UUID,
    import_method VARCHAR(50), -- MANUAL, API, FILE_UPLOAD, HL7, FHIR
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    
    CONSTRAINT fk_reconciliation_source_reconciliation FOREIGN KEY (reconciliation_id) 
        REFERENCES ehr.medication_reconciliation(reconciliation_id) ON DELETE CASCADE,
    CONSTRAINT chk_source_type CHECK (source_type IN 
        ('EHR_CURRENT', 'EHR_PREVIOUS', 'PATIENT_REPORTED', 'PHARMACY', 
         'DISCHARGE_LIST', 'EXTERNAL_PROVIDER', 'OTHER_EHR'))
);

-- Medication Reconciliation Comparison Table
CREATE TABLE IF NOT EXISTS ehr.medication_reconciliation_comparisons (
    comparison_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_id UUID NOT NULL,
    
    -- Medication Information
    medication_name VARCHAR(500) NOT NULL,
    generic_name VARCHAR(500),
    medication_code VARCHAR(100),
    medication_code_type VARCHAR(20),
    
    -- Comparison Details
    comparison_status VARCHAR(20) NOT NULL, -- NEW, CHANGED, DISCONTINUED, UNCHANGED, CONFLICT
    action_taken VARCHAR(50), -- ADDED, MODIFIED, DISCONTINUED, KEPT, RESOLVED
    
    -- Source Information
    source_medication_id UUID, -- Reference to medication in source
    target_medication_id UUID, -- Reference to medication in target (EHR)
    
    -- Comparison Data (before/after)
    before_dosage_strength DECIMAL(10, 3),
    after_dosage_strength DECIMAL(10, 3),
    before_dosage_unit VARCHAR(50),
    after_dosage_unit VARCHAR(50),
    before_frequency VARCHAR(200),
    after_frequency VARCHAR(200),
    before_route VARCHAR(50),
    after_route VARCHAR(50),
    before_instructions TEXT,
    after_instructions TEXT,
    
    -- Differences
    differences TEXT, -- JSON or text description of differences
    resolution_notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    
    CONSTRAINT fk_comparison_reconciliation FOREIGN KEY (reconciliation_id) 
        REFERENCES ehr.medication_reconciliation(reconciliation_id) ON DELETE CASCADE,
    CONSTRAINT chk_comparison_status CHECK (comparison_status IN 
        ('NEW', 'CHANGED', 'DISCONTINUED', 'UNCHANGED', 'CONFLICT')),
    CONSTRAINT chk_action_taken CHECK (action_taken IN 
        ('ADDED', 'MODIFIED', 'DISCONTINUED', 'KEPT', 'RESOLVED', 'PENDING'))
);

-- Indexes for Medication Reconciliation
CREATE INDEX IF NOT EXISTS idx_reconciliation_patient ON ehr.medication_reconciliation(patient_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_encounter ON ehr.medication_reconciliation(encounter_id) 
    WHERE encounter_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reconciliation_date ON ehr.medication_reconciliation(patient_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_reconciliation_status ON ehr.medication_reconciliation(patient_id, reconciliation_status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_type ON ehr.medication_reconciliation(patient_id, reconciliation_type);

CREATE INDEX IF NOT EXISTS idx_reconciliation_sources_reconciliation ON ehr.medication_reconciliation_sources(reconciliation_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_sources_type ON ehr.medication_reconciliation_sources(reconciliation_id, source_type);

CREATE INDEX IF NOT EXISTS idx_reconciliation_comparisons_reconciliation ON ehr.medication_reconciliation_comparisons(reconciliation_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_comparisons_status ON ehr.medication_reconciliation_comparisons(reconciliation_id, comparison_status);

-- Triggers for updated_at
CREATE TRIGGER update_medication_reconciliation_updated_at
    BEFORE UPDATE ON ehr.medication_reconciliation
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ehr.medication_reconciliation IS 'Medication reconciliation records - tracks medication list comparisons and reconciliations';
COMMENT ON TABLE ehr.medication_reconciliation_sources IS 'Medication reconciliation source data - stores medication lists from different sources';
COMMENT ON TABLE ehr.medication_reconciliation_comparisons IS 'Medication reconciliation comparisons - detailed comparison of medications between sources';

-- ============================================================================
-- PDMP (Prescription Drug Monitoring Program) Query Results
-- ============================================================================

CREATE TABLE IF NOT EXISTS ehr.pdmp_query_results (
    query_result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    
    -- Query Information
    query_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_state VARCHAR(50) NOT NULL,
    query_type VARCHAR(50) DEFAULT 'PATIENT_HISTORY',
    
    -- Provider Information
    querying_provider_id UUID NOT NULL,
    querying_provider_npi VARCHAR(20),
    querying_provider_name VARCHAR(200),
    dea_number VARCHAR(20),
    
    -- Query Status
    query_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    query_success BOOLEAN DEFAULT false,
    error_message TEXT,
    
    -- Query Results Summary
    total_prescriptions INTEGER,
    total_pharmacies INTEGER,
    total_prescribers INTEGER,
    date_range_start DATE,
    date_range_end DATE,
    has_controlled_substances BOOLEAN DEFAULT false,
    risk_score INTEGER, -- 0-100 risk score
    risk_level VARCHAR(20), -- LOW, MODERATE, HIGH, CRITICAL
    
    -- Detailed Results (stored as JSONB)
    prescription_history JSONB,
    pharmacy_list JSONB,
    prescriber_list JSONB,
    raw_response JSONB, -- Store full PDMP response
    
    -- Flags and Warnings
    has_duplicate_prescriptions BOOLEAN DEFAULT false,
    has_overlapping_prescriptions BOOLEAN DEFAULT false,
    has_early_refills BOOLEAN DEFAULT false,
    has_multiple_prescribers BOOLEAN DEFAULT false,
    has_multiple_pharmacies BOOLEAN DEFAULT false,
    warnings TEXT,
    
    -- Documentation
    query_reason TEXT,
    clinical_notes TEXT,
    action_taken TEXT,
    
    -- External PDMP System Information
    pdmp_system_name VARCHAR(100),
    pdmp_system_id VARCHAR(100),
    pdmp_query_id VARCHAR(100),
    pdmp_response_id VARCHAR(100),
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_pdmp_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT fk_pdmp_patient FOREIGN KEY (patient_id) 
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_query_type CHECK (query_type IN 
        ('PATIENT_HISTORY', 'PRESCRIPTION_CHECK', 'REFILL_CHECK', 'COMPLIANCE_CHECK', 'INVESTIGATION')),
    CONSTRAINT chk_query_status CHECK (query_status IN 
        ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'TIMEOUT', 'CANCELLED')),
    CONSTRAINT chk_risk_level CHECK (risk_level IN 
        ('LOW', 'MODERATE', 'HIGH', 'CRITICAL') OR risk_level IS NULL)
);

-- Indexes for PDMP Query Results
CREATE INDEX IF NOT EXISTS idx_pdmp_prescription ON ehr.pdmp_query_results(prescription_id);
CREATE INDEX IF NOT EXISTS idx_pdmp_patient ON ehr.pdmp_query_results(patient_id);
CREATE INDEX IF NOT EXISTS idx_pdmp_query_date ON ehr.pdmp_query_results(patient_id, query_date DESC);
CREATE INDEX IF NOT EXISTS idx_pdmp_provider ON ehr.pdmp_query_results(querying_provider_id);
CREATE INDEX IF NOT EXISTS idx_pdmp_state ON ehr.pdmp_query_results(query_state);
CREATE INDEX IF NOT EXISTS idx_pdmp_status ON ehr.pdmp_query_results(query_status);
CREATE INDEX IF NOT EXISTS idx_pdmp_risk_level ON ehr.pdmp_query_results(risk_level) 
    WHERE risk_level IN ('HIGH', 'CRITICAL');
CREATE INDEX IF NOT EXISTS idx_pdmp_success ON ehr.pdmp_query_results(prescription_id, query_success) 
    WHERE query_success = true;

-- Trigger for updated_at
CREATE TRIGGER update_pdmp_query_results_updated_at
    BEFORE UPDATE ON ehr.pdmp_query_results
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ehr.pdmp_query_results IS 'PDMP (Prescription Drug Monitoring Program) query results - stores controlled substance history queries and results';
COMMENT ON COLUMN ehr.pdmp_query_results.query_state IS 'State where PDMP query was performed';
COMMENT ON COLUMN ehr.pdmp_query_results.query_type IS 'Type of query: PATIENT_HISTORY, PRESCRIPTION_CHECK, REFILL_CHECK, COMPLIANCE_CHECK, INVESTIGATION';
COMMENT ON COLUMN ehr.pdmp_query_results.query_status IS 'Status of query: PENDING, IN_PROGRESS, COMPLETED, FAILED, TIMEOUT, CANCELLED';
COMMENT ON COLUMN ehr.pdmp_query_results.risk_score IS 'Risk score from 0-100 calculated based on prescription patterns';
COMMENT ON COLUMN ehr.pdmp_query_results.risk_level IS 'Risk level: LOW, MODERATE, HIGH, CRITICAL';
COMMENT ON COLUMN ehr.pdmp_query_results.prescription_history IS 'JSON array of prescription history items from PDMP';
COMMENT ON COLUMN ehr.pdmp_query_results.pharmacy_list IS 'JSON array of pharmacy information';
COMMENT ON COLUMN ehr.pdmp_query_results.prescriber_list IS 'JSON array of prescriber information';
COMMENT ON COLUMN ehr.pdmp_query_results.raw_response IS 'Full raw response from PDMP system for audit purposes';

-- ============================================================================
-- E-Prescribing Network Integration
-- ============================================================================

-- Pharmacy Networks Table
CREATE TABLE IF NOT EXISTS ehr.pharmacy_networks (
    network_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    
    -- Network Identification
    network_name VARCHAR(100) NOT NULL,
    network_code VARCHAR(50),
    network_type VARCHAR(50) DEFAULT 'E_PRESCRIBING',
    
    -- Network Configuration
    api_endpoint VARCHAR(500),
    api_key VARCHAR(500), -- Encrypted API key
    username VARCHAR(200),
    password VARCHAR(500), -- Encrypted password
    certificate_path VARCHAR(500),
    environment VARCHAR(50) DEFAULT 'PRODUCTION',
    
    -- Network Status
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    last_connection_test TIMESTAMP,
    connection_status VARCHAR(50),
    last_error_message TEXT,
    
    -- Network Capabilities
    supports_prescription_transmission BOOLEAN DEFAULT true,
    supports_fill_status_updates BOOLEAN DEFAULT true,
    supports_medication_history BOOLEAN DEFAULT false,
    supports_benefits_information BOOLEAN DEFAULT false,
    supports_prior_authorization BOOLEAN DEFAULT false,
    
    -- Network Settings (stored as JSONB)
    network_settings JSONB,
    
    -- Statistics
    total_transmissions BIGINT DEFAULT 0,
    successful_transmissions BIGINT DEFAULT 0,
    failed_transmissions BIGINT DEFAULT 0,
    last_transmission_date TIMESTAMP,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT chk_network_type CHECK (network_type IN 
        ('E_PRESCRIBING', 'DIRECT_API', 'HL7', 'FHIR', 'FAX', 'OTHER')),
    CONSTRAINT chk_environment CHECK (environment IN 
        ('PRODUCTION', 'STAGING', 'TEST', 'DEVELOPMENT')),
    CONSTRAINT chk_connection_status CHECK (connection_status IN 
        ('CONNECTED', 'DISCONNECTED', 'ERROR', 'TESTING', 'UNKNOWN') OR connection_status IS NULL)
);

-- Prescription Transmissions Table
CREATE TABLE IF NOT EXISTS ehr.prescription_transmissions (
    transmission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL,
    
    -- Transmission Information
    transmission_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transmission_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transmission_method VARCHAR(50) DEFAULT 'E_PRESCRIBING',
    
    -- Network Information
    network_name VARCHAR(100),
    network_id VARCHAR(100),
    network_transaction_id VARCHAR(200),
    
    -- Pharmacy Information
    pharmacy_id UUID,
    pharmacy_name VARCHAR(200),
    pharmacy_npi VARCHAR(20),
    pharmacy_dea VARCHAR(20),
    pharmacy_address_line1 VARCHAR(255),
    pharmacy_address_line2 VARCHAR(255),
    pharmacy_city VARCHAR(100),
    pharmacy_state VARCHAR(50),
    pharmacy_zip VARCHAR(20),
    pharmacy_phone VARCHAR(50),
    pharmacy_fax VARCHAR(50),
    
    -- Transmission Results
    transmission_success BOOLEAN DEFAULT false,
    confirmation_received BOOLEAN DEFAULT false,
    confirmation_date TIMESTAMP,
    confirmation_message TEXT,
    error_message TEXT,
    error_code VARCHAR(50),
    retry_count INTEGER DEFAULT 0,
    last_retry_date TIMESTAMP,
    max_retries INTEGER DEFAULT 3,
    
    -- Network Response Data (stored as JSONB)
    network_response JSONB,
    transmission_payload JSONB,
    
    -- Fill Status Information
    fill_status VARCHAR(50),
    fill_status_date TIMESTAMP,
    fill_status_message TEXT,
    filled_date TIMESTAMP,
    picked_up_date TIMESTAMP,
    cancelled_by_pharmacy BOOLEAN DEFAULT false,
    cancellation_reason TEXT,
    
    -- Provider Information
    transmitted_by UUID NOT NULL,
    transmitted_by_name VARCHAR(200),
    transmitted_by_npi VARCHAR(20),
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT fk_transmission_prescription FOREIGN KEY (prescription_id) 
        REFERENCES ehr.prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT chk_transmission_status CHECK (transmission_status IN 
        ('PENDING', 'IN_PROGRESS', 'SENT', 'CONFIRMED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_transmission_method CHECK (transmission_method IN 
        ('E_PRESCRIBING', 'FAX', 'PHONE', 'PAPER', 'DIRECT_MESSAGE', 'API')),
    CONSTRAINT chk_fill_status CHECK (fill_status IN 
        ('PENDING', 'IN_PROGRESS', 'FILLED', 'PARTIALLY_FILLED', 'PICKED_UP', 
         'CANCELLED', 'ON_HOLD', 'OUT_OF_STOCK', 'REJECTED', 'EXPIRED') OR fill_status IS NULL)
);

-- Indexes for Pharmacy Networks
CREATE INDEX IF NOT EXISTS idx_pharmacy_networks_org ON ehr.pharmacy_networks(organization_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_networks_active ON ehr.pharmacy_networks(is_active) 
    WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_pharmacy_networks_default ON ehr.pharmacy_networks(is_default) 
    WHERE is_default = true AND is_active = true;
CREATE INDEX IF NOT EXISTS idx_pharmacy_networks_name ON ehr.pharmacy_networks(network_name) 
    WHERE is_active = true;

-- Indexes for Prescription Transmissions
CREATE INDEX IF NOT EXISTS idx_transmissions_prescription ON ehr.prescription_transmissions(prescription_id);
CREATE INDEX IF NOT EXISTS idx_transmissions_date ON ehr.prescription_transmissions(prescription_id, transmission_date DESC);
CREATE INDEX IF NOT EXISTS idx_transmissions_status ON ehr.prescription_transmissions(transmission_status);
CREATE INDEX IF NOT EXISTS idx_transmissions_success ON ehr.prescription_transmissions(prescription_id, transmission_success) 
    WHERE transmission_success = true;
CREATE INDEX IF NOT EXISTS idx_transmissions_network_tx_id ON ehr.prescription_transmissions(network_transaction_id) 
    WHERE network_transaction_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transmissions_pharmacy_npi ON ehr.prescription_transmissions(pharmacy_npi);
CREATE INDEX IF NOT EXISTS idx_transmissions_network_name ON ehr.prescription_transmissions(network_name);
CREATE INDEX IF NOT EXISTS idx_transmissions_fill_status ON ehr.prescription_transmissions(prescription_id, fill_status) 
    WHERE fill_status IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transmissions_pending_retry ON ehr.prescription_transmissions(transmission_status, retry_count) 
    WHERE transmission_status IN ('PENDING', 'FAILED') AND retry_count < max_retries;

-- Triggers for updated_at
CREATE TRIGGER update_pharmacy_networks_updated_at
    BEFORE UPDATE ON ehr.pharmacy_networks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_prescription_transmissions_updated_at
    BEFORE UPDATE ON ehr.prescription_transmissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ehr.pharmacy_networks IS 'Pharmacy network configurations - stores e-prescribing network settings (Surescripts, etc.)';
COMMENT ON TABLE ehr.prescription_transmissions IS 'Prescription transmission records - tracks e-prescribing transmissions to pharmacies via networks';
COMMENT ON COLUMN ehr.pharmacy_networks.network_type IS 'Type of network: E_PRESCRIBING, DIRECT_API, HL7, FHIR, FAX, OTHER';
COMMENT ON COLUMN ehr.pharmacy_networks.environment IS 'Environment: PRODUCTION, STAGING, TEST, DEVELOPMENT';
COMMENT ON COLUMN ehr.prescription_transmissions.transmission_status IS 'Status: PENDING, IN_PROGRESS, SENT, CONFIRMED, FAILED, CANCELLED, EXPIRED';
COMMENT ON COLUMN ehr.prescription_transmissions.transmission_method IS 'Method: E_PRESCRIBING, FAX, PHONE, PAPER, DIRECT_MESSAGE, API';
COMMENT ON COLUMN ehr.prescription_transmissions.fill_status IS 'Fill status from pharmacy: PENDING, IN_PROGRESS, FILLED, PARTIALLY_FILLED, PICKED_UP, CANCELLED, ON_HOLD, OUT_OF_STOCK, REJECTED, EXPIRED';
COMMENT ON COLUMN ehr.prescription_transmissions.network_transaction_id IS 'Transaction ID from e-prescribing network for tracking';