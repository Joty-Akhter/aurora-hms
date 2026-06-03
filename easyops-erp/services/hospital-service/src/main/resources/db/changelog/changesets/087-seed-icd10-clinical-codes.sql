--liquibase formatted sql

--changeset easyops:087-seed-icd10-clinical-codes
--comment: Seed representative ICD-10-CM codes for EHR lookups (e.g. Salmonella A02.x) — expands autocomplete beyond empty catalog

INSERT INTO ehr.icd10_codes (code, description, category, chapter, is_valid) VALUES
('A02.0', 'Salmonella enteritis', 'Certain infectious and parasitic diseases', 'Intestinal infectious diseases', true),
('A02.1', 'Salmonella sepsis', 'Certain infectious and parasitic diseases', 'Intestinal infectious diseases', true),
('A02.2', 'Localized salmonella infections, unspecified', 'Certain infectious and parasitic diseases', 'Intestinal infectious diseases', true),
('A02.8', 'Other specified salmonella infections', 'Certain infectious and parasitic diseases', 'Intestinal infectious diseases', true),
('A02.9', 'Salmonella infection, unspecified', 'Certain infectious and parasitic diseases', 'Intestinal infectious diseases', true)
ON CONFLICT (code) DO UPDATE SET
  description = EXCLUDED.description,
  category = EXCLUDED.category,
  chapter = EXCLUDED.chapter,
  is_valid = EXCLUDED.is_valid,
  updated_at = CURRENT_TIMESTAMP;
