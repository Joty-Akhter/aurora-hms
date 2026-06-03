--liquibase formatted sql

-- FR-P3.11a: Per-integration shared secrets for fill-status webhook HMAC-SHA256 authentication.
-- Each pharmacy or e-prescribing network that calls POST /api/prescriptions/transmissions/fill-status
-- must present an X-Webhook-Integration-Id header identifying which integration record to look up,
-- and an X-Webhook-Signature: sha256=<hex> header computed from HMAC-SHA256(secret, request_body).

--changeset hospital-service:045-webhook-integrations-table
CREATE TABLE IF NOT EXISTS ehr.webhook_integrations (
    integration_id   UUID         NOT NULL DEFAULT gen_random_uuid(),
    integration_name VARCHAR(100) NOT NULL,
    integration_type VARCHAR(50)  NOT NULL DEFAULT 'FILL_STATUS_CALLBACK',

    -- Shared secret used to verify HMAC-SHA256 signatures.
    -- In production, encrypt this column with application-level AES-256 using
    -- a key sourced from a secrets manager (e.g., AWS KMS, HashiCorp Vault).
    -- At minimum the database must be encrypted at rest (PG transparent encryption
    -- or cloud-provider managed encryption).
    secret           TEXT         NOT NULL,

    -- Optional comma-separated CIDR IP allowlist (secondary control).
    -- NULL means "any IP is permitted" (HMAC is still required).
    allowed_ips      TEXT,

    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       UUID,

    CONSTRAINT pk_webhook_integrations PRIMARY KEY (integration_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_integrations_active
    ON ehr.webhook_integrations (is_active)
    WHERE is_active = TRUE;

COMMENT ON TABLE  ehr.webhook_integrations IS
    'FR-P3.11a: Shared secrets for inbound pharmacy fill-status webhook authentication (HMAC-SHA256).';
COMMENT ON COLUMN ehr.webhook_integrations.secret IS
    'Plain or application-AES-encrypted shared secret. Never log or expose in API responses.';
COMMENT ON COLUMN ehr.webhook_integrations.allowed_ips IS
    'Optional secondary IP allowlist; HMAC validation is always required regardless.';
COMMENT ON COLUMN ehr.webhook_integrations.integration_type IS
    'Discriminator for future webhook types; currently always FILL_STATUS_CALLBACK.';

--changeset hospital-service:045-webhook-integrations-default-seed
-- Seed a placeholder default integration.
-- Replace secret with a cryptographically random value (min 32 bytes / 64 hex chars) in each environment.
-- This row is used when callers omit X-Webhook-Integration-Id.
INSERT INTO ehr.webhook_integrations (integration_id, integration_name, integration_type, secret, is_active)
VALUES (
    'aaaaaaaa-0000-0000-0000-000000000001'::uuid,
    'default',
    'FILL_STATUS_CALLBACK',
    'REPLACE_WITH_RANDOM_SECRET_BEFORE_GO_LIVE',
    TRUE
)
ON CONFLICT DO NOTHING;
