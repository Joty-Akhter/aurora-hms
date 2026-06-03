--liquibase formatted sql

--changeset easyops:hosp-bill-005-invoice-discount-lines context:hospital-billing
--comment: Invoice-level discount lines (in addition to charge-line discounts) for getAppliedDiscounts
CREATE TABLE IF NOT EXISTS hospital_billing.invoice_discount_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    description VARCHAR(500),
    source VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hb_invoice_discount_lines_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id)
);

CREATE INDEX IF NOT EXISTS idx_hb_invoice_discount_lines_invoice
    ON hospital_billing.invoice_discount_lines (invoice_id);

--changeset easyops:hosp-bill-006-discount-audit-log context:hospital-billing
--comment: Audit trail for discount applications (who, which card/corporate, what discount, which invoice/line)
CREATE TABLE IF NOT EXISTS hospital_billing.discount_audit_log (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    charge_line_id UUID,
    discount_amount NUMERIC(19,4) NOT NULL,
    source VARCHAR(50) NOT NULL,
    corporate_contract_id UUID,
    card_reference VARCHAR(100),
    applied_by_user_id UUID,
    applied_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hb_discount_audit_invoice
        FOREIGN KEY (invoice_id) REFERENCES hospital_billing.invoices(id),
    CONSTRAINT fk_hb_discount_audit_charge_line
        FOREIGN KEY (charge_line_id) REFERENCES hospital_billing.charge_lines(id)
);

CREATE INDEX IF NOT EXISTS idx_hb_discount_audit_invoice
    ON hospital_billing.discount_audit_log (invoice_id);
CREATE INDEX IF NOT EXISTS idx_hb_discount_audit_charge_line
    ON hospital_billing.discount_audit_log (charge_line_id);
CREATE INDEX IF NOT EXISTS idx_hb_discount_audit_applied_at
    ON hospital_billing.discount_audit_log (applied_at);
