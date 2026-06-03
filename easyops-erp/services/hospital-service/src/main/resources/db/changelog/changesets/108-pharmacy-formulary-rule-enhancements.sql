--liquibase formatted sql

--changeset easyops:108a-formulary-rules-add-versioning
ALTER TABLE hospital_pharmacy.formulary_rules
    ADD COLUMN IF NOT EXISTS effective_from DATE,
    ADD COLUMN IF NOT EXISTS effective_to DATE,
    ADD COLUMN IF NOT EXISTS rule_set_version VARCHAR(50);

ALTER TABLE hospital_pharmacy.formulary_rules
    ADD CONSTRAINT chk_hp_formulary_effective_dates
    CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from);

--changeset easyops:108b-formulary-alternatives-table
CREATE TABLE IF NOT EXISTS hospital_pharmacy.formulary_alternatives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    formulary_rule_id UUID NOT NULL REFERENCES hospital_pharmacy.formulary_rules(id) ON DELETE CASCADE,
    alternative_drug_id UUID NOT NULL REFERENCES hospital_pharmacy.drugs(id),
    priority INT NOT NULL DEFAULT 0,
    equivalence_class VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hp_formulary_alt_rule ON hospital_pharmacy.formulary_alternatives (formulary_rule_id);
CREATE INDEX IF NOT EXISTS idx_hp_formulary_alt_drug ON hospital_pharmacy.formulary_alternatives (alternative_drug_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_hp_formulary_alt_rule_drug ON hospital_pharmacy.formulary_alternatives (formulary_rule_id, alternative_drug_id);
