--liquibase formatted sql

--changeset easyops:056-pharmacy-p2-in-house-fill-events
--comment: Pharmacy P2 — idempotent replay for in-house dispense → EHR fill sync
CREATE TABLE IF NOT EXISTS ehr.in_house_dispense_fill_events (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    prescription_id UUID NOT NULL,
    dispense_order_id UUID NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_in_house_fill_prescription
    ON ehr.in_house_dispense_fill_events (prescription_id);
