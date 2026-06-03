--liquibase formatted sql

--changeset easyops:041-ep-prescription-encounter-mode
--comment: Persist OPD vs IPD session context on prescriptions (EP-1 / EP-11)

ALTER TABLE ehr.prescriptions ADD COLUMN IF NOT EXISTS ep_encounter_mode VARCHAR(8);

ALTER TABLE ehr.prescriptions DROP CONSTRAINT IF EXISTS chk_prescriptions_ep_encounter_mode;

ALTER TABLE ehr.prescriptions ADD CONSTRAINT chk_prescriptions_ep_encounter_mode
    CHECK (ep_encounter_mode IS NULL OR ep_encounter_mode IN ('OPD', 'IPD'));
