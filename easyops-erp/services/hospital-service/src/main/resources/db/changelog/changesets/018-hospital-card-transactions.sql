--liquibase formatted sql

--changeset easyops:hosp-card-006-card-transactions context:hospital-card
--comment: Phase 1.5 – card_transactions for TOPUP/ADJUSTMENT; Phase 2 adds AUTH/CAPTURE
CREATE TABLE IF NOT EXISTS hospital_card.card_transactions (
    id UUID PRIMARY KEY,
    card_account_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    source_system VARCHAR(50),
    external_reference_id VARCHAR(255),
    authorization_id UUID,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    meal_count_delta INT,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    posted_at TIMESTAMPTZ,
    created_by UUID,
    CONSTRAINT fk_card_transactions_account
        FOREIGN KEY (card_account_id) REFERENCES hospital_card.card_accounts(id),
    CONSTRAINT fk_card_transactions_authorization
        FOREIGN KEY (authorization_id) REFERENCES hospital_card.card_transactions(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_card_transactions_idempotency_key
    ON hospital_card.card_transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_card_transactions_card_account_created
    ON hospital_card.card_transactions(card_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_card_transactions_external_ref
    ON hospital_card.card_transactions(external_reference_id, source_system);

--changeset easyops:hosp-card-006b-card-transactions-status-posted context:hospital-card
--comment: Phase 5.3 – index for reconciliation/usage-by-domain (status, posted_at)
CREATE INDEX IF NOT EXISTS idx_card_transactions_status_posted
    ON hospital_card.card_transactions(status, posted_at);
