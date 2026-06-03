--liquibase formatted sql

--changeset easyops:050-ep-workspace-audit-hipaa-columns
--comment: EP-10 HIPAA — add organization_id, ip_address, http_status to ep_doctor_workspace_audit; make snapshot_json nullable (already is) and stop requiring it

ALTER TABLE ehr.ep_doctor_workspace_audit
    ADD COLUMN IF NOT EXISTS organization_id UUID,
    ADD COLUMN IF NOT EXISTS ip_address      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS http_status     INTEGER;

-- workspace_id FK may be null for READ audits where no workspace row exists yet
ALTER TABLE ehr.ep_doctor_workspace_audit
    ALTER COLUMN workspace_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ep_workspace_audit_org
    ON ehr.ep_doctor_workspace_audit (organization_id);

CREATE INDEX IF NOT EXISTS idx_ep_workspace_audit_user
    ON ehr.ep_doctor_workspace_audit (user_id, created_at DESC);
