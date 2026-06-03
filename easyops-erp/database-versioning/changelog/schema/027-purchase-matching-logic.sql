--liquibase formatted sql

--changeset easyops:027-add-variance-approval-columns
--comment: Add variance approval fields to purchase_invoices

ALTER TABLE purchase.purchase_invoices 
ADD COLUMN IF NOT EXISTS variance_approved_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS variance_approved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS variance_approval_notes TEXT;

COMMENT ON COLUMN purchase.purchase_invoices.variance_approved_by IS 'User who approved the invoice variance';
COMMENT ON COLUMN purchase.purchase_invoices.variance_approved_at IS 'Timestamp when variance was approved';

--rollback ALTER TABLE purchase.purchase_invoices DROP COLUMN IF EXISTS variance_approved_by, DROP COLUMN IF EXISTS variance_approved_at, DROP COLUMN IF EXISTS variance_approval_notes;

--changeset easyops:027-create-variance-summary-view
--comment: Create view for invoice variances summary

CREATE OR REPLACE VIEW purchase.v_invoice_variances AS
SELECT 
    pi.id as invoice_id,
    pi.organization_id,
    pi.invoice_number,
    pi.invoice_date,
    po.po_number,
    pi.vendor_name,
    pi.total_amount,
    pi.price_variance,
    pi.quantity_variance,
    pi.matching_status,
    pi.variance_approved_by,
    pi.variance_approved_at,
    CASE 
        WHEN ABS(pi.price_variance) > 1000 OR ABS(pi.quantity_variance) > 10 THEN 'HIGH'
        WHEN ABS(pi.price_variance) > 100 OR ABS(pi.quantity_variance) > 5 THEN 'MEDIUM'
        ELSE 'LOW'
    END as variance_severity,
    COUNT(pil.id) FILTER (WHERE pil.variance_status != 'MATCHED') as variance_line_count
FROM purchase.purchase_invoices pi
JOIN purchase.purchase_orders po ON pi.po_id = po.id
LEFT JOIN purchase.purchase_invoice_lines pil ON pi.id = pil.invoice_id
WHERE pi.matching_status IN ('VARIANCE', 'PENDING')
GROUP BY pi.id, pi.organization_id, pi.invoice_number, pi.invoice_date, 
         po.po_number, pi.vendor_name, pi.total_amount, pi.price_variance,
         pi.quantity_variance, pi.matching_status, pi.variance_approved_by,
         pi.variance_approved_at;

COMMENT ON VIEW purchase.v_invoice_variances IS 'Summary of invoices with variances requiring attention';

--rollback DROP VIEW IF EXISTS purchase.v_invoice_variances;

--changeset easyops:027-create-payment-tracking-view
--comment: Create view for payment tracking summary

CREATE OR REPLACE VIEW purchase.v_payment_tracking AS
SELECT 
    pi.organization_id,
    pi.id as invoice_id,
    pi.invoice_number,
    pi.invoice_date,
    pi.due_date,
    pi.vendor_id,
    pi.vendor_name,
    pi.total_amount,
    pi.paid_amount,
    pi.balance_amount,
    pi.payment_status,
    pi.bill_id,
    pi.bill_created,
    CASE 
        WHEN pi.due_date < CURRENT_DATE AND pi.balance_amount > 0 THEN 
            CURRENT_DATE - pi.due_date
        ELSE 0
    END as days_overdue,
    CASE
        WHEN pi.due_date < CURRENT_DATE AND pi.balance_amount > 0 THEN TRUE
        ELSE FALSE
    END as is_overdue
FROM purchase.purchase_invoices pi
WHERE pi.status IN ('APPROVED', 'POSTED')
ORDER BY pi.due_date ASC;

COMMENT ON VIEW purchase.v_payment_tracking IS 'Payment tracking and overdue monitoring';

--rollback DROP VIEW IF EXISTS purchase.v_payment_tracking;

--changeset easyops:027-create-purchase-analytics-view
--comment: Create comprehensive analytics view for purchase module

CREATE OR REPLACE VIEW purchase.v_purchase_analytics AS
SELECT 
    po.organization_id,
    DATE_TRUNC('month', po.po_date) as month,
    COUNT(DISTINCT po.id) as total_pos,
    COUNT(DISTINCT po.vendor_id) as unique_vendors,
    SUM(po.total_amount) as total_spend,
    AVG(po.total_amount) as avg_po_value,
    COUNT(CASE WHEN po.status = 'APPROVED' THEN 1 END) as approved_count,
    COUNT(CASE WHEN po.status = 'RECEIVED' THEN 1 END) as received_count,
    COUNT(CASE WHEN po.status = 'CANCELLED' THEN 1 END) as cancelled_count,
    po.currency,
    AVG(CASE 
        WHEN po.actual_delivery_date IS NOT NULL AND po.expected_delivery_date IS NOT NULL
        THEN (po.actual_delivery_date - po.expected_delivery_date)
        ELSE NULL
    END) as avg_delivery_variance_days
FROM purchase.purchase_orders po
WHERE po.status != 'DRAFT'
GROUP BY po.organization_id, DATE_TRUNC('month', po.po_date), po.currency
ORDER BY month DESC;

COMMENT ON VIEW purchase.v_purchase_analytics IS 'Monthly purchase analytics for trend analysis';

--rollback DROP VIEW IF EXISTS purchase.v_purchase_analytics;