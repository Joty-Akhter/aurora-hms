--liquibase formatted sql

--changeset easyops:082-hr-salary-component-proration
--comment: ES-28–ES-30 Proration: rule per component (BY_DAYS, NO_PRORATION, BY_HOURS). Join/relieving from employee master (hire_date, termination_date).

SET search_path TO hr, admin, public;

ALTER TABLE hr.salary_components
    ADD COLUMN IF NOT EXISTS proration_rule VARCHAR(30) NOT NULL DEFAULT 'BY_DAYS';

COMMENT ON COLUMN hr.salary_components.proration_rule IS 'ES-29: BY_DAYS | NO_PRORATION | BY_HOURS. Default per component (e.g. Basic prorated, fixed allowance not).';
