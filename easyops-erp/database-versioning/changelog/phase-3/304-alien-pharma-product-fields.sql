--liquibase formatted sql

--changeset easyops:304-add-alien-pharma-product-fields context:inventory
--comment: Add pack_size and box_size fields to products table for Alien Pharma requirements
ALTER TABLE inventory.products 
ADD COLUMN IF NOT EXISTS pack_size DECIMAL(10, 2) DEFAULT 1;

ALTER TABLE inventory.products 
ADD COLUMN IF NOT EXISTS box_size DECIMAL(10, 2) DEFAULT 1;

--changeset easyops:305-update-existing-products-pack-box-size context:inventory
--comment: Update existing products to have default values for pack_size and box_size
UPDATE inventory.products 
SET pack_size = 1 
WHERE pack_size IS NULL;

UPDATE inventory.products 
SET box_size = 1 
WHERE box_size IS NULL;

