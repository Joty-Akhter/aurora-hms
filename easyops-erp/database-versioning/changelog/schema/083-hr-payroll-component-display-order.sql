--liquibase formatted sql

--changeset easyops:083-hr-payroll-component-display-order
--comment: ES-26 Payslip with component order; store display_order on payroll_components.

SET search_path TO hr, admin, public;

ALTER TABLE hr.payroll_components ADD COLUMN IF NOT EXISTS display_order INT DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_payroll_component_display ON hr.payroll_components(payroll_detail_id, display_order);
COMMENT ON COLUMN hr.payroll_components.display_order IS 'ES-26: Order for payslip display; copied from salary_component at calculation time.';
