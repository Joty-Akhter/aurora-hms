--liquibase formatted sql

--changeset easyops:051-ep-workspace-audit-drop-snapshot-json
--comment: Req-I3 / EP-10 HIPAA compliance — drop snapshot_json from ep_doctor_workspace_audit.
-- The column was introduced in changeset 040 to optionally store a workspace JSON snapshot
-- alongside each audit event.  Under EP-10, audit rows must contain only access/mutation
-- metadata (action, userId, organizationId, version, ip_address, http_status, timestamp).
-- Storing the workspace JSON blob — which may contain PHI — in the audit table is prohibited
-- for both READ and WRITE operations.  The Java entity (EpDoctorWorkspaceAudit) was already
-- updated to remove the snapshotJson field; this migration removes the column from the database
-- so the prohibition is enforced at the storage layer and cannot be bypassed by raw SQL.

ALTER TABLE ehr.ep_doctor_workspace_audit
    DROP COLUMN IF EXISTS snapshot_json;
