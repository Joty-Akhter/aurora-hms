--liquibase formatted sql

--changeset easyops:307-drop-box-size-column context:inventory
--comment: Remove box_size column from products; use quantity only everywhere
ALTER TABLE inventory.products DROP COLUMN IF EXISTS box_size;
