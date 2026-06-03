--liquibase formatted sql

--changeset easyops:042-prescription-interaction-categories-fr-p1-7
--comment: FR-P1.7 — extend interaction_category for pregnancy/lactation, dosing, organ alerts

ALTER TABLE ehr.prescription_interactions DROP CONSTRAINT IF EXISTS chk_interaction_category;

ALTER TABLE ehr.prescription_interactions ADD CONSTRAINT chk_interaction_category CHECK (interaction_category IN (
    'DRUG_DRUG',
    'DRUG_FOOD',
    'DRUG_LAB',
    'DRUG_HERBAL',
    'DRUG_ALCOHOL',
    'DRUG_DISEASE',
    'PREGNANCY_LACTATION',
    'PEDIATRIC_GERIATRIC_DOSING',
    'WEIGHT_BASED_DOSING',
    'RENAL_HEPATIC_ALERT',
    'OTHER'
));

COMMENT ON COLUMN ehr.prescription_interactions.interaction_category IS 'Includes FR-P1.7 categories: PREGNANCY_LACTATION, PEDIATRIC_GERIATRIC_DOSING, WEIGHT_BASED_DOSING, RENAL_HEPATIC_ALERT';
