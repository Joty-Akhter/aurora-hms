--liquibase formatted sql

--changeset easyops:071-ehr-patient-dummy-data
--validCheckSum: ANY
--comment: Seed 10 dummy patients for hospital testing environments
INSERT INTO ehr.patients (
    mrn,
    full_name,
    preferred_name,
    date_of_birth,
    gender,
    id_no,
    id_type,
    marital_status,
    blood_group,
    occupation,
    primary_address_line1,
    primary_city,
    primary_state,
    primary_zip,
    primary_country,
    primary_phone,
    primary_email,
    preferred_contact_method,
    consent_text_messaging,
    consent_email_communication,
    patient_status,
    preferred_language
) VALUES
    ('ASHK-26000000001', 'John Carter', 'John', DATE '1988-03-12', 'Male', 'A1001001', 'NATIONAL_ID', 'Married', 'O+', 'Engineer', '101 Maple St', 'Boston', 'MA', '02108', 'USA', '+1-617-555-1001', 'john.carter@example.test', 'Phone', true, true, 'ACTIVE', 'English'),
    ('ASHK-26000000002', 'Emma Walker', 'Emma', DATE '1992-07-24', 'Female', 'A1001002', 'NATIONAL_ID', 'Single', 'A+', 'Teacher', '202 Pine Ave', 'Chicago', 'IL', '60601', 'USA', '+1-312-555-1002', 'emma.walker@example.test', 'Email', true, true, 'ACTIVE', 'English'),
    ('ASHK-26000000003', 'Liam Brooks', 'Liam', DATE '1979-11-08', 'Male', 'A1001003', 'NATIONAL_ID', 'Married', 'B+', 'Manager', '33 River Rd', 'Dallas', 'TX', '75201', 'USA', '+1-214-555-1003', 'liam.brooks@example.test', 'Phone', false, true, 'ACTIVE', 'English'),
    ('ASHK-26000000004', 'Sophia Nguyen', 'Sophia', DATE '2001-01-16', 'Female', 'A1001004', 'NATIONAL_ID', 'Single', 'AB+', 'Student', '14 Lake View', 'Seattle', 'WA', '98101', 'USA', '+1-206-555-1004', 'sophia.nguyen@example.test', 'Phone', true, true, 'ACTIVE', 'English'),
    ('ASHK-26000000005', 'Noah Patel', 'Noah', DATE '1968-09-30', 'Male', 'A1001005', 'NATIONAL_ID', 'Married', 'O-', 'Accountant', '88 Cedar Ln', 'Atlanta', 'GA', '30301', 'USA', '+1-404-555-1005', 'noah.patel@example.test', 'Phone', true, false, 'ACTIVE', 'English'),
    ('ASHK-26000000006', 'Ava Johnson', 'Ava', DATE '1985-05-05', 'Female', 'A1001006', 'NATIONAL_ID', 'Divorced', 'B-', 'Nurse', '9 Elm Street', 'Denver', 'CO', '80202', 'USA', '+1-303-555-1006', 'ava.johnson@example.test', 'Email', true, true, 'ACTIVE', 'English'),
    ('ASHK-26000000007', 'Mason Rivera', 'Mason', DATE '1998-12-19', 'Male', 'A1001007', 'NATIONAL_ID', 'Single', 'A-', 'Analyst', '707 Hill Dr', 'Phoenix', 'AZ', '85001', 'USA', '+1-602-555-1007', 'mason.rivera@example.test', 'Phone', false, true, 'ACTIVE', 'English'),
    ('ASHK-26000000008', 'Isabella Reed', 'Bella', DATE '1974-04-02', 'Female', 'A1001008', 'NATIONAL_ID', 'Widowed', 'AB-', 'Consultant', '55 Sunset Blvd', 'Miami', 'FL', '33101', 'USA', '+1-305-555-1008', 'isabella.reed@example.test', 'Mail', false, true, 'ACTIVE', 'English'),
    ('ASHK-26000000009', 'Ethan Scott', 'Ethan', DATE '1990-10-27', 'Male', 'A1001009', 'NATIONAL_ID', 'Married', 'O+', 'Developer', '303 Oak Ct', 'San Jose', 'CA', '95101', 'USA', '+1-408-555-1009', 'ethan.scott@example.test', 'Email', true, true, 'ACTIVE', 'English'),
    ('ASHK-26000000010', 'Mia Turner', 'Mia', DATE '1983-06-14', 'Female', 'A1001010', 'NATIONAL_ID', 'Single', 'A+', 'Designer', '444 Garden Way', 'New York', 'NY', '10001', 'USA', '+1-212-555-1010', 'mia.turner@example.test', 'Phone', true, true, 'ACTIVE', 'English')
ON CONFLICT (mrn) DO NOTHING;
