--liquibase formatted sql

--changeset easyops:073-ehr-fix-preferred-contact-method-enum
--comment: Align chk_preferred_contact with JPA enum names (Text_Message); legacy DB had "Text Message" in CHECK only
ALTER TABLE ehr.patients DROP CONSTRAINT IF EXISTS chk_preferred_contact;

UPDATE ehr.patients
SET preferred_contact_method = 'Text_Message'
WHERE preferred_contact_method = 'Text Message';

ALTER TABLE ehr.patients ADD CONSTRAINT chk_preferred_contact CHECK (
    preferred_contact_method IS NULL
    OR preferred_contact_method IN ('Phone', 'Email', 'Mail', 'Text_Message')
);
