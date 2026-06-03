--liquibase formatted sql
--changeset aurora-hms:106-hr-leave-staged-workflow

ALTER TABLE hr.leave_requests
    ADD COLUMN IF NOT EXISTS verified_by UUID,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_by UUID;
