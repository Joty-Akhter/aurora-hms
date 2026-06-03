--liquibase formatted sql

--changeset easyops:105a-drugs-add-pricing-catalog-cols
ALTER TABLE hospital_pharmacy.drugs
    ADD COLUMN IF NOT EXISTS product_group_id UUID REFERENCES hospital_pharmacy.product_groups(id),
    ADD COLUMN IF NOT EXISTS dispensing_unit_id UUID REFERENCES hospital_pharmacy.units(id),
    ADD COLUMN IF NOT EXISTS mrp NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS sale_price NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS purchase_price NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS rack_no VARCHAR(50),
    ADD COLUMN IF NOT EXISTS reminder_stock NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS product_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS department_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS ux_hp_drugs_product_code
    ON hospital_pharmacy.drugs (product_code)
    WHERE product_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_hp_drugs_product_group
    ON hospital_pharmacy.drugs (product_group_id);
