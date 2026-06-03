--liquibase formatted sql

--changeset easyops:hosp-pharm-001-create-schema context:hospital-pharmacy
--comment: Create hospital_pharmacy schema (owned by hospital-service)
CREATE SCHEMA IF NOT EXISTS hospital_pharmacy;
GRANT ALL PRIVILEGES ON SCHEMA hospital_pharmacy TO easyops;

--changeset easyops:hosp-pharm-002-create-manufacturers context:hospital-pharmacy
--comment: Create manufacturers table for hospital pharmacy drug catalog
CREATE TABLE IF NOT EXISTS hospital_pharmacy.manufacturers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    short_code VARCHAR(50),
    country VARCHAR(100),
    contact_info TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hp_manufacturers_name ON hospital_pharmacy.manufacturers (LOWER(name));

--changeset easyops:hosp-pharm-003-create-drugs context:hospital-pharmacy
--comment: Create drugs table (central drug master for hospital module)
CREATE TABLE IF NOT EXISTS hospital_pharmacy.drugs (
    id UUID PRIMARY KEY,
    generic_name VARCHAR(255) NOT NULL,
    brand_name VARCHAR(255),
    strength VARCHAR(100),
    form VARCHAR(100),
    route VARCHAR(100),
    pack_size VARCHAR(50),
    unit_of_measure VARCHAR(50),
    therapeutic_class_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    controlled_drug_flag BOOLEAN DEFAULT FALSE,
    batch_required BOOLEAN DEFAULT TRUE,
    expiry_required BOOLEAN DEFAULT TRUE,
    manufacturer_id UUID NOT NULL REFERENCES hospital_pharmacy.manufacturers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hp_drugs_generic_name ON hospital_pharmacy.drugs (LOWER(generic_name));
CREATE INDEX IF NOT EXISTS idx_hp_drugs_brand_name ON hospital_pharmacy.drugs (LOWER(brand_name));
CREATE INDEX IF NOT EXISTS idx_hp_drugs_active ON hospital_pharmacy.drugs (is_active);

--changeset easyops:hosp-pharm-004-create-formulary-rules context:hospital-pharmacy
--comment: Create formulary_rules table for hospital pharmacy
CREATE TABLE IF NOT EXISTS hospital_pharmacy.formulary_rules (
    id UUID PRIMARY KEY,
    drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id) ON DELETE CASCADE,
    restricted BOOLEAN DEFAULT FALSE,
    restriction_reason TEXT,
    ward_id UUID,
    department_id UUID,
    corporate_contract_id UUID,
    preferred_alternative_drug_ids JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hp_formulary_rules_drug ON hospital_pharmacy.formulary_rules (drug_id);

--changeset easyops:hosp-pharm-005-create-pharmacy-locations context:hospital-pharmacy
--comment: Create pharmacy_locations table for hospital pharmacy
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_locations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_24x7 BOOLEAN DEFAULT FALSE,
    operational_hours TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hp_pharmacy_locations_type ON hospital_pharmacy.pharmacy_locations (type);
CREATE INDEX IF NOT EXISTS idx_hp_pharmacy_locations_active ON hospital_pharmacy.pharmacy_locations (is_active);

