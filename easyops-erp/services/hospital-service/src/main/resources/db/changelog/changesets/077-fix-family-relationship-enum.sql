--liquibase formatted sql

--changeset easyops:077-fix-family-relationship-enum splitStatements:true
--comment: Align chk_family_relationship with JPA enum names (underscores instead of spaces for compound values)
ALTER TABLE ehr.family_history DROP CONSTRAINT IF EXISTS chk_family_relationship;

UPDATE ehr.family_history SET family_member_relationship = 'Maternal_Grandmother' WHERE family_member_relationship = 'Maternal Grandmother';
UPDATE ehr.family_history SET family_member_relationship = 'Paternal_Grandmother' WHERE family_member_relationship = 'Paternal Grandmother';
UPDATE ehr.family_history SET family_member_relationship = 'Maternal_Grandfather' WHERE family_member_relationship = 'Maternal Grandfather';
UPDATE ehr.family_history SET family_member_relationship = 'Paternal_Grandfather' WHERE family_member_relationship = 'Paternal Grandfather';

ALTER TABLE ehr.family_history ADD CONSTRAINT chk_family_relationship CHECK (
    family_member_relationship IN (
        'Mother', 'Father', 'Sister', 'Brother',
        'Maternal_Grandmother', 'Maternal_Grandfather',
        'Paternal_Grandmother', 'Paternal_Grandfather',
        'Aunt', 'Uncle', 'Cousin', 'Other'
    )
);
