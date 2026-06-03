--liquibase formatted sql
--changeset easyops:ep-dosage-form-suppository-134
--comment: Add SUPPOSITORY to EP dosage form lookup for Rx templates

INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('DOSAGE_FORM', 'SUPPOSITORY', 15)
ON CONFLICT (category, value) DO NOTHING;
