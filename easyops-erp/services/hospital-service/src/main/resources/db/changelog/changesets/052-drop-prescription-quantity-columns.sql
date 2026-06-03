--liquibase formatted sql

--changeset hospital-service:052-drop-prescription-quantity-columns
--comment: Remove prescribed quantity/unit; derive from frequency × duration (Bangladesh EP workflow)

ALTER TABLE ehr.prescription_medications DROP COLUMN IF EXISTS quantity;
ALTER TABLE ehr.prescription_medications DROP COLUMN IF EXISTS quantity_unit;

ALTER TABLE ehr.prescriptions DROP COLUMN IF EXISTS quantity;
ALTER TABLE ehr.prescriptions DROP COLUMN IF EXISTS quantity_unit;
