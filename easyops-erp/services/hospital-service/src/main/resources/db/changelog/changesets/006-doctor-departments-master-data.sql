--liquibase formatted sql

--changeset hospital-service:006-doctor-departments-master-data
--comment: Phase 1.1 - Insert master data for common doctor departments/specialties

-- Insert common medical departments
INSERT INTO hospital.doctor_departments (department_name, status, created_by)
VALUES
    ('Cardiology', 'ACTIVE', 'system'),
    ('Orthopedics', 'ACTIVE', 'system'),
    ('Neurology', 'ACTIVE', 'system'),
    ('General Medicine', 'ACTIVE', 'system'),
    ('Pediatrics', 'ACTIVE', 'system'),
    ('Gynecology', 'ACTIVE', 'system'),
    ('Obstetrics', 'ACTIVE', 'system'),
    ('Dermatology', 'ACTIVE', 'system'),
    ('Ophthalmology', 'ACTIVE', 'system'),
    ('ENT (Ear, Nose, Throat)', 'ACTIVE', 'system'),
    ('Urology', 'ACTIVE', 'system'),
    ('Gastroenterology', 'ACTIVE', 'system'),
    ('Pulmonology', 'ACTIVE', 'system'),
    ('Endocrinology', 'ACTIVE', 'system'),
    ('Nephrology', 'ACTIVE', 'system'),
    ('Psychiatry', 'ACTIVE', 'system'),
    ('Oncology', 'ACTIVE', 'system'),
    ('Hematology', 'ACTIVE', 'system'),
    ('Rheumatology', 'ACTIVE', 'system'),
    ('General Surgery', 'ACTIVE', 'system'),
    ('Plastic Surgery', 'ACTIVE', 'system'),
    ('Neurosurgery', 'ACTIVE', 'system'),
    ('Cardiac Surgery', 'ACTIVE', 'system'),
    ('Orthopedic Surgery', 'ACTIVE', 'system'),
    ('Anesthesiology', 'ACTIVE', 'system'),
    ('Radiology', 'ACTIVE', 'system'),
    ('Pathology', 'ACTIVE', 'system'),
    ('Emergency Medicine', 'ACTIVE', 'system'),
    ('Intensive Care Unit (ICU)', 'ACTIVE', 'system'),
    ('Physical Medicine and Rehabilitation', 'ACTIVE', 'system')
ON CONFLICT (department_name) DO NOTHING;
