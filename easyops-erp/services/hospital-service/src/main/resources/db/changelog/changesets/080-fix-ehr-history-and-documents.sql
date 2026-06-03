--liquibase formatted sql

--changeset easyops:080-fix-ehr-history-and-documents splitStatements:false
--comment: Fix family_history relationship constraint values and normalize patient_documents type/category compatibility.

DO $$
BEGIN
    IF to_regclass('ehr.family_history') IS NOT NULL THEN
        -- Normalize any legacy values that used spaces instead of enum-style underscores.
        UPDATE ehr.family_history
        SET family_member_relationship = REPLACE(family_member_relationship, ' ', '_')
        WHERE family_member_relationship IN (
            'Maternal Grandmother',
            'Maternal Grandfather',
            'Paternal Grandmother',
            'Paternal Grandfather'
        );

        ALTER TABLE ehr.family_history
            DROP CONSTRAINT IF EXISTS chk_family_relationship;

        ALTER TABLE ehr.family_history
            ADD CONSTRAINT chk_family_relationship CHECK (
                family_member_relationship IN (
                    'Mother', 'Father', 'Sister', 'Brother',
                    'Maternal_Grandmother', 'Maternal_Grandfather',
                    'Paternal_Grandmother', 'Paternal_Grandfather',
                    'Aunt', 'Uncle', 'Cousin', 'Other'
                )
            );
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('ehr.patient_documents') IS NOT NULL THEN
        -- Ensure schema matches entity mapping even on environments with older table definition.
        ALTER TABLE ehr.patient_documents
            ADD COLUMN IF NOT EXISTS document_category VARCHAR(100);

        -- Coerce legacy/unknown values to OTHER to avoid enum parsing/query failures.
        UPDATE ehr.patient_documents
        SET document_type = 'OTHER'
        WHERE document_type IS NULL
           OR document_type NOT IN (
               'PATHOLOGY_REPORT',
               'RADIOLOGY_REPORT',
               'LAB_REPORT',
               'CLINICAL_PHOTO',
               'SURGICAL_REPORT',
               'PRESCRIPTION',
               'REFERRAL_LETTER',
               'DISCHARGE_SUMMARY',
               'CONSENT_FORM',
               'INSURANCE_DOCUMENT',
               'VITAL_RECORDS',
               'IDENTITY_DOCUMENT',
               'EXTERNAL_RECORD',
               'ADVANCE_DIRECTIVE',
               'OTHER'
           );
    END IF;
END $$;
