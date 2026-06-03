--liquibase formatted sql

--changeset easyops:hosp-card-001-create-schema context:hospital-card
--comment: Create hospital_card schema (owned by hospital-service); Phase 0 – tables added in Phase 1
CREATE SCHEMA IF NOT EXISTS hospital_card;
GRANT ALL PRIVILEGES ON SCHEMA hospital_card TO easyops;
