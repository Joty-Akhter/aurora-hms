--liquibase formatted sql

--changeset easyops:069-ep-lookup-items
--comment: EP lookup/reference data table — stores configurable dropdown and suggestion lists (dosage forms, disease categories, frequencies, instructions, referrals, complaints, medications, advice, tests)

CREATE TABLE IF NOT EXISTS ehr.ep_lookup_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    category    VARCHAR(50)  NOT NULL,
    value       VARCHAR(255) NOT NULL,
    display_order INTEGER    NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ep_lookup_category_value UNIQUE (category, value)
);

CREATE INDEX IF NOT EXISTS idx_ep_lookup_category     ON ehr.ep_lookup_items (category);
CREATE INDEX IF NOT EXISTS idx_ep_lookup_category_ord ON ehr.ep_lookup_items (category, display_order);

-- ── Seed: DOSAGE_FORM ──────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('DOSAGE_FORM', 'TABLET',      1),
  ('DOSAGE_FORM', 'CAPSULE',     2),
  ('DOSAGE_FORM', 'SYRUP',       3),
  ('DOSAGE_FORM', 'LIQUID',      4),
  ('DOSAGE_FORM', 'SOLUTION',    5),
  ('DOSAGE_FORM', 'SUSPENSION',  6),
  ('DOSAGE_FORM', 'INJECTION',   7),
  ('DOSAGE_FORM', 'INFUSION',    8),
  ('DOSAGE_FORM', 'CREAM',       9),
  ('DOSAGE_FORM', 'OINTMENT',   10),
  ('DOSAGE_FORM', 'GEL',        11),
  ('DOSAGE_FORM', 'DROPS',      12),
  ('DOSAGE_FORM', 'INHALER',    13),
  ('DOSAGE_FORM', 'OTHER',      14)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: DISEASE_CATEGORY ────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('DISEASE_CATEGORY', 'Respiratory',     1),
  ('DISEASE_CATEGORY', 'Gastrointestinal',2),
  ('DISEASE_CATEGORY', 'Cardiovascular',  3),
  ('DISEASE_CATEGORY', 'Endocrine',       4),
  ('DISEASE_CATEGORY', 'Renal',           5),
  ('DISEASE_CATEGORY', 'Musculoskeletal', 6),
  ('DISEASE_CATEGORY', 'Neurological',    7),
  ('DISEASE_CATEGORY', 'Mental Health',   8),
  ('DISEASE_CATEGORY', 'Dermatology',     9),
  ('DISEASE_CATEGORY', 'Other',          10)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: FREQUENCY ───────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('FREQUENCY', '1+0+0',              1),
  ('FREQUENCY', '0+1+0',              2),
  ('FREQUENCY', '0+0+1',              3),
  ('FREQUENCY', '1+0+1',              4),
  ('FREQUENCY', '1+1+0',              5),
  ('FREQUENCY', '0+1+1',              6),
  ('FREQUENCY', '1+1+1',              7),
  ('FREQUENCY', '1-0-1',              8),
  ('FREQUENCY', '0-1-0',              9),
  ('FREQUENCY', '0-0-1',             10),
  ('FREQUENCY', '1-1-0',             11),
  ('FREQUENCY', '0-1-1',             12),
  ('FREQUENCY', '1-1-1',             13),
  ('FREQUENCY', '1-0-0',             14),
  ('FREQUENCY', 'Once daily',        15),
  ('FREQUENCY', 'Twice daily',       16),
  ('FREQUENCY', 'Three times daily', 17),
  ('FREQUENCY', 'Four times daily',  18),
  ('FREQUENCY', 'Every 8 hours',     19),
  ('FREQUENCY', 'Every 12 hours',    20),
  ('FREQUENCY', 'Once a week',       21),
  ('FREQUENCY', 'Twice a week',      22),
  ('FREQUENCY', 'As needed (PRN)',   23),
  ('FREQUENCY', 'Stat (once only)',  24)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: INSTRUCTION ─────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('INSTRUCTION', 'Before meal',                       1),
  ('INSTRUCTION', 'After meal',                        2),
  ('INSTRUCTION', 'With food',                         3),
  ('INSTRUCTION', 'Empty stomach',                     4),
  ('INSTRUCTION', 'At bedtime',                        5),
  ('INSTRUCTION', 'In the morning',                    6),
  ('INSTRUCTION', 'In the evening',                    7),
  ('INSTRUCTION', 'As needed (PRN)',                   8),
  ('INSTRUCTION', 'Do not crush or chew',              9),
  ('INSTRUCTION', 'Swallow whole with water',         10),
  ('INSTRUCTION', 'Shake well before use',            11),
  ('INSTRUCTION', 'Apply thin layer to affected area',12),
  ('INSTRUCTION', 'Complete full course',             13),
  ('INSTRUCTION', 'Avoid alcohol while taking this medicine', 14)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: REFERRAL ────────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('REFERRAL', 'Cardiology',               1),
  ('REFERRAL', 'Dermatology',              2),
  ('REFERRAL', 'Endocrinology',            3),
  ('REFERRAL', 'ENT (Ear, Nose & Throat)', 4),
  ('REFERRAL', 'Gastroenterology',         5),
  ('REFERRAL', 'General Surgery',          6),
  ('REFERRAL', 'Gynecology & Obstetrics',  7),
  ('REFERRAL', 'Hematology',               8),
  ('REFERRAL', 'Nephrology',               9),
  ('REFERRAL', 'Neurology',               10),
  ('REFERRAL', 'Neurosurgery',            11),
  ('REFERRAL', 'Oncology',               12),
  ('REFERRAL', 'Ophthalmology',          13),
  ('REFERRAL', 'Orthopedics',            14),
  ('REFERRAL', 'Pediatrics',             15),
  ('REFERRAL', 'Psychiatry',             16),
  ('REFERRAL', 'Pulmonology',            17),
  ('REFERRAL', 'Rheumatology',           18),
  ('REFERRAL', 'Urology',                19),
  ('REFERRAL', 'Vascular Surgery',       20)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: COMPLAINT ───────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('COMPLAINT', 'Fever',               1),
  ('COMPLAINT', 'Cough',               2),
  ('COMPLAINT', 'Shortness of breath', 3),
  ('COMPLAINT', 'Chest pain',          4),
  ('COMPLAINT', 'Headache',            5),
  ('COMPLAINT', 'Abdominal pain',      6),
  ('COMPLAINT', 'Nausea',              7),
  ('COMPLAINT', 'Vomiting',            8),
  ('COMPLAINT', 'Diarrhea',            9),
  ('COMPLAINT', 'Constipation',       10),
  ('COMPLAINT', 'Back pain',          11),
  ('COMPLAINT', 'Joint pain',         12),
  ('COMPLAINT', 'Fatigue',            13),
  ('COMPLAINT', 'Dizziness',          14),
  ('COMPLAINT', 'Sore throat',        15),
  ('COMPLAINT', 'Runny nose',         16),
  ('COMPLAINT', 'Skin rash',          17),
  ('COMPLAINT', 'Palpitations',       18),
  ('COMPLAINT', 'Loss of appetite',   19),
  ('COMPLAINT', 'Weight loss',        20),
  ('COMPLAINT', 'Swelling',           21),
  ('COMPLAINT', 'Burning urination',  22),
  ('COMPLAINT', 'Frequent urination', 23)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: MEDICATION ──────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('MEDICATION', 'Paracetamol',     1),
  ('MEDICATION', 'Ibuprofen',       2),
  ('MEDICATION', 'Aspirin',         3),
  ('MEDICATION', 'Amoxicillin',     4),
  ('MEDICATION', 'Azithromycin',    5),
  ('MEDICATION', 'Metformin',       6),
  ('MEDICATION', 'Atorvastatin',    7),
  ('MEDICATION', 'Omeprazole',      8),
  ('MEDICATION', 'Metoprolol',      9),
  ('MEDICATION', 'Amlodipine',     10),
  ('MEDICATION', 'Lisinopril',     11),
  ('MEDICATION', 'Losartan',       12),
  ('MEDICATION', 'Cetirizine',     13),
  ('MEDICATION', 'Loratadine',     14),
  ('MEDICATION', 'Pantoprazole',   15),
  ('MEDICATION', 'Ranitidine',     16),
  ('MEDICATION', 'Domperidone',    17),
  ('MEDICATION', 'Metoclopramide', 18),
  ('MEDICATION', 'Loperamide',     19),
  ('MEDICATION', 'ORS',            20),
  ('MEDICATION', 'Prednisolone',   21),
  ('MEDICATION', 'Dexamethasone',  22),
  ('MEDICATION', 'Salbutamol',     23),
  ('MEDICATION', 'Montelukast',    24),
  ('MEDICATION', 'Ciprofloxacin',  25),
  ('MEDICATION', 'Doxycycline',    26),
  ('MEDICATION', 'Metronidazole',  27),
  ('MEDICATION', 'Fluconazole',    28),
  ('MEDICATION', 'Acyclovir',      29),
  ('MEDICATION', 'Diclofenac',     30)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: ADVICE ──────────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('ADVICE', 'Take rest',                       1),
  ('ADVICE', 'Drink plenty of water',           2),
  ('ADVICE', 'Light diet',                      3),
  ('ADVICE', 'Avoid oily/spicy food',           4),
  ('ADVICE', 'Complete antibiotic course',      5),
  ('ADVICE', 'Do not self-medicate',            6),
  ('ADVICE', 'Regular exercise',                7),
  ('ADVICE', 'Monitor blood pressure at home',  8),
  ('ADVICE', 'Monitor blood glucose at home',   9),
  ('ADVICE', 'Return if symptoms worsen',       10)
ON CONFLICT (category, value) DO NOTHING;

-- ── Seed: TEST ────────────────────────────────────────────────────────────
INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('TEST', 'CBC',                  1),
  ('TEST', 'FBS',                  2),
  ('TEST', 'RBS',                  3),
  ('TEST', 'HbA1c',                4),
  ('TEST', 'Lipid Profile',        5),
  ('TEST', 'LFT',                  6),
  ('TEST', 'RFT',                  7),
  ('TEST', 'Thyroid Profile',      8),
  ('TEST', 'Urine R/E',            9),
  ('TEST', 'Urine C/S',           10),
  ('TEST', 'ECG',                 11),
  ('TEST', 'Chest X-Ray',         12),
  ('TEST', 'Ultrasound Abdomen',  13)
ON CONFLICT (category, value) DO NOTHING;
