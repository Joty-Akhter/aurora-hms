--liquibase formatted sql

--changeset easyops:118-pharmacy-credit-account-version context:schema
--comment: Add optimistic-lock version column to pharmacy_credit_accounts (required by @Version on PharmacyCreditAccount entity)
ALTER TABLE hospital_pharmacy.pharmacy_credit_accounts
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
