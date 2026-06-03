--liquibase formatted sql

--changeset easyops:hosp-clinical-orders-005-result-links context:hospital-clinical-orders
--comment: Phase 2 – result_links table for LIS/RIS/PACS result metadata and viewer URLs
CREATE TABLE IF NOT EXISTS hospital_clinical_orders.result_links (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    system_type VARCHAR(30) NOT NULL,
    external_system_id VARCHAR(255),
    viewer_url VARCHAR(1000),
    version INTEGER DEFAULT 1,
    revised_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_co_result_links_order
        FOREIGN KEY (order_id) REFERENCES hospital_clinical_orders.clinical_orders(id)
);

CREATE INDEX IF NOT EXISTS idx_co_result_links_order_id
    ON hospital_clinical_orders.result_links (order_id);
CREATE INDEX IF NOT EXISTS idx_co_result_links_external_system_id
    ON hospital_clinical_orders.result_links (external_system_id);
