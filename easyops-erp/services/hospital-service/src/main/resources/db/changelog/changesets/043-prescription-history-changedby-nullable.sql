--liquibase formatted sql

--changeset hospital-service:043-prescription-history-changedby-nullable
-- changed_by is legitimately absent when a prescription history record is created by an
-- inbound pharmacy/network webhook (fill-status updates) rather than by a user action.
-- Relax the NOT NULL constraint so webhook-driven records persist correctly.
ALTER TABLE ehr.prescription_history
    ALTER COLUMN changed_by DROP NOT NULL;

--rollback ALTER TABLE ehr.prescription_history ALTER COLUMN changed_by SET NOT NULL;
