--liquibase formatted sql

--changeset easyops:112-create-api-keys-table context:schema
--comment: API key table for third-party integrations — hashed keys mapped to service accounts
CREATE TABLE IF NOT EXISTS users.api_keys (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    organization_id UUID,
    key_hash        VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMPTZ,
    last_used_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_api_keys_key_hash ON users.api_keys (key_hash);
CREATE INDEX IF NOT EXISTS ix_api_keys_user_id ON users.api_keys (user_id);
