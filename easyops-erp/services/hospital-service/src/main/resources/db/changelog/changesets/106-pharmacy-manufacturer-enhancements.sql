--liquibase formatted sql

--changeset easyops:106-manufacturers-add-cols
ALTER TABLE hospital_pharmacy.manufacturers
    ADD COLUMN IF NOT EXISTS license_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vat VARCHAR(50),
    ADD COLUMN IF NOT EXISTS commission NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS address TEXT;

ALTER TABLE hospital_pharmacy.manufacturers
    ADD CONSTRAINT chk_hp_manufacturer_type
    CHECK (type IS NULL OR type IN ('COMPANY', 'LOCAL_MARKET'));
