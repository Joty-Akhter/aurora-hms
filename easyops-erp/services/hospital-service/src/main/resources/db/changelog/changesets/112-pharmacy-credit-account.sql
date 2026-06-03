--liquibase formatted sql

--changeset easyops:112a-pharmacy-credit-accounts
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_credit_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    customer_name VARCHAR(255),
    credit_limit NUMERIC(12,2) NOT NULL DEFAULT 0,
    outstanding_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_ca_credit_limit CHECK (credit_limit >= 0),
    CONSTRAINT chk_hp_ca_balance CHECK (outstanding_balance >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_hp_credit_account_patient ON hospital_pharmacy.pharmacy_credit_accounts (patient_id);

--changeset easyops:112b-pharmacy-payments
CREATE TABLE IF NOT EXISTS hospital_pharmacy.pharmacy_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_account_id UUID NOT NULL REFERENCES hospital_pharmacy.pharmacy_credit_accounts(id),
    dispense_order_id UUID REFERENCES hospital_pharmacy.dispense_orders(id),
    amount NUMERIC(12,2) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    reference_no VARCHAR(100),
    received_by UUID NOT NULL,
    payment_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hp_pay_amount CHECK (amount > 0),
    CONSTRAINT chk_hp_pay_mode CHECK (payment_mode IN ('CASH','CARD','UPI','BANK_TRANSFER','OTHER'))
);
CREATE INDEX IF NOT EXISTS idx_hp_payment_account ON hospital_pharmacy.pharmacy_payments (credit_account_id);
CREATE INDEX IF NOT EXISTS idx_hp_payment_date ON hospital_pharmacy.pharmacy_payments (payment_date DESC);
CREATE INDEX IF NOT EXISTS idx_hp_payment_order ON hospital_pharmacy.pharmacy_payments (dispense_order_id) WHERE dispense_order_id IS NOT NULL;
