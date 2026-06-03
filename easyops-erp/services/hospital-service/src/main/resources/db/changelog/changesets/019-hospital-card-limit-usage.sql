--liquibase formatted sql

--changeset easyops:hosp-card-007-card-limit-usage context:hospital-card
--comment: Phase 3.1 – card_limit_usage for daily/monthly and meal/visit limit tracking
CREATE TABLE IF NOT EXISTS hospital_card.card_limit_usage (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    limit_profile_id UUID NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    amount_consumed NUMERIC(19,4) DEFAULT 0,
    meal_count_consumed INT DEFAULT 0,
    visit_count_consumed INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_limit_usage_card
        FOREIGN KEY (card_id) REFERENCES hospital_card.cards(id),
    CONSTRAINT fk_card_limit_usage_limit_profile
        FOREIGN KEY (limit_profile_id) REFERENCES hospital_card.limit_profiles(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_card_limit_usage_card_profile_period
    ON hospital_card.card_limit_usage(card_id, limit_profile_id, period_start, period_end);
