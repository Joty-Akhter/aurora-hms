--liquibase formatted sql

-- FR-P3.5: Pharmacy Directory — first-class master data entity for retail / e-prescribing pharmacies.
-- Lives in hospital_pharmacy schema alongside the drug catalog.
-- ehr.prescriptions continues to denormalise snapshot fields (name, NPI, phone) for legal/audit
-- immutability; pharmacy_id is a soft link to this table, not a hard FK.

--changeset hospital-service:045-pharmacy-directory-table
--comment: FR-P3.5 — Create hospital_pharmacy.pharmacy_directory master data table
CREATE TABLE hospital_pharmacy.pharmacy_directory (
    id                       UUID          NOT NULL DEFAULT gen_random_uuid(),
    name                     VARCHAR(255)  NOT NULL,
    -- NPI-2 (10-digit Organisation NPI assigned by CMS)
    npi                      VARCHAR(10),
    -- NCPDP/Surescripts pharmacy ID (up to 7 digits, zero-padded)
    ncpdp_id                 VARCHAR(15),
    address_line1            VARCHAR(255),
    address_line2            VARCHAR(255),
    city                     VARCHAR(100),
    state                    VARCHAR(50),
    zip                      VARCHAR(20),
    country                  VARCHAR(100)  NOT NULL DEFAULT 'US',
    phone                    VARCHAR(30),
    fax                      VARCHAR(30),
    email                    VARCHAR(255),
    -- E-prescribing capability flags
    is_eprescribing_capable  BOOLEAN       NOT NULL DEFAULT FALSE,
    eprescribing_network     VARCHAR(100),                        -- e.g. SURESCRIPTS, RCOPIA, RXHUB
    -- Data provenance & staleness tracking
    data_source              VARCHAR(50)   NOT NULL DEFAULT 'MANUAL',  -- MANUAL | NCPDP_FEED | SURESCRIPTS | IMPORTED
    last_verified_at         TIMESTAMP WITH TIME ZONE,
    verification_notes       TEXT,
    -- Soft-delete / active flag
    is_active                BOOLEAN       NOT NULL DEFAULT TRUE,
    -- Audit
    notes                    TEXT,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by               UUID,
    updated_by               UUID,

    CONSTRAINT pk_pharmacy_directory PRIMARY KEY (id),
    CONSTRAINT uq_pharmacy_directory_npi UNIQUE (npi),
    CONSTRAINT chk_pharmacy_directory_data_source
        CHECK (data_source IN ('MANUAL','NCPDP_FEED','SURESCRIPTS','IMPORTED'))
);

CREATE INDEX idx_pharm_dir_name       ON hospital_pharmacy.pharmacy_directory (LOWER(name));
CREATE INDEX idx_pharm_dir_npi        ON hospital_pharmacy.pharmacy_directory (npi);
CREATE INDEX idx_pharm_dir_ncpdp      ON hospital_pharmacy.pharmacy_directory (ncpdp_id);
CREATE INDEX idx_pharm_dir_city_state ON hospital_pharmacy.pharmacy_directory (city, state);
CREATE INDEX idx_pharm_dir_active     ON hospital_pharmacy.pharmacy_directory (is_active);
CREATE INDEX idx_pharm_dir_verified   ON hospital_pharmacy.pharmacy_directory (last_verified_at);

COMMENT ON TABLE  hospital_pharmacy.pharmacy_directory IS 'FR-P3.5: Master data for retail / mail-order / e-prescribing pharmacies. ehr.prescriptions.pharmacy_id is a soft reference to this table.';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.npi                    IS 'CMS-assigned 10-digit Organisation NPI (NPI-2). Must be unique when present.';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.ncpdp_id               IS 'NCPDP/Surescripts pharmacy identifier (up to 7 digits).';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.is_eprescribing_capable IS 'TRUE when the pharmacy is enrolled in an e-prescribing network and can receive electronic Rx.';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.eprescribing_network   IS 'Network identifier (e.g. SURESCRIPTS) when is_eprescribing_capable = TRUE.';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.data_source            IS 'How the record was sourced: MANUAL entry, NCPDP_FEED import, SURESCRIPTS sync, or IMPORTED bulk load.';
COMMENT ON COLUMN hospital_pharmacy.pharmacy_directory.last_verified_at       IS 'Timestamp of last data-quality verification. Records older than 90 days are flagged as stale.';

--changeset hospital-service:045-pharmacy-directory-trigger splitStatements:false
--comment: Auto-update updated_at on pharmacy_directory (splitStatements:false so $$ function body is not split on inner semicolons)
CREATE OR REPLACE FUNCTION hospital_pharmacy.fn_pharmacy_directory_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pharmacy_directory_updated_at
    BEFORE UPDATE ON hospital_pharmacy.pharmacy_directory
    FOR EACH ROW EXECUTE FUNCTION hospital_pharmacy.fn_pharmacy_directory_updated_at();
--rollback DROP TRIGGER trg_pharmacy_directory_updated_at ON hospital_pharmacy.pharmacy_directory;
--rollback DROP FUNCTION hospital_pharmacy.fn_pharmacy_directory_updated_at();
--rollback DROP TABLE hospital_pharmacy.pharmacy_directory;
