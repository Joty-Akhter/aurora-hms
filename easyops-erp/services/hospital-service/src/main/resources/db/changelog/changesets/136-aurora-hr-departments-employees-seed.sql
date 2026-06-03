--liquibase formatted sql

--changeset hospital-service:136-aurora-hr-departments-employees-seed
--comment: Seed admin.departments, hr.positions, and hr.employees for Aurora Specialized Hospital (ASHK). Departments are owned by organization-service (admin.departments only).

-- Stable department ids (admin.departments.id; referenced by hr.employees / hr.positions)
-- Organization: a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6 (changeset 033)

INSERT INTO admin.departments (id, organization_id, code, name, description, type, status)
VALUES
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e001'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-CLINICAL', 'Clinical Services', 'Outpatient and inpatient clinical operations', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e002'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-NURSING', 'Nursing', 'Nursing care across wards and units', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e003'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-ADMIN', 'Hospital Administration', 'Front office, admissions, and general administration', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e004'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-HR', 'Human Resources', 'Recruitment, payroll support, and employee relations', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e005'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-FINANCE', 'Finance & Billing', 'Patient billing, accounts, and revenue cycle', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e006'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-PHARMACY', 'Pharmacy', 'In-house pharmacy and dispensary', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e007'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-LAB', 'Laboratory', 'Diagnostic laboratory services', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e008'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-RADIOLOGY', 'Radiology & Imaging', 'Radiology, CT, MRI, and imaging support', 'DEPARTMENT', 'ACTIVE'),
    ('f1a2b3c4-d5e6-4789-a012-a1b2c3d4e009'::uuid, 'a1b2c3d4-e5f6-4789-a012-a1b2c3d4e5f6'::uuid, 'DEPT-EMERGENCY', 'Emergency Department', '24/7 emergency and trauma care', 'DEPARTMENT', 'ACTIVE')
ON CONFLICT (organization_id, code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    type = EXCLUDED.type,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- Positions (department_id references admin.departments)
INSERT INTO hr.positions (organization_id, title, description, department_id, level, currency, is_active)
SELECT o.id, v.title, v.description, d.id, v.level, 'BDT', true
FROM admin.organizations o
CROSS JOIN (VALUES
    ('Medical Officer', 'Clinical support and ward coverage', 'DEPT-CLINICAL', 'MID'),
    ('Staff Nurse', 'Direct patient nursing care', 'DEPT-NURSING', 'JUNIOR'),
    ('Head Nurse', 'Nursing unit supervision', 'DEPT-NURSING', 'SENIOR'),
    ('Receptionist', 'Patient registration and front desk', 'DEPT-ADMIN', 'JUNIOR'),
    ('Hospital Administrator', 'Day-to-day hospital operations', 'DEPT-ADMIN', 'EXECUTIVE'),
    ('HR Officer', 'Employee records and HR operations', 'DEPT-HR', 'MID'),
    ('Billing Officer', 'Invoicing and payment collection', 'DEPT-FINANCE', 'MID'),
    ('Pharmacist', 'Medication dispensing and inventory', 'DEPT-PHARMACY', 'MID'),
    ('Lab Technologist', 'Sample processing and reporting', 'DEPT-LAB', 'MID'),
    ('Radiographer', 'Imaging procedures and PACS workflow', 'DEPT-RADIOLOGY', 'MID'),
    ('Emergency Nurse', 'Triage and emergency nursing care', 'DEPT-EMERGENCY', 'MID')
) AS v(title, description, dept_code, level)
JOIN admin.departments d ON d.organization_id = o.id AND d.code = v.dept_code
WHERE o.code = 'ASHK'
  AND NOT EXISTS (
      SELECT 1 FROM hr.positions p
      WHERE p.organization_id = o.id AND p.title = v.title
  );

-- Sample employees (non-physician hospital staff)
INSERT INTO hr.employees (
    organization_id, user_id, employee_number, name, email, phone,
    hire_date, department_id, position_id, employment_type, employment_status, is_active, created_by
)
SELECT
    o.id,
    NULL,
    v.emp_no,
    v.emp_name,
    v.email,
    v.phone,
    v.hire_date,
    d.id,
    p.position_id,
    'FULL_TIME',
    'ACTIVE',
    true,
    'hospital-seed'
FROM admin.organizations o
CROSS JOIN (VALUES
    ('EMP-ASHK-001', 'Fatima Begum', 'fatima.begum@aurora.hospital', '+880-1711-100001', DATE '2018-03-01', 'DEPT-NURSING', 'Staff Nurse'),
    ('EMP-ASHK-002', 'Rashida Khatun', 'rashida.khatun@aurora.hospital', '+880-1711-100002', DATE '2015-06-15', 'DEPT-NURSING', 'Head Nurse'),
    ('EMP-ASHK-003', 'Karim Uddin', 'karim.uddin@aurora.hospital', '+880-1711-100003', DATE '2019-01-10', 'DEPT-ADMIN', 'Receptionist'),
    ('EMP-ASHK-004', 'Nasima Akter', 'nasima.akter@aurora.hospital', '+880-1711-100004', DATE '2016-09-01', 'DEPT-ADMIN', 'Hospital Administrator'),
    ('EMP-ASHK-005', 'Shahidul Islam', 'shahidul.islam@aurora.hospital', '+880-1711-100005', DATE '2017-04-20', 'DEPT-HR', 'HR Officer'),
    ('EMP-ASHK-006', 'Mizanur Rahman', 'mizanur.rahman@aurora.hospital', '+880-1711-100006', DATE '2018-11-05', 'DEPT-FINANCE', 'Billing Officer'),
    ('EMP-ASHK-007', 'Nargis Sultana', 'nargis.sultana@aurora.hospital', '+880-1711-100007', DATE '2019-07-12', 'DEPT-PHARMACY', 'Pharmacist'),
    ('EMP-ASHK-008', 'Abdul Halim', 'abdul.halim@aurora.hospital', '+880-1711-100008', DATE '2020-02-28', 'DEPT-LAB', 'Lab Technologist'),
    ('EMP-ASHK-009', 'Taslima Ahmed', 'taslima.ahmed@aurora.hospital', '+880-1711-100009', DATE '2019-05-18', 'DEPT-RADIOLOGY', 'Radiographer'),
    ('EMP-ASHK-010', 'Mohammad Ali', 'mohammad.ali@aurora.hospital', '+880-1711-100010', DATE '2021-08-01', 'DEPT-EMERGENCY', 'Emergency Nurse'),
    ('EMP-ASHK-011', 'Sabina Yasmin', 'sabina.yasmin@aurora.hospital', '+880-1711-100011', DATE '2020-10-15', 'DEPT-CLINICAL', 'Medical Officer'),
    ('EMP-ASHK-012', 'Hasan Mahmud', 'hasan.mahmud@aurora.hospital', '+880-1711-100012', DATE '2022-01-03', 'DEPT-CLINICAL', 'Medical Officer')
) AS v(emp_no, emp_name, email, phone, hire_date, dept_code, pos_title)
JOIN admin.departments d ON d.organization_id = o.id AND d.code = v.dept_code
LEFT JOIN hr.positions p ON p.organization_id = o.id AND p.title = v.pos_title AND p.department_id = d.id
WHERE o.code = 'ASHK'
  AND NOT EXISTS (
      SELECT 1 FROM hr.employees e
      WHERE e.organization_id = o.id AND e.employee_number = v.emp_no
  );

-- Link default admin portal user to HR department when present
INSERT INTO hr.employees (
    organization_id, user_id, employee_number, name, email, phone,
    hire_date, department_id, position_id, employment_type, employment_status, is_active, created_by
)
SELECT
    o.id,
    u.id,
    'EMP-ASHK-ADMIN',
    'System Administrator',
    u.email,
    NULL,
    CURRENT_DATE,
    d.id,
    p.position_id,
    'FULL_TIME',
    'ACTIVE',
    true,
    'hospital-seed'
FROM admin.organizations o
CROSS JOIN users.users u
JOIN admin.departments d ON d.organization_id = o.id AND d.code = 'DEPT-HR'
LEFT JOIN hr.positions p ON p.organization_id = o.id AND p.title = 'HR Officer' AND p.department_id = d.id
WHERE o.code = 'ASHK'
  AND u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM hr.employees e
      WHERE e.organization_id = o.id AND e.user_id = u.id
  )
ON CONFLICT (organization_id, employee_number) DO UPDATE
SET name = EXCLUDED.name,
    email = EXCLUDED.email,
    department_id = EXCLUDED.department_id,
    position_id = EXCLUDED.position_id,
    is_active = true,
    updated_at = CURRENT_TIMESTAMP;
