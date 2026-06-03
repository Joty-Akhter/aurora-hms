--liquibase formatted sql

--changeset easyops:077-hr-salary-component-statutory-taxability
--comment: SC-20–SC-22 Statutory tags (PF_WAGE, PF_EMPLOYEE, etc.) and taxability (Taxable/Exempt/PartiallyTaxable).

SET search_path TO hr, public;

-- SC-21: Taxability per component (Taxable, Exempt, PartiallyTaxable)
ALTER TABLE hr.salary_components ADD COLUMN IF NOT EXISTS taxability VARCHAR(30);
COMMENT ON COLUMN hr.salary_components.taxability IS 'SC-21: TAXABLE, EXEMPT, or PARTIALLY_TAXABLE for income tax computation.';

-- SC-20: Statutory tags (PF_WAGE, PF_EMPLOYEE, PF_EMPLOYER, TAXABLE, TAX_EXEMPT, ESI_WAGE) – multiple per component
CREATE TABLE IF NOT EXISTS hr.salary_component_statutory_tags (
    component_id UUID NOT NULL REFERENCES hr.salary_components(component_id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (component_id, tag)
);
COMMENT ON TABLE hr.salary_component_statutory_tags IS 'SC-20: Statutory tags e.g. PF_WAGE, PF_EMPLOYEE, TAXABLE, ESI_WAGE. PF wage = sum of components with PF_WAGE (SC-22).';
CREATE INDEX IF NOT EXISTS idx_salary_component_statutory_tags_tag ON hr.salary_component_statutory_tags(tag);
CREATE INDEX IF NOT EXISTS idx_salary_component_statutory_tags_component ON hr.salary_component_statutory_tags(component_id);
