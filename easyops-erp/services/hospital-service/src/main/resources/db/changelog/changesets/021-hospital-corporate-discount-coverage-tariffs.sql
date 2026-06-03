--liquibase formatted sql

--changeset easyops:hosp-corp-disc-004-coverage-rules
--comment: Phase 2 – coverage_rules table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.coverage_rules (
    id UUID PRIMARY KEY,
    corporate_contract_id UUID NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    scope_value VARCHAR(100) NOT NULL,
    coverage_percent NUMERIC(5,2) NOT NULL,
    max_amount NUMERIC(19,4),
    co_pay_percent NUMERIC(5,2) DEFAULT 0,
    deductible_amount NUMERIC(19,4) DEFAULT 0,
    applicable_visit_types VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coverage_rules_contract
        FOREIGN KEY (corporate_contract_id) REFERENCES hospital_corporate_discount.corporate_contracts(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_coverage_rules_contract
    ON hospital_corporate_discount.coverage_rules (corporate_contract_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hcd_coverage_rules_contract_scope
    ON hospital_corporate_discount.coverage_rules (corporate_contract_id, scope_type, scope_value);

--changeset easyops:hosp-corp-disc-005-packages
--comment: Phase 2 – packages table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.packages (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    default_price NUMERIC(19,4),
    is_corporate_only BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:hosp-corp-disc-006-package-items
--comment: Phase 2 – package_items table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.package_items (
    id UUID PRIMARY KEY,
    package_id UUID NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    quantity_included NUMERIC(19,4) DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_package_items_package
        FOREIGN KEY (package_id) REFERENCES hospital_corporate_discount.packages(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_package_items_package
    ON hospital_corporate_discount.package_items (package_id);

--changeset easyops:hosp-corp-disc-007-corporate-tariffs
--comment: Phase 2 – corporate_tariffs table
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.corporate_tariffs (
    id UUID PRIMARY KEY,
    corporate_contract_id UUID NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    scope_value VARCHAR(100) NOT NULL,
    tariff_type VARCHAR(20) NOT NULL,
    tariff_amount NUMERIC(19,4),
    tariff_percent NUMERIC(5,2),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_corporate_tariffs_contract
        FOREIGN KEY (corporate_contract_id) REFERENCES hospital_corporate_discount.corporate_contracts(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_tariffs_contract
    ON hospital_corporate_discount.corporate_tariffs (corporate_contract_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hcd_corporate_tariffs_contract_scope
    ON hospital_corporate_discount.corporate_tariffs (corporate_contract_id, scope_type, scope_value);
