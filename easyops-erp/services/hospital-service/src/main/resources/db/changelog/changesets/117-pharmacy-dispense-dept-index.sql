--liquibase formatted sql

--changeset easyops:117-dispense-orders-dept-index
CREATE INDEX IF NOT EXISTS idx_hp_dispense_orders_dept
    ON hospital_pharmacy.dispense_orders (department_id)
    WHERE department_id IS NOT NULL;
