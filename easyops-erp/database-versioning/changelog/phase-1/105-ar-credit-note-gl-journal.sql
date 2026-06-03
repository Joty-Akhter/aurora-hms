--liquibase formatted sql

--changeset easyops:105-ar-credit-note-gl-journal context:ar
--comment: Link AR credit notes to posted GL journals
ALTER TABLE accounting.ar_credit_notes
    ADD COLUMN IF NOT EXISTS gl_journal_id UUID REFERENCES accounting.journal_entries(id);

CREATE INDEX IF NOT EXISTS idx_ar_credit_notes_gl_journal
    ON accounting.ar_credit_notes(gl_journal_id);
