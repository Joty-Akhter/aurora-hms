--liquibase formatted sql

--changeset easyops:074-hospital-sample-doctors
--validCheckSum: ANY
--comment: Seed 10 sample doctors in hospital.doctors for testing (links to doctor_departments by name)
INSERT INTO hospital.doctors (
    doctor_code,
    doctor_name,
    department_id,
    doctor_type,
    indoor_outdoor_status,
    degree,
    speciality,
    gender,
    birth_date,
    registration_date,
    bmdc_registration_number,
    phone_number,
    email,
    visit_fee_new,
    visit_fee_old,
    prescription_status,
    availability_status,
    is_active,
    created_by
)
SELECT
    v.doctor_code,
    v.doctor_name,
    d.department_id,
    v.doctor_type,
    v.indoor_outdoor_status,
    v.degree,
    v.speciality,
    v.gender,
    v.birth_date,
    v.registration_date,
    v.bmdc_registration_number,
    v.phone_number,
    v.email,
    v.visit_fee_new,
    v.visit_fee_old,
    'ACTIVE',
    'AVAILABLE',
    TRUE,
    'system'
FROM (VALUES
    ('TEST-DOC-0001', 'Dr. Farhana Rahman', 'Cardiology', 'CONSULTANT', 'INDOOR', 'Female', DATE '1980-04-12', DATE '2010-06-01', 'MBBS, MD', 'Interventional Cardiology', 'BMDC-TEST-0001', '+880-1711-010101', 'farhana.rahman.doctor@example.test', 2000.00, 1500.00),
    ('TEST-DOC-0002', 'Dr. Md. Kamrul Hasan', 'Orthopedics', 'CONSULTANT', 'INDOOR', 'Male', DATE '1976-09-03', DATE '2008-03-15', 'MBBS, MS (Ortho)', 'Joint Replacement', 'BMDC-TEST-0002', '+880-1712-010102', 'kamrul.hasan.doctor@example.test', 1800.00, 1400.00),
    ('TEST-DOC-0003', 'Dr. Nusrat Jahan Chowdhury', 'Neurology', 'CONSULTANT', 'INDOOR', 'Female', DATE '1982-11-20', DATE '2012-01-10', 'MBBS, DM (Neuro)', 'Stroke & Epilepsy', 'BMDC-TEST-0003', '+880-1713-010103', 'nusrat.jahan.chowdhury.doctor@example.test', 2200.00, 1700.00),
    ('TEST-DOC-0004', 'Dr. Md. Abdul Karim', 'General Medicine', 'CONSULTANT', 'OUTDOOR', 'Male', DATE '1979-02-14', DATE '2007-07-01', 'MBBS, MD (Med)', 'Internal Medicine', 'BMDC-TEST-0004', '+880-1714-010104', 'abdul.karim.doctor@example.test', 1200.00, 900.00),
    ('TEST-DOC-0005', 'Dr. Sharmin Sultana', 'Pediatrics', 'CONSULTANT', 'INDOOR', 'Female', DATE '1985-07-08', DATE '2014-05-20', 'MBBS, DCH', 'General Pediatrics', 'BMDC-TEST-0005', '+880-1715-010105', 'sharmin.sultana.doctor@example.test', 1300.00, 1000.00),
    ('TEST-DOC-0006', 'Dr. Md. Rafiqul Islam', 'Emergency Medicine', 'CONSULTANT', 'INDOOR', 'Male', DATE '1983-12-01', DATE '2011-09-01', 'MBBS, MD (EM)', 'Emergency Care', 'BMDC-TEST-0006', '+880-1716-010106', 'rafiqul.islam.doctor@example.test', 1500.00, 1200.00),
    ('TEST-DOC-0007', 'Dr. Nasreen Akter', 'Obstetrics', 'CONSULTANT', 'INDOOR', 'Female', DATE '1981-05-25', DATE '2009-11-12', 'MBBS, MS (OBGYN)', 'High-Risk Obstetrics', 'BMDC-TEST-0007', '+880-1717-010107', 'nasreen.akter.doctor@example.test', 1900.00, 1450.00),
    ('TEST-DOC-0008', 'Dr. Md. Mahmudul Haque', 'Radiology', 'CONSULTANT', 'INDOOR', 'Male', DATE '1978-10-30', DATE '2006-04-18', 'MBBS, MD (Rad)', 'Diagnostic Imaging', 'BMDC-TEST-0008', '+880-1718-010108', 'mahmudul.haque.doctor@example.test', 1600.00, 1300.00),
    ('TEST-DOC-0009', 'Dr. Shama Naznin', 'Dermatology', 'CONSULTANT', 'OUTDOOR', 'Female', DATE '1987-03-17', DATE '2015-08-05', 'MBBS, DDVL', 'Clinical Dermatology', 'BMDC-TEST-0009', '+880-1719-010109', 'shama.naznin.doctor@example.test', 1400.00, 1100.00),
    ('TEST-DOC-0010', 'Dr. Md. Ashfaq Uddin', 'Pulmonology', 'CONSULTANT', 'INDOOR', 'Male', DATE '1975-06-22', DATE '2005-02-28', 'MBBS, MD (Pulm)', 'Respiratory Medicine', 'BMDC-TEST-0010', '+880-1720-010110', 'ashfaq.uddin.doctor@example.test', 2100.00, 1600.00)
) AS v (
    doctor_code,
    doctor_name,
    dept_name,
    doctor_type,
    indoor_outdoor_status,
    gender,
    birth_date,
    registration_date,
    degree,
    speciality,
    bmdc_registration_number,
    phone_number,
    email,
    visit_fee_new,
    visit_fee_old
)
JOIN hospital.doctor_departments d ON d.department_name = v.dept_name
ON CONFLICT (doctor_code) DO NOTHING;
