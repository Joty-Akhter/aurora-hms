--liquibase formatted sql

--changeset easyops:100-ep-advice-user-usage
--comment: EP advice catalog — longer lookup values + per-user usage for ranked suggestions

ALTER TABLE ehr.ep_lookup_items ALTER COLUMN value TYPE VARCHAR(1000);

CREATE TABLE IF NOT EXISTS ehr.ep_advice_user_usage (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    advice_lookup_id   UUID         NOT NULL,
    use_count          BIGINT       NOT NULL DEFAULT 1,
    last_used_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ep_advice_usage_lookup FOREIGN KEY (advice_lookup_id)
        REFERENCES ehr.ep_lookup_items (id) ON DELETE CASCADE,
    CONSTRAINT uq_ep_advice_usage_user_lookup UNIQUE (user_id, advice_lookup_id)
);

CREATE INDEX IF NOT EXISTS idx_ep_advice_usage_user ON ehr.ep_advice_user_usage (user_id);
CREATE INDEX IF NOT EXISTS idx_ep_advice_usage_lookup ON ehr.ep_advice_user_usage (advice_lookup_id);
