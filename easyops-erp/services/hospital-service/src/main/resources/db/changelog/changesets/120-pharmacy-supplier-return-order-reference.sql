--liquibase formatted sql

--changeset easyops:120-supplier-return-order-return-reference context:schema
--comment: Add return_reference column to supplier_return_orders (required by SupplierReturnOrder entity)
ALTER TABLE hospital_pharmacy.supplier_return_orders
    ADD COLUMN IF NOT EXISTS return_reference VARCHAR(100);
