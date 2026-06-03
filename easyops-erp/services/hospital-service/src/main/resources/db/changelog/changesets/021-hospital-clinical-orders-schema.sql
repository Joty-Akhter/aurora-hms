--liquibase formatted sql

--changeset easyops:hosp-clinical-orders-001-create-schema context:hospital-clinical-orders
--comment: Create hospital_clinical_orders schema (owned by hospital-service); Phase 0 – tables added in Phase 1
CREATE SCHEMA IF NOT EXISTS hospital_clinical_orders;
GRANT ALL PRIVILEGES ON SCHEMA hospital_clinical_orders TO easyops;
