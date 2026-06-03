--liquibase formatted sql

--changeset easyops:083-hospital-menu-feature-permissions context:data
--comment: Fine-grained hospital UI menu permissions (MainLayout path prefixes). HOSPITAL_MANAGE grants full access; HOSPITAL_VIEW opens hub only (feature codes gate sub-routes).

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Rx templates', 'HOSPITAL_FEAT_RX_TEMPLATES', 'hospital.menu', 'feat_rx_templates', 'Navigate to prescription protocol templates'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_RX_TEMPLATES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Rx settings', 'HOSPITAL_FEAT_RX_SETTINGS', 'hospital.menu', 'feat_rx_settings', 'Navigate to Easy Prescription admin/settings'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_RX_SETTINGS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctor dashboard', 'HOSPITAL_FEAT_DOCTOR_DASHBOARD', 'hospital.menu', 'feat_doctor_dashboard', 'Navigate to doctor dashboard'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_DOCTOR_DASHBOARD');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Patients', 'HOSPITAL_FEAT_PATIENTS', 'hospital.menu', 'feat_patients', 'Navigate to patient master data and charts'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PATIENTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctors', 'HOSPITAL_FEAT_DOCTORS', 'hospital.menu', 'feat_doctors', 'Navigate to doctors'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_DOCTORS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctor schedule', 'HOSPITAL_FEAT_DOCTOR_SCHEDULE', 'hospital.menu', 'feat_doctor_schedule', 'Navigate to doctor schedules'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_DOCTOR_SCHEDULE');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctor departments', 'HOSPITAL_FEAT_DOCTOR_DEPARTMENTS', 'hospital.menu', 'feat_doctor_departments', 'Navigate to doctor departments'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_DOCTOR_DEPARTMENTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Medical codes', 'HOSPITAL_FEAT_MEDICAL_CODES', 'hospital.menu', 'feat_medical_codes', 'Navigate to medical codes catalog'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_MEDICAL_CODES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: ICD-10 codes', 'HOSPITAL_FEAT_MEDICAL_CODES_ICD10', 'hospital.menu', 'feat_medical_codes_icd10', 'Navigate to ICD-10 codes'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_MEDICAL_CODES_ICD10');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: ICD-11 codes', 'HOSPITAL_FEAT_MEDICAL_CODES_ICD11', 'hospital.menu', 'feat_medical_codes_icd11', 'Navigate to ICD-11 codes'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_MEDICAL_CODES_ICD11');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Note templates', 'HOSPITAL_FEAT_NOTE_TEMPLATES', 'hospital.menu', 'feat_note_templates', 'Navigate to clinical note templates'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_NOTE_TEMPLATES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy (section)', 'HOSPITAL_FEAT_PHARMACY', 'hospital.menu', 'feat_pharmacy', 'Navigate to hospital pharmacy area'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy catalog', 'HOSPITAL_FEAT_PHARMACY_CATALOG', 'hospital.menu', 'feat_pharmacy_catalog', 'Navigate to pharmacy catalog'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY_CATALOG');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy locations', 'HOSPITAL_FEAT_PHARMACY_LOCATIONS', 'hospital.menu', 'feat_pharmacy_locations', 'Navigate to pharmacy locations'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY_LOCATIONS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy stock', 'HOSPITAL_FEAT_PHARMACY_STOCK', 'hospital.menu', 'feat_pharmacy_stock', 'Navigate to pharmacy stock'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY_STOCK');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy dispense', 'HOSPITAL_FEAT_PHARMACY_DISPENSE', 'hospital.menu', 'feat_pharmacy_dispense_menu', 'Navigate to pharmacy dispensing'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY_DISPENSE');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Pharmacy reports', 'HOSPITAL_FEAT_PHARMACY_REPORTS', 'hospital.menu', 'feat_pharmacy_reports', 'Navigate to pharmacy reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PHARMACY_REPORTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Billing (section)', 'HOSPITAL_FEAT_BILLING', 'hospital.menu', 'feat_billing', 'Navigate to hospital billing area'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_BILLING');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Billing charges', 'HOSPITAL_FEAT_BILLING_CHARGES', 'hospital.menu', 'feat_billing_charges', 'Navigate to billing charges'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_BILLING_CHARGES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Billing invoices', 'HOSPITAL_FEAT_BILLING_INVOICES', 'hospital.menu', 'feat_billing_invoices', 'Navigate to billing invoices'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_BILLING_INVOICES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Billing payments', 'HOSPITAL_FEAT_BILLING_PAYMENTS', 'hospital.menu', 'feat_billing_payments', 'Navigate to billing payments'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_BILLING_PAYMENTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Billing reports', 'HOSPITAL_FEAT_BILLING_REPORTS', 'hospital.menu', 'feat_billing_reports', 'Navigate to billing reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_BILLING_REPORTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical orders (section)', 'HOSPITAL_FEAT_CLINICAL_ORDERS', 'hospital.menu', 'feat_clinical_orders', 'Navigate to clinical orders area'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical orders entry', 'HOSPITAL_FEAT_CLINICAL_ORDERS_ENTRY', 'hospital.menu', 'feat_clinical_orders_entry', 'Navigate to clinical order entry'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS_ENTRY');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Order sets', 'HOSPITAL_FEAT_CLINICAL_ORDERS_SETS', 'hospital.menu', 'feat_clinical_orders_sets', 'Navigate to clinical order sets'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS_SETS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical orders list', 'HOSPITAL_FEAT_CLINICAL_ORDERS_ORDERS', 'hospital.menu', 'feat_clinical_orders_orders', 'Navigate to clinical orders'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS_ORDERS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical worklists', 'HOSPITAL_FEAT_CLINICAL_ORDERS_WORKLISTS', 'hospital.menu', 'feat_clinical_orders_worklists', 'Navigate to clinical order worklists'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS_WORKLISTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Clinical orders reports', 'HOSPITAL_FEAT_CLINICAL_ORDERS_REPORTS', 'hospital.menu', 'feat_clinical_orders_reports', 'Navigate to clinical orders reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CLINICAL_ORDERS_REPORTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Corporate & discount (section)', 'HOSPITAL_FEAT_CORPORATE_DISCOUNT', 'hospital.menu', 'feat_corporate_discount', 'Navigate to corporate / discount area'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_DISCOUNT');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Corporates', 'HOSPITAL_FEAT_CORPORATE_CLIENTS', 'hospital.menu', 'feat_corporate_clients', 'Navigate to corporate clients'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_CLIENTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Contracts', 'HOSPITAL_FEAT_CORPORATE_CONTRACTS', 'hospital.menu', 'feat_corporate_contracts', 'Navigate to corporate contracts'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_CONTRACTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Packages', 'HOSPITAL_FEAT_CORPORATE_PACKAGES', 'hospital.menu', 'feat_corporate_packages', 'Navigate to packages'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_PACKAGES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Discount schemes', 'HOSPITAL_FEAT_CORPORATE_DISCOUNT_SCHEMES', 'hospital.menu', 'feat_corporate_discount_schemes', 'Navigate to discount schemes'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_DISCOUNT_SCHEMES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Discount decisions', 'HOSPITAL_FEAT_CORPORATE_DECISIONS', 'hospital.menu', 'feat_corporate_decisions', 'Navigate to discount decisions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_DECISIONS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Corporate reports', 'HOSPITAL_FEAT_CORPORATE_REPORTS', 'hospital.menu', 'feat_corporate_reports', 'Navigate to corporate/discount reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CORPORATE_REPORTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Cards (section)', 'HOSPITAL_FEAT_CARDS', 'hospital.menu', 'feat_cards', 'Navigate to patient cards management'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CARDS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Card products', 'HOSPITAL_FEAT_CARD_PRODUCTS', 'hospital.menu', 'feat_card_products', 'Navigate to card products'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_CARD_PRODUCTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Scheduling (section)', 'HOSPITAL_FEAT_SCHEDULING', 'hospital.menu', 'feat_scheduling', 'Navigate to scheduling area'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Scheduling resources', 'HOSPITAL_FEAT_SCHEDULING_RESOURCES', 'hospital.menu', 'feat_scheduling_resources', 'Navigate to scheduling resources'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_RESOURCES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Slot templates', 'HOSPITAL_FEAT_SCHEDULING_SLOT_TEMPLATES', 'hospital.menu', 'feat_scheduling_slot_templates', 'Navigate to slot templates'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_SLOT_TEMPLATES');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Blackouts', 'HOSPITAL_FEAT_SCHEDULING_BLACKOUTS', 'hospital.menu', 'feat_scheduling_blackouts', 'Navigate to scheduling blackouts'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_BLACKOUTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Availability', 'HOSPITAL_FEAT_SCHEDULING_AVAILABILITY', 'hospital.menu', 'feat_scheduling_availability', 'Navigate to availability'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_AVAILABILITY');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Reservations', 'HOSPITAL_FEAT_SCHEDULING_RESERVATIONS', 'hospital.menu', 'feat_scheduling_reservations', 'Navigate to reservations'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_RESERVATIONS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Appointments', 'HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS', 'hospital.menu', 'feat_scheduling_appointments', 'Navigate to appointments'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Queue', 'HOSPITAL_FEAT_SCHEDULING_QUEUE', 'hospital.menu', 'feat_scheduling_queue', 'Navigate to scheduling queue'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_QUEUE');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Planned admissions', 'HOSPITAL_FEAT_SCHEDULING_PLANNED_ADMISSIONS', 'hospital.menu', 'feat_scheduling_planned_admissions', 'Navigate to planned admissions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_PLANNED_ADMISSIONS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Expected admissions', 'HOSPITAL_FEAT_SCHEDULING_EXPECTED_ADMISSIONS', 'hospital.menu', 'feat_scheduling_expected_admissions', 'Navigate to expected admissions'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_EXPECTED_ADMISSIONS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Roster blocks', 'HOSPITAL_FEAT_SCHEDULING_ROSTER_BLOCKS', 'hospital.menu', 'feat_scheduling_roster_blocks', 'Navigate to roster blocks'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_ROSTER_BLOCKS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Waitlist', 'HOSPITAL_FEAT_SCHEDULING_WAITLIST', 'hospital.menu', 'feat_scheduling_waitlist', 'Navigate to waitlist'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_WAITLIST');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Scheduling reports', 'HOSPITAL_FEAT_SCHEDULING_REPORTS', 'hospital.menu', 'feat_scheduling_reports', 'Navigate to scheduling reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_REPORTS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Doctor mappings', 'HOSPITAL_FEAT_SCHEDULING_DOCTOR_MAPPINGS', 'hospital.menu', 'feat_scheduling_doctor_mappings', 'Navigate to doctor mappings'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_DOCTOR_MAPPINGS');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Scheduling audit log', 'HOSPITAL_FEAT_SCHEDULING_AUDIT_LOG', 'hospital.menu', 'feat_scheduling_audit_log', 'Navigate to scheduling audit log'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_SCHEDULING_AUDIT_LOG');
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Hospital menu: Portal my cards', 'HOSPITAL_FEAT_PORTAL_MY_CARDS', 'hospital.menu', 'feat_portal_my_cards', 'Navigate to portal card holder view'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HOSPITAL_FEAT_PORTAL_MY_CARDS');

INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.resource = 'hospital.menu' AND p.code LIKE 'HOSPITAL_FEAT_%'
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
