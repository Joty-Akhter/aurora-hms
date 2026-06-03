--liquibase formatted sql

--changeset easyops:hosp-corp-disc-064-corporate-cards
--comment: Phase 3 - corporate benefit cards registry mapping in corporate service
CREATE TABLE IF NOT EXISTS hospital_corporate_discount.corporate_cards (
    id UUID PRIMARY KEY,
    corporate_client_id UUID NOT NULL,
    contract_id UUID NULL,
    holder_name VARCHAR(255) NOT NULL,
    holder_identifier VARCHAR(255) NOT NULL,
    card_type VARCHAR(100) NOT NULL,
    card_product_id UUID NOT NULL,
    card_id UUID NOT NULL,
    card_number VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    replaced_by_corporate_card_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NULL,
    CONSTRAINT fk_hcd_corporate_cards_client
        FOREIGN KEY (corporate_client_id) REFERENCES hospital_corporate_discount.corporate_clients(id),
    CONSTRAINT fk_hcd_corporate_cards_contract
        FOREIGN KEY (contract_id) REFERENCES hospital_corporate_discount.corporate_contracts(id),
    CONSTRAINT fk_hcd_corporate_cards_replaced_by
        FOREIGN KEY (replaced_by_corporate_card_id) REFERENCES hospital_corporate_discount.corporate_cards(id)
);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_cards_client
    ON hospital_corporate_discount.corporate_cards (corporate_client_id);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_cards_contract
    ON hospital_corporate_discount.corporate_cards (contract_id);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_cards_card_id
    ON hospital_corporate_discount.corporate_cards (card_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hcd_corporate_cards_card_number
    ON hospital_corporate_discount.corporate_cards (card_number);

CREATE INDEX IF NOT EXISTS idx_hcd_corporate_cards_status
    ON hospital_corporate_discount.corporate_cards (status);
