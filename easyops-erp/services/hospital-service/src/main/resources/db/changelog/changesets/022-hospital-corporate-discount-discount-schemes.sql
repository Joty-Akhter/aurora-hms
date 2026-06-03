--liquibase formatted sql

--changeset easyops:hosp-corp-disc-008-discount-schemes
--comment: Phase 3 – discount_schemes table (§4.7)
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.discount_schemes (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    corporate_client_id UUID,
    visit_type VARCHAR(20),
    department_id UUID,
    service_code VARCHAR(100),
    patient_category VARCHAR(50),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(19,4) NOT NULL,
    max_discount_amount NUMERIC(19,4),
    max_discount_percent NUMERIC(5,2),
    requires_approval BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    valid_from DATE,
    valid_to DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT uq_hcd_discount_schemes_code UNIQUE (code),
    CONSTRAINT fk_hcd_discount_schemes_corporate
        FOREIGN KEY (corporate_client_id) REFERENCES hospital_corporate_discount.corporate_clients(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_discount_schemes_code
    ON hospital_corporate_discount.discount_schemes (code);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_schemes_corporate_client_id
    ON hospital_corporate_discount.discount_schemes (corporate_client_id);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_schemes_status
    ON hospital_corporate_discount.discount_schemes (status);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_schemes_valid_from
    ON hospital_corporate_discount.discount_schemes (valid_from);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_schemes_valid_to
    ON hospital_corporate_discount.discount_schemes (valid_to);

--changeset easyops:hosp-corp-disc-009-discount-approval-levels
--comment: Phase 3 – discount_approval_levels table (§4.8)
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.discount_approval_levels (
    id UUID PRIMARY KEY,
    discount_scheme_id UUID NOT NULL,
    role_or_group_id VARCHAR(100) NOT NULL,
    max_discount_percent NUMERIC(5,2),
    max_discount_amount NUMERIC(19,4),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hcd_discount_approval_levels_scheme
        FOREIGN KEY (discount_scheme_id) REFERENCES hospital_corporate_discount.discount_schemes(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_discount_approval_levels_scheme
    ON hospital_corporate_discount.discount_approval_levels (discount_scheme_id);

--changeset easyops:hosp-corp-disc-010-discount-decisions
--comment: Phase 3 – discount_decisions table (§4.9)
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.discount_decisions (
    id UUID PRIMARY KEY,
    bill_context_id VARCHAR(255),
    patient_id UUID,
    corporate_client_id UUID,
    discount_scheme_id UUID,
    discount_amount NUMERIC(19,4) NOT NULL,
    discount_percent NUMERIC(5,2),
    decided_by_user_id UUID,
    approved_by_user_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMPTZ,
    CONSTRAINT fk_hcd_discount_decisions_scheme
        FOREIGN KEY (discount_scheme_id) REFERENCES hospital_corporate_discount.discount_schemes(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_discount_decisions_bill_context_id
    ON hospital_corporate_discount.discount_decisions (bill_context_id);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_decisions_patient_id
    ON hospital_corporate_discount.discount_decisions (patient_id);
CREATE INDEX IF NOT EXISTS idx_hcd_discount_decisions_created_at
    ON hospital_corporate_discount.discount_decisions (created_at);
