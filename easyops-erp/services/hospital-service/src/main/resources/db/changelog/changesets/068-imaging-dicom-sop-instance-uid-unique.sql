-- Enforce uniqueness of DICOM SOP Instance UID when present (upload + C-GET ingest deduplication).
-- Multiple NULL/empty rows may still exist for legacy non-DICOM attachments; non-null values must be unique.
CREATE UNIQUE INDEX IF NOT EXISTS uq_imaging_attachments_dicom_sop_instance_uid
    ON ehr.imaging_image_attachments (dicom_sop_instance_uid)
    WHERE dicom_sop_instance_uid IS NOT NULL AND length(trim(dicom_sop_instance_uid)) > 0;
