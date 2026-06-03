--liquibase formatted sql

--changeset easyops:057-pharmacy-p2-dispense-billing-posted
--comment: Pharmacy P2 — mark when hospital-pharmacy has posted charges for a dispense order (WS-A idempotency)
ALTER TABLE hospital_pharmacy.dispense_orders
    ADD COLUMN IF NOT EXISTS billing_posted_at TIMESTAMPTZ;

COMMENT ON COLUMN hospital_pharmacy.dispense_orders.billing_posted_at IS
    'Set when charges were posted to hospital-billing (Phase P2); prevents duplicate posting';
