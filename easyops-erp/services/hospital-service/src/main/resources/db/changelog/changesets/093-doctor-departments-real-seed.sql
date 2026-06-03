--liquibase formatted sql

--changeset hospital-service:093-doctor-departments-real-seed
--comment: Replace dummy/default department seed with real department master data from DrDept.xlsx

CREATE TEMP TABLE _093_doctor_department_import (
    department_name TEXT NOT NULL,
    general_visit_amount NUMERIC NOT NULL,
    status TEXT NOT NULL,
    created_by TEXT NOT NULL
);

INSERT INTO _093_doctor_department_import (department_name, general_visit_amount, status, created_by) VALUES
    ('Allergy & Asthma', 500.00, 'ACTIVE', 'system-import'),
    ('Aneasthesia', 500.00, 'ACTIVE', 'system-import'),
    ('Anesthesiology', 500.00, 'ACTIVE', 'system-import'),
    ('Burn & Plastic Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('Cardiology', 500.00, 'ACTIVE', 'system-import'),
    ('Chest & Medicine', 0.00, 'ACTIVE', 'system-import'),
    ('Child Specialist', 0.00, 'ACTIVE', 'system-import'),
    ('Consultant', 500.00, 'ACTIVE', 'system-import'),
    ('Dental', 500.00, 'ACTIVE', 'system-import'),
    ('Dermatology(Skin &VD)', 500.00, 'ACTIVE', 'system-import'),
    ('Diabetes & Endocrinology', 500.00, 'ACTIVE', 'system-import'),
    ('ENODOERINOLOGY', 300.00, 'ACTIVE', 'system-import'),
    ('ENT', 500.00, 'ACTIVE', 'system-import'),
    ('Emergency', 200.00, 'ACTIVE', 'system-import'),
    ('Eye', 0.00, 'ACTIVE', 'system-import'),
    ('Famaly Physician', 500.00, 'ACTIVE', 'system-import'),
    ('Food & Nutrition', 500.00, 'ACTIVE', 'system-import'),
    ('Gastroenterology', 500.00, 'ACTIVE', 'system-import'),
    ('General', 500.00, 'ACTIVE', 'system-import'),
    ('General Laparascopic  & Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('General Physician', 500.00, 'ACTIVE', 'system-import'),
    ('General Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('Gasstroenterology', 500.00, 'ACTIVE', 'system-import'),
    ('Gynae & Obs', 500.00, 'ACTIVE', 'system-import'),
    ('Haematology', 500.00, 'ACTIVE', 'system-import'),
    ('Hepatology', 500.00, 'ACTIVE', 'system-import'),
    ('Histopathology', 0.00, 'ACTIVE', 'system-import'),
    ('Hospital, Clinic & Others', 500.00, 'ACTIVE', 'system-import'),
    ('Internal Medicine', 500.00, 'ACTIVE', 'system-import'),
    ('Marketing', 0.00, 'ACTIVE', 'system-import'),
    ('Medical Officer', 500.00, 'ACTIVE', 'system-import'),
    ('Medicine', 500.00, 'ACTIVE', 'system-import'),
    ('Medicine & Nephrology', 0.00, 'ACTIVE', 'system-import'),
    ('Medicine Allergy Asthma & Chest', 500.00, 'ACTIVE', 'system-import'),
    ('Nephrology & Medicine', 500.00, 'ACTIVE', 'system-import'),
    ('Neuro Medicine', 500.00, 'ACTIVE', 'system-import'),
    ('Neuro Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('ORAL & MAXILLOFACIAL SURGERY', 500.00, 'ACTIVE', 'system-import'),
    ('Oncology', 500.00, 'ACTIVE', 'system-import'),
    ('Ophthalmology', 500.00, 'ACTIVE', 'system-import'),
    ('Orthopedics & Spine Specialist', 500.00, 'ACTIVE', 'system-import'),
    ('Orthopedics Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('Pathology', 500.00, 'ACTIVE', 'system-import'),
    ('Pediatric', 500.00, 'ACTIVE', 'system-import'),
    ('Pediatric Nephrology', 500.00, 'ACTIVE', 'system-import'),
    ('Pediatric Surgery & Urology', 500.00, 'ACTIVE', 'system-import'),
    ('Physical Medicine', 500.00, 'ACTIVE', 'system-import'),
    ('Physiotherapy', 500.00, 'ACTIVE', 'system-import'),
    ('Psychiatry', 500.00, 'ACTIVE', 'system-import'),
    ('Radiology & Imaging', 0.00, 'ACTIVE', 'system-import'),
    ('Rheumatology', 1000.00, 'ACTIVE', 'system-import'),
    ('Skin-VD', 0.00, 'ACTIVE', 'system-import'),
    ('Surgery', 500.00, 'ACTIVE', 'system-import'),
    ('Ultrasonogram', 0.00, 'ACTIVE', 'system-import'),
    ('Urology', 500.00, 'ACTIVE', 'system-import'),
    ('Vascular Surgery', 500.00, 'ACTIVE', 'system-import');

INSERT INTO hospital.doctor_departments (department_name, general_visit_amount, status, created_by)
SELECT department_name, general_visit_amount, status, created_by
FROM _093_doctor_department_import
ON CONFLICT (department_name) DO UPDATE
SET
    general_visit_amount = EXCLUDED.general_visit_amount,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system-import';

DELETE FROM hospital.doctor_departments d
WHERE d.department_name NOT IN (SELECT i.department_name FROM _093_doctor_department_import i)
  AND NOT EXISTS (
      SELECT 1
      FROM hospital.doctors doc
      WHERE doc.department_id = d.department_id
  );

DROP TABLE _093_doctor_department_import;
