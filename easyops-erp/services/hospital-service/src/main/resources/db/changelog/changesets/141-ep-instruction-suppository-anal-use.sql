--liquibase formatted sql
--changeset easyops:ep-instruction-suppository-anal-use-141
--comment: Add suppository (anal use) instruction to EP lookup

INSERT INTO ehr.ep_lookup_items (category, value, display_order) VALUES
  ('INSTRUCTION', 'Anal Use (পায়ু পথে ব্যবহার)', 15)
ON CONFLICT (category, value) DO NOTHING;
