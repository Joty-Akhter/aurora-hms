--liquibase formatted sql

--changeset easyops:hosp-clinical-orders-007-facility-order-sets context:hospital-clinical-orders
--comment: Phase 4.1 – optional facility_id on order_sets for multi-facility scaling
ALTER TABLE hospital_clinical_orders.order_sets
    ADD COLUMN IF NOT EXISTS facility_id UUID;

CREATE INDEX IF NOT EXISTS idx_co_order_sets_facility_created
    ON hospital_clinical_orders.order_sets (facility_id, created_at);

--changeset easyops:hosp-clinical-orders-008-facility-clinical-orders context:hospital-clinical-orders
--comment: Phase 4.1 – optional facility_id on clinical_orders (denormalized for query)
ALTER TABLE hospital_clinical_orders.clinical_orders
    ADD COLUMN IF NOT EXISTS facility_id UUID;

CREATE INDEX IF NOT EXISTS idx_co_clinical_orders_facility
    ON hospital_clinical_orders.clinical_orders (facility_id);
