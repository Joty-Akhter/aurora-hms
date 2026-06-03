--liquibase formatted sql

--changeset easyops:hosp-card-008-status-reason context:hospital-card
--comment: Phase 4.3 – persist status change reason (block/close) for audit
ALTER TABLE hospital_card.cards
    ADD COLUMN status_change_reason VARCHAR(500);
