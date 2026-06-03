--liquibase formatted sql

--changeset easyops:078-fix-gender-enum splitStatements:true
--comment: Align chk_gender with JPA enum name (Prefer_not_to_answer vs "Prefer not to answer"); fixes 500 on patient load
ALTER TABLE ehr.patients DROP CONSTRAINT IF EXISTS chk_gender;

UPDATE ehr.patients SET gender = 'Prefer_not_to_answer' WHERE gender = 'Prefer not to answer';

ALTER TABLE ehr.patients ADD CONSTRAINT chk_gender CHECK (
    gender IS NULL OR gender IN ('Male', 'Female', 'Other', 'Prefer_not_to_answer')
);
