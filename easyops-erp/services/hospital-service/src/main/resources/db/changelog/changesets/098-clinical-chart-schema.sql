-- Clinical Chart master (legacy charge / investigation catalog) — used for billing alignment and EP test/advice suggestions.

CREATE TABLE IF NOT EXISTS hospital.clinical_chart_items (
    clinical_chart_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_row_id        BIGINT NOT NULL,
    pcode                VARCHAR(64),
    description          TEXT NOT NULL,
    charge               NUMERIC(18, 4),
    dept_name            VARCHAR(255),
    sub_dept_name        VARCHAR(255),
    sub_sub_dept_name    VARCHAR(255),
    report_group_name    VARCHAR(255),
    out_test             SMALLINT NOT NULL DEFAULT 0,
    status_legacy        SMALLINT NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clinical_chart_legacy_row UNIQUE (legacy_row_id)
);

CREATE INDEX IF NOT EXISTS idx_clinical_chart_items_active_desc
    ON hospital.clinical_chart_items (status_legacy, description);

CREATE INDEX IF NOT EXISTS idx_clinical_chart_items_pcode
    ON hospital.clinical_chart_items (pcode);
