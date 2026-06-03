--liquibase formatted sql

--changeset easyops:055-pharmacy-p2-fill-status-source
--comment: Pharmacy P2 — distinguish network webhook vs in-house pharmacy fill updates (WS-B)
ALTER TABLE ehr.prescription_transmissions
    ADD COLUMN IF NOT EXISTS fill_status_source VARCHAR(32);

COMMENT ON COLUMN ehr.prescription_transmissions.fill_status_source IS
    'NETWORK_WEBHOOK | IN_HOUSE_PHARMACY — prevents double-apply when both channels exist';
