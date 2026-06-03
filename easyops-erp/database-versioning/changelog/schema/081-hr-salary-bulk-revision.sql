--liquibase formatted sql

--changeset easyops:081-hr-salary-bulk-revision
--comment: ES-21 Bulk revision (e.g. X% to Basic by grade) with optional approval.

SET search_path TO hr, admin, public;

CREATE TABLE IF NOT EXISTS hr.salary_bulk_revisions (
    bulk_revision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    revision_type VARCHAR(50) NOT NULL DEFAULT 'BULK_PERCENTAGE',
    target_type VARCHAR(20) NOT NULL,
    target_grade_id UUID REFERENCES hr.salary_grades(salary_grade_id) ON DELETE SET NULL,
    target_structure_id UUID REFERENCES hr.salary_structures(salary_structure_id) ON DELETE SET NULL,
    component_code VARCHAR(100) NOT NULL,
    percentage_value DECIMAL(5,2) NOT NULL,
    effective_from DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(100),
    requested_at TIMESTAMP,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    comment VARCHAR(1000),
    rows_applied INT
);

CREATE INDEX IF NOT EXISTS idx_sbr_organization ON hr.salary_bulk_revisions(organization_id);
CREATE INDEX IF NOT EXISTS idx_sbr_status ON hr.salary_bulk_revisions(status);
CREATE INDEX IF NOT EXISTS idx_sbr_effective_from ON hr.salary_bulk_revisions(effective_from);
COMMENT ON TABLE hr.salary_bulk_revisions IS 'ES-21: Bulk salary revision (e.g. X% to Basic by grade) with optional approval.';
