--liquibase formatted sql

--changeset hospital-service:005-doctor-management-schema
--comment: Phase 1.1 - Create Hospital Doctor Management schema (Doctor Departments and Doctors tables)

-- Create Hospital schema (if not exists)
CREATE SCHEMA IF NOT EXISTS hospital;

-- Set search path
SET search_path TO hospital, public;

-- =============================================
-- 1. DOCTOR DEPARTMENT MASTER TABLE
-- =============================================
CREATE TABLE hospital.doctor_departments (
    department_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_name VARCHAR(100) NOT NULL UNIQUE,
    general_visit_amount DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_doctor_departments_status ON hospital.doctor_departments(status);
CREATE INDEX idx_doctor_departments_name ON hospital.doctor_departments(department_name);

COMMENT ON TABLE hospital.doctor_departments IS 'Master table for doctor departments/specialties';
COMMENT ON COLUMN hospital.doctor_departments.department_id IS 'Unique identifier for the department';
COMMENT ON COLUMN hospital.doctor_departments.department_name IS 'Name of the department (e.g., Cardiology, Orthopedics)';
COMMENT ON COLUMN hospital.doctor_departments.general_visit_amount IS 'Default visit fee for the department';
COMMENT ON COLUMN hospital.doctor_departments.status IS 'Department status: ACTIVE or INACTIVE';

-- =============================================
-- 2. DOCTOR MASTER TABLE
-- =============================================
CREATE TABLE hospital.doctors (
    doctor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_code VARCHAR(50) NOT NULL UNIQUE, -- Auto-generated: [Dept][Name][Serial]
    doctor_name VARCHAR(100) NOT NULL,
    department_id UUID NOT NULL REFERENCES hospital.doctor_departments(department_id),
    doctor_type VARCHAR(50) NOT NULL, -- CONSULTANT, RESIDENT, INTERN, etc.
    indoor_outdoor_status VARCHAR(20) NOT NULL, -- INDOOR, OUTDOOR
    degree VARCHAR(200),
    speciality VARCHAR(200),
    gender VARCHAR(20),
    birth_date DATE,
    registration_date DATE NOT NULL,
    bmdc_registration_number VARCHAR(50), -- Bangladesh Medical & Dental Council
    phone_number VARCHAR(20),
    email VARCHAR(100) UNIQUE,
    present_address TEXT,
    district VARCHAR(100),
    thana VARCHAR(100),
    area VARCHAR(100),
    chamber_room VARCHAR(100),
    visit_fee_new DECIMAL(10,2),
    visit_fee_old DECIMAL(10,2),
    take_commission BOOLEAN DEFAULT FALSE,
    patients_per_day INTEGER,
    serial_start_from INTEGER DEFAULT 1,
    number_of_days_can_appointment INTEGER,
    number_of_appointments_from_web INTEGER,
    number_of_appointments_from_mobile INTEGER,
    sms_enabled BOOLEAN DEFAULT FALSE,
    prescription_status VARCHAR(50), -- ACTIVE, INACTIVE
    availability_status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, NOT_AVAILABLE
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes for doctors table
CREATE INDEX idx_doctors_department ON hospital.doctors(department_id);
CREATE INDEX idx_doctors_code ON hospital.doctors(doctor_code);
CREATE INDEX idx_doctors_name ON hospital.doctors(doctor_name);
CREATE INDEX idx_doctors_status ON hospital.doctors(is_active, availability_status);
CREATE INDEX idx_doctors_type ON hospital.doctors(doctor_type);
CREATE INDEX idx_doctors_indoor_outdoor ON hospital.doctors(indoor_outdoor_status);
CREATE INDEX idx_doctors_bmdc ON hospital.doctors(bmdc_registration_number);
CREATE INDEX idx_doctors_email ON hospital.doctors(email);

COMMENT ON TABLE hospital.doctors IS 'Master table for doctors/physicians';
COMMENT ON COLUMN hospital.doctors.doctor_id IS 'Unique identifier for the doctor';
COMMENT ON COLUMN hospital.doctors.doctor_code IS 'Auto-generated doctor code: [Dept Initial][Name Initial][Serial]';
COMMENT ON COLUMN hospital.doctors.doctor_name IS 'Full name of the doctor';
COMMENT ON COLUMN hospital.doctors.department_id IS 'Foreign key to doctor_departments table';
COMMENT ON COLUMN hospital.doctors.doctor_type IS 'Type of doctor: CONSULTANT, RESIDENT, INTERN, etc.';
COMMENT ON COLUMN hospital.doctors.indoor_outdoor_status IS 'INDOOR or OUTDOOR doctor status';
COMMENT ON COLUMN hospital.doctors.bmdc_registration_number IS 'Bangladesh Medical & Dental Council registration number';
COMMENT ON COLUMN hospital.doctors.visit_fee_new IS 'Visit fee for new patients';
COMMENT ON COLUMN hospital.doctors.visit_fee_old IS 'Visit fee for returning patients';
COMMENT ON COLUMN hospital.doctors.prescription_status IS 'Prescription status: ACTIVE or INACTIVE';
COMMENT ON COLUMN hospital.doctors.availability_status IS 'Current availability: AVAILABLE or NOT_AVAILABLE';
