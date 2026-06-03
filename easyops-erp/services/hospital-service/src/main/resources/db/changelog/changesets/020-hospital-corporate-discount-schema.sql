--liquibase formatted sql

--changeset easyops:hosp-corp-disc-001-create-schema
--comment: Create hospital_corporate_discount schema (owned by hospital-service)
CREATE SCHEMA IF NOT EXISTS hospital_corporate_discount;
GRANT ALL PRIVILEGES ON SCHEMA hospital_corporate_discount TO easyops;

--changeset easyops:hosp-corp-disc-002-corporate-clients
--comment: Phase 1 – corporate_clients table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.corporate_clients (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valid_from DATE,
    valid_to DATE,
    primary_contact_name VARCHAR(255),
    primary_contact_phone VARCHAR(50),
    primary_contact_email VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_clients_code
    ON hospital_corporate_discount.corporate_clients (code);
CREATE INDEX IF NOT EXISTS idx_hcd_corporate_clients_status
    ON hospital_corporate_discount.corporate_clients (status);
CREATE INDEX IF NOT EXISTS idx_hcd_corporate_clients_type
    ON hospital_corporate_discount.corporate_clients (type);

--changeset easyops:hosp-corp-disc-003-corporate-contracts
--comment: Phase 1 – corporate_contracts table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.corporate_contracts (
    id UUID PRIMARY KEY,
    corporate_client_id UUID NOT NULL,
    contract_code VARCHAR(50) NOT NULL,
    contract_name VARCHAR(255),
    valid_from DATE NOT NULL,
    valid_to DATE,
    coverage_type VARCHAR(20) NOT NULL,
    service_locations VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_corporate_contracts_client
        FOREIGN KEY (corporate_client_id) REFERENCES hospital_corporate_discount.corporate_clients(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_contracts_client
    ON hospital_corporate_discount.corporate_contracts (corporate_client_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hcd_corporate_contracts_client_code
    ON hospital_corporate_discount.corporate_contracts (corporate_client_id, contract_code);
CREATE INDEX IF NOT EXISTS idx_hcd_corporate_contracts_valid_from
    ON hospital_corporate_discount.corporate_contracts (valid_from);
CREATE INDEX IF NOT EXISTS idx_hcd_corporate_contracts_valid_to
    ON hospital_corporate_discount.corporate_contracts (valid_to);
