--liquibase formatted sql

--changeset easyops:074-hr-salary-component-payslip-label-currency
--comment: SC-07, SC-08 – Add payslip_label (short name for payslip) and optional currency to salary_components.

SET search_path TO hr, public;

-- SC-07: Optional short name / payslip label for display on payslips (e.g. "Basic Sal", "HRA"). If not provided, name is used.
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS payslip_label VARCHAR(100);
COMMENT ON COLUMN hr.salary_components.payslip_label IS 'Optional short label for payslip display (SC-07). If null, component_name is used.';

-- SC-08: Optional currency per component; default from organization or structure when used in payroll.
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
COMMENT ON COLUMN hr.salary_components.currency IS 'Optional currency for this component (SC-08). When null, structure or organization default is used in payroll.';
