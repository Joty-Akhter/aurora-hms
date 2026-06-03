--liquibase formatted sql
--changeset aurora-hms:107-hr-department-leave-approval-matrix

CREATE TABLE IF NOT EXISTS hr.department_leave_approvers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    department_id UUID NOT NULL,
    step_order INTEGER NOT NULL CHECK (step_order >= 1),
    approver_employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dept_leave_approver_step UNIQUE (department_id, step_order),
    CONSTRAINT uq_dept_leave_approver_employee UNIQUE (department_id, approver_employee_id)
);

CREATE INDEX IF NOT EXISTS idx_dept_leave_appr_org_dept ON hr.department_leave_approvers(organization_id, department_id);

ALTER TABLE hr.leave_requests
    ADD COLUMN IF NOT EXISTS pending_step_index INTEGER NOT NULL DEFAULT 1 CHECK (pending_step_index >= 1),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITHOUT TIME ZONE;
