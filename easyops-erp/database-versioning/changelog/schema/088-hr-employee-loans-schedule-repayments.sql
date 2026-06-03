--liquibase formatted sql

--changeset easyops:088-hr-employee-loans-schedule-repayments
--comment: Phase 3: loan accounts, installments, manual repayments (AL-04, AL-05, RP-01–RP-03, RP-06).

SET search_path TO hr, admin, public;

CREATE TABLE hr.employee_loans (
    loan_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    loan_application_id UUID,
    category_id UUID NOT NULL,
    principal_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    outstanding_balance NUMERIC(15, 2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    disbursement_date DATE,
    disbursed_amount NUMERIC(15, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_employee_loans_org FOREIGN KEY (organization_id)
        REFERENCES admin.organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_loans_employee FOREIGN KEY (employee_id)
        REFERENCES hr.employees(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_loans_application FOREIGN KEY (loan_application_id)
        REFERENCES hr.loan_applications(application_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_loans_category FOREIGN KEY (category_id)
        REFERENCES hr.loan_categories(category_id) ON DELETE RESTRICT,
    CONSTRAINT uk_employee_loans_application UNIQUE (loan_application_id),
    CONSTRAINT chk_employee_loans_principal CHECK (principal_amount > 0),
    CONSTRAINT chk_employee_loans_tenure CHECK (tenure_months > 0),
    CONSTRAINT chk_employee_loans_outstanding CHECK (outstanding_balance >= 0)
);

CREATE INDEX idx_employee_loans_org ON hr.employee_loans(organization_id);
CREATE INDEX idx_employee_loans_employee ON hr.employee_loans(organization_id, employee_id);
CREATE INDEX idx_employee_loans_status ON hr.employee_loans(organization_id, status);

COMMENT ON TABLE hr.employee_loans IS 'Active/closed loan accounts (Phase 3).';
COMMENT ON COLUMN hr.employee_loans.status IS 'PENDING_DISBURSEMENT, ACTIVE, CLOSED.';

CREATE TABLE hr.loan_installments (
    installment_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    scheduled_amount NUMERIC(15, 2) NOT NULL,
    paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DUE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_installment_loan FOREIGN KEY (loan_id)
        REFERENCES hr.employee_loans(loan_id) ON DELETE CASCADE,
    CONSTRAINT uk_loan_installment_seq UNIQUE (loan_id, sequence_number),
    CONSTRAINT chk_loan_inst_scheduled CHECK (scheduled_amount > 0),
    CONSTRAINT chk_loan_inst_paid CHECK (paid_amount >= 0 AND paid_amount <= scheduled_amount)
);

CREATE INDEX idx_loan_installments_loan ON hr.loan_installments(loan_id);
CREATE INDEX idx_loan_installments_due ON hr.loan_installments(loan_id, due_date);

COMMENT ON COLUMN hr.loan_installments.status IS 'DUE, PARTIAL, PAID.';

CREATE TABLE hr.loan_repayment_transactions (
    transaction_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_date DATE NOT NULL,
    source VARCHAR(20) NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT fk_loan_repay_tx_loan FOREIGN KEY (loan_id)
        REFERENCES hr.employee_loans(loan_id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_repay_amount CHECK (amount > 0)
);

CREATE INDEX idx_loan_repay_tx_loan ON hr.loan_repayment_transactions(loan_id);

COMMENT ON COLUMN hr.loan_repayment_transactions.source IS 'MANUAL, PAYROLL.';

CREATE TABLE hr.loan_repayment_allocations (
    allocation_id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    CONSTRAINT fk_loan_repay_alloc_tx FOREIGN KEY (transaction_id)
        REFERENCES hr.loan_repayment_transactions(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_repay_alloc_inst FOREIGN KEY (installment_id)
        REFERENCES hr.loan_installments(installment_id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_repay_alloc_amount CHECK (amount > 0)
);

CREATE INDEX idx_loan_repay_alloc_tx ON hr.loan_repayment_allocations(transaction_id);
CREATE INDEX idx_loan_repay_alloc_inst ON hr.loan_repayment_allocations(installment_id);
