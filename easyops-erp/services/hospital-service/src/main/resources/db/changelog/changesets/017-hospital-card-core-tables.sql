--liquibase formatted sql

--changeset easyops:hosp-card-002-limit-profiles context:hospital-card
--comment: Phase 1 – limit_profiles (no FK to other card tables)
CREATE TABLE IF NOT EXISTS hospital_card.limit_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    daily_amount_limit NUMERIC(19,4),
    monthly_amount_limit NUMERIC(19,4),
    daily_meal_limit INT,
    daily_visit_limit INT,
    reset_policy VARCHAR(30) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_limit_profiles_name ON hospital_card.limit_profiles(name);

--changeset easyops:hosp-card-003-card-products context:hospital-card
--comment: Phase 1 – card_products
CREATE TABLE IF NOT EXISTS hospital_card.card_products (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    medium_type VARCHAR(30),
    usage_domains VARCHAR(100),
    default_limit_profile_id UUID,
    validity_start_date DATE,
    validity_end_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_card_products_limit_profile
        FOREIGN KEY (default_limit_profile_id) REFERENCES hospital_card.limit_profiles(id)
);

CREATE INDEX IF NOT EXISTS idx_card_products_code ON hospital_card.card_products(code);
CREATE INDEX IF NOT EXISTS idx_card_products_status ON hospital_card.card_products(status);

--changeset easyops:hosp-card-004-cards context:hospital-card
--comment: Phase 1 – cards
CREATE TABLE IF NOT EXISTS hospital_card.cards (
    id UUID PRIMARY KEY,
    card_number VARCHAR(100) UNIQUE NOT NULL,
    physical_serial VARCHAR(100),
    card_product_id UUID NOT NULL,
    limit_profile_id UUID,
    owner_type VARCHAR(30) NOT NULL,
    owner_reference_id VARCHAR(255) NOT NULL,
    corporate_id UUID,
    status VARCHAR(30) NOT NULL,
    replaced_by_card_id UUID,
    issued_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMPTZ,
    blocked_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_cards_card_product
        FOREIGN KEY (card_product_id) REFERENCES hospital_card.card_products(id),
    CONSTRAINT fk_cards_limit_profile
        FOREIGN KEY (limit_profile_id) REFERENCES hospital_card.limit_profiles(id),
    CONSTRAINT fk_cards_replaced_by
        FOREIGN KEY (replaced_by_card_id) REFERENCES hospital_card.cards(id)
);

CREATE INDEX IF NOT EXISTS idx_cards_card_number ON hospital_card.cards(card_number);
CREATE INDEX IF NOT EXISTS idx_cards_owner_type_ref ON hospital_card.cards(owner_type, owner_reference_id);
CREATE INDEX IF NOT EXISTS idx_cards_corporate_id ON hospital_card.cards(corporate_id);
CREATE INDEX IF NOT EXISTS idx_cards_status ON hospital_card.cards(status);
CREATE INDEX IF NOT EXISTS idx_cards_card_product_id ON hospital_card.cards(card_product_id);

--changeset easyops:hosp-card-005-card-accounts context:hospital-card
--comment: Phase 1 – card_accounts
CREATE TABLE IF NOT EXISTS hospital_card.card_accounts (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    current_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit_limit NUMERIC(19,4),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_accounts_card
        FOREIGN KEY (card_id) REFERENCES hospital_card.cards(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_card_accounts_card_id ON hospital_card.card_accounts(card_id);
