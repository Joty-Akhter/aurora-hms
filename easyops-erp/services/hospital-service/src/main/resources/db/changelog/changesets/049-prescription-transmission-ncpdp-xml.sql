--liquibase formatted sql

-- FR-P3.6: Store the raw NCPDP SCRIPT 2017071 XML payload alongside the transmission record.
-- ncpdp_xml_payload: the full validated XML string sent (or that would have been sent) to the network.
-- ncpdp_message_id:  the MessageID from the NCPDP Header, for cross-reference / audit with the network.
--
-- LIQUIBASE ID LOCK (Gap N3): This file is named 049-*.sql but the changeset id below is
-- hospital-service:048-prescription-transmission-ncpdp-xml (not 049-...). It is distinct from
-- hospital-service:048-ep-encounter-mode-immutable-trigger in 048-ep-encounter-mode-immutable.sql.
-- The id is frozen in DATABASECHANGELOG — do NOT rename to match the filename or Liquibase will re-run
-- and fail on existing columns. See db/changelog/README-LIQUIBASE-CHANGESET-IDS.md

--changeset hospital-service:048-prescription-transmission-ncpdp-xml
ALTER TABLE ehr.prescription_transmissions
    ADD COLUMN IF NOT EXISTS ncpdp_xml_payload TEXT,
    ADD COLUMN IF NOT EXISTS ncpdp_message_id  VARCHAR(100);

COMMENT ON COLUMN ehr.prescription_transmissions.ncpdp_xml_payload IS
    'FR-P3.6: Full NCPDP SCRIPT 2017071 NewRx XML string that was built and validated before transmission.';
COMMENT ON COLUMN ehr.prescription_transmissions.ncpdp_message_id IS
    'FR-P3.6: MessageID from the NCPDP SCRIPT Header — used for audit correlation with the e-prescribing network.';

CREATE INDEX IF NOT EXISTS idx_prescription_transmissions_ncpdp_message_id
    ON ehr.prescription_transmissions (ncpdp_message_id)
    WHERE ncpdp_message_id IS NOT NULL;
