--liquibase formatted sql

--changeset easyops:hosp-clinical-orders-002-order-sets context:hospital-clinical-orders
--comment: Phase 1 – order_sets table
CREATE TABLE IF NOT EXISTS hospital_clinical_orders.order_sets (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    visit_id UUID,
    ordering_doctor_id UUID,
    ordering_department_id UUID,
    order_context VARCHAR(20),
    priority VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_co_order_sets_patient_created
    ON hospital_clinical_orders.order_sets (patient_id, created_at);
CREATE INDEX IF NOT EXISTS idx_co_order_sets_visit_created
    ON hospital_clinical_orders.order_sets (visit_id, created_at);

--changeset easyops:hosp-clinical-orders-003-clinical-orders context:hospital-clinical-orders
--comment: Phase 1 – clinical_orders table
CREATE TABLE IF NOT EXISTS hospital_clinical_orders.clinical_orders (
    id UUID PRIMARY KEY,
    order_set_id UUID NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20),
    ordering_notes TEXT,
    performed_at TIMESTAMPTZ,
    performed_by UUID,
    cancel_reason TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID,
    external_system_id VARCHAR(255),
    result_status VARCHAR(20),
    result_available_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_co_clinical_orders_order_set
        FOREIGN KEY (order_set_id) REFERENCES hospital_clinical_orders.order_sets(id)
);

CREATE INDEX IF NOT EXISTS idx_co_clinical_orders_order_set
    ON hospital_clinical_orders.clinical_orders (order_set_id);
CREATE INDEX IF NOT EXISTS idx_co_clinical_orders_status_type
    ON hospital_clinical_orders.clinical_orders (status, order_type);
CREATE INDEX IF NOT EXISTS idx_co_clinical_orders_created_at
    ON hospital_clinical_orders.clinical_orders (created_at);

--changeset easyops:hosp-clinical-orders-004-worklist-items context:hospital-clinical-orders
--comment: Phase 1 – order_worklist_items table
CREATE TABLE IF NOT EXISTS hospital_clinical_orders.order_worklist_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    worklist_type VARCHAR(50) NOT NULL,
    assigned_to_user_id UUID,
    assigned_to_role VARCHAR(100),
    scheduled_time TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_co_worklist_items_order
        FOREIGN KEY (order_id) REFERENCES hospital_clinical_orders.clinical_orders(id)
);

CREATE INDEX IF NOT EXISTS idx_co_worklist_items_order
    ON hospital_clinical_orders.order_worklist_items (order_id);
CREATE INDEX IF NOT EXISTS idx_co_worklist_items_type_status
    ON hospital_clinical_orders.order_worklist_items (worklist_type, status);
CREATE INDEX IF NOT EXISTS idx_co_worklist_items_assigned_status
    ON hospital_clinical_orders.order_worklist_items (assigned_to_user_id, status);
