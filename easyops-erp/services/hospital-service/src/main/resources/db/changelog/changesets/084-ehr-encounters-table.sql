--liquibase formatted sql

--changeset easyops:084-ehr-encounters-table
--comment: Create ehr.encounters for JPA Encounter (IPD active inpatients, encounter APIs). Table was absent from initial hospital.sql while entity and APIs existed.

CREATE TABLE IF NOT EXISTS ehr.encounters (
    encounter_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    encounter_number VARCHAR(50) NOT NULL,
    encounter_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    start_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_date DATE,
    end_time TIME,
    admission_date DATE,
    admission_time TIME,
    discharge_date DATE,
    discharge_time TIME,
    location_id UUID,
    department_id UUID,
    room_number VARCHAR(50),
    bed_number VARCHAR(50),
    attending_physician_id UUID,
    admitting_physician_id UUID,
    primary_care_provider_id UUID,
    referring_physician_id UUID,
    chief_complaint TEXT,
    admission_diagnosis TEXT,
    primary_diagnosis TEXT,
    secondary_diagnoses TEXT[],
    discharge_diagnosis TEXT,
    discharge_disposition VARCHAR(100),
    discharge_instructions TEXT,
    visit_reason TEXT,
    visit_type VARCHAR(50),
    service_type VARCHAR(50),
    insurance_provider_id UUID,
    insurance_policy_number VARCHAR(100),
    authorization_number VARCHAR(100),
    billing_status VARCHAR(50),
    notes TEXT,
    special_instructions TEXT,
    is_emergency BOOLEAN DEFAULT FALSE,
    is_readmission BOOLEAN DEFAULT FALSE,
    readmission_reason TEXT,
    length_of_stay_days INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_encounters_encounter_number UNIQUE (encounter_number),
    CONSTRAINT fk_encounters_patient FOREIGN KEY (patient_id)
        REFERENCES ehr.patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_encounters_org_dates
    ON ehr.encounters(organization_id, start_date DESC, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_encounters_org_type_status_dates
    ON ehr.encounters(organization_id, encounter_type, status, start_date DESC, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_encounters_org_attending_dates
    ON ehr.encounters(organization_id, attending_physician_id, start_date DESC, start_time DESC)
    WHERE attending_physician_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_encounters_patient_dates
    ON ehr.encounters(patient_id, start_date DESC, start_time DESC);
