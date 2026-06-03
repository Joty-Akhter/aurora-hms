--liquibase formatted sql

--changeset easyops:040-easy-prescription-workspace
--comment: Server-side Easy Prescription workspace (templates, config, drafts blob) + audit trail

CREATE TABLE IF NOT EXISTS ehr.ep_doctor_workspace (
    workspace_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    version INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ep_workspace_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_ep_workspace_user ON ehr.ep_doctor_workspace (user_id);
CREATE INDEX IF NOT EXISTS idx_ep_workspace_org ON ehr.ep_doctor_workspace (organization_id);

CREATE TABLE IF NOT EXISTS ehr.ep_doctor_workspace_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES ehr.ep_doctor_workspace(workspace_id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    action VARCHAR(32) NOT NULL,
    snapshot_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ep_workspace_audit_ws ON ehr.ep_doctor_workspace_audit (workspace_id);
CREATE INDEX IF NOT EXISTS idx_ep_workspace_audit_created ON ehr.ep_doctor_workspace_audit (created_at DESC);
