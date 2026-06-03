--liquibase formatted sql

--changeset easyops:119-supplier-return-order-line-notes context:schema
--comment: Add notes column to supplier_return_order_lines (required by SupplierReturnOrderLine entity)
ALTER TABLE hospital_pharmacy.supplier_return_order_lines
    ADD COLUMN IF NOT EXISTS notes TEXT;
