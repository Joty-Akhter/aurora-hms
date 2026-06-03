--liquibase formatted sql

--changeset easyops:070-pharmacy-location-workflow-type
--comment: Add workflow_type column to pharmacy_locations (SUPPLIER, CENTRAL_STORE, OUTLET_PHARMACY)
ALTER TABLE hospital_pharmacy.pharmacy_locations
    ADD COLUMN IF NOT EXISTS workflow_type VARCHAR(50);
