--liquibase formatted sql

--changeset easyops:087-hr-loan-applications
--comment: Phase 2 employee loans: applications, workflow statuses, audit actions (AL-01–AL-03, EL-01–EL-05).

SET search_path TO hr, admin, public;

CREATE TABLE hr.loan_applications (
    application_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    category_id UUID NOT NULL,
    requested_amount NUMERIC(15, 2) NOT NULL,
    requested_tenure_months INTEGER NOT NULL,
    purpose_notes VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    application_date DATE NOT NULL DEFAULT CURRENT_DATE,
    submitted_at TIMESTAMP,
    decided_at TIMESTAMP,
    decided_by_user_id UUID,
    rejection_reason VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_loan_app_organization FOREIGN KEY (organization_id)
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_app_employee FOREIGN KEY (employee_id)
        REFERENCES hr.employees(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_app_category FOREIGN KEY (category_id)
        REFERENCES hr.loan_categories(category_id) ON DELETE RESTRICT,
    CONSTRAINT chk_loan_app_amount CHECK (requested_amount > 0),
    CONSTRAINT chk_loan_app_tenure CHECK (requested_tenure_months > 0)
);

CREATE INDEX idx_loan_app_org ON hr.loan_applications(organization_id);
CREATE INDEX idx_loan_app_employee ON hr.loan_applications(organization_id, employee_id);
CREATE INDEX idx_loan_app_status ON hr.loan_applications(organization_id, status);
CREATE INDEX idx_loan_app_category ON hr.loan_applications(category_id);

COMMENT ON TABLE hr.loan_applications IS 'Employee loan applications (Phase 2).';
COMMENT ON COLUMN hr.loan_applications.status IS 'DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED.';

CREATE TABLE hr.loan_application_actions (
    action_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    actor_user_id UUID,
    comment_text VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_app_action_app FOREIGN KEY (application_id)
        REFERENCES hr.loan_applications(application_id) ON DELETE CASCADE
);

CREATE INDEX idx_loan_app_actions_app ON hr.loan_application_actions(application_id);

COMMENT ON TABLE hr.loan_application_actions IS 'Audit trail for loan application workflow.';
