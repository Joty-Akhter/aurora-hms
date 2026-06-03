--liquibase formatted sql

--changeset easyops:hosp-corp-disc-065-corporate-cards-validity-and-block
--comment: Phase 3 gap closure - add validity window for expiry handling
ALTER TABLE hospital_corporate_discount.corporate_cards
    ADD COLUMN IF NOT EXISTS valid_from DATE NULL,
    ADD COLUMN IF NOT EXISTS valid_to DATE NULL;

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_cards_valid_to
    ON hospital_corporate_discount.corporate_cards (valid_to);
