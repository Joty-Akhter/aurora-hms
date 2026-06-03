--liquibase formatted sql

--changeset easyops:026-create-purchase-updated-at-triggers
--comment: Create updated_at triggers for all purchase tables

-- Purchase orders
CREATE TRIGGER trg_purchase_orders_updated_at
    BEFORE UPDATE ON purchase.purchase_orders
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- Purchase receipts
CREATE TRIGGER trg_purchase_receipts_updated_at
    BEFORE UPDATE ON purchase.purchase_receipts
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- Purchase invoices
CREATE TRIGGER trg_purchase_invoices_updated_at
    BEFORE UPDATE ON purchase.purchase_invoices
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- Purchase approvals
CREATE TRIGGER trg_purchase_approvals_updated_at
    BEFORE UPDATE ON purchase.purchase_approvals
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

--rollback DROP TRIGGER IF EXISTS trg_purchase_orders_updated_at ON purchase.purchase_orders;
--rollback DROP TRIGGER IF EXISTS trg_purchase_receipts_updated_at ON purchase.purchase_receipts;
--rollback DROP TRIGGER IF EXISTS trg_purchase_invoices_updated_at ON purchase.purchase_invoices;
--rollback DROP TRIGGER IF EXISTS trg_purchase_approvals_updated_at ON purchase.purchase_approvals;

--changeset easyops:026-create-purchase-views
--comment: Create views for purchase module reporting and analytics

-- View: Purchase summary by status
CREATE OR REPLACE VIEW purchase.v_purchase_summary AS
SELECT 
    po.organization_id,
    po.status,
    COUNT(*) as po_count,
    SUM(po.total_amount) as total_amount,
    po.currency,
    AVG(po.total_amount) as average_po_value,
    MIN(po.po_date) as earliest_po_date,
    MAX(po.po_date) as latest_po_date
FROM purchase.purchase_orders po
GROUP BY po.organization_id, po.status, po.currency;

COMMENT ON VIEW purchase.v_purchase_summary IS 'Purchase order summary aggregated by status';

-- View: Pending receipts (POs awaiting receipt)
CREATE OR REPLACE VIEW purchase.v_pending_receipts AS
SELECT 
    po.id as po_id,
    po.po_number,
    po.po_date,
    po.vendor_id,
    po.vendor_name,
    po.total_amount,
    po.currency,
    po.expected_delivery_date,
    COALESCE(SUM(pol.quantity), 0) as total_ordered_quantity,
    COALESCE(SUM(pol.received_quantity), 0) as total_received_quantity,
    COALESCE(SUM(pol.quantity - COALESCE(pol.received_quantity, 0)), 0) as total_pending_quantity,
    COUNT(pol.id) as line_count
FROM purchase.purchase_orders po
LEFT JOIN purchase.purchase_order_lines pol ON po.id = pol.po_id
WHERE po.status IN ('APPROVED')
GROUP BY po.id, po.po_number, po.po_date, po.vendor_id, po.vendor_name, 
         po.total_amount, po.currency, po.expected_delivery_date
HAVING SUM(pol.quantity - COALESCE(pol.received_quantity, 0)) > 0;

COMMENT ON VIEW purchase.v_pending_receipts IS 'Purchase orders awaiting goods receipt';

-- View: Pending invoices (Receipts awaiting invoicing)
CREATE OR REPLACE VIEW purchase.v_pending_invoices AS
SELECT 
    pr.id as receipt_id,
    pr.receipt_number,
    pr.receipt_date,
    pr.po_id,
    po.po_number,
    pr.vendor_id,
    pr.vendor_name,
    pr.total_amount,
    pr.status,
    COALESCE(COUNT(prl.id), 0) as line_count,
    COALESCE(SUM(prl.received_quantity), 0) as total_received_quantity
FROM purchase.purchase_receipts pr
JOIN purchase.purchase_orders po ON pr.po_id = po.id
LEFT JOIN purchase.purchase_receipt_lines prl ON pr.id = prl.receipt_id
WHERE pr.status IN ('RECEIVED', 'INSPECTED')
AND NOT EXISTS (
    SELECT 1 FROM purchase.purchase_invoices pi
    WHERE pi.receipt_id = pr.id
)
GROUP BY pr.id, pr.receipt_number, pr.receipt_date, pr.po_id, po.po_number,
         pr.vendor_id, pr.vendor_name, pr.total_amount, pr.status;

COMMENT ON VIEW purchase.v_pending_invoices IS 'Purchase receipts awaiting invoice matching';

-- View: Vendor performance metrics
CREATE OR REPLACE VIEW purchase.v_vendor_performance AS
SELECT 
    po.vendor_id,
    po.vendor_name,
    po.organization_id,
    COUNT(DISTINCT po.id) as total_pos,
    SUM(po.total_amount) as total_spend,
    AVG(po.total_amount) as average_po_value,
    po.currency,
    COUNT(CASE WHEN po.status = 'RECEIVED' THEN 1 END) as completed_pos,
    COUNT(CASE WHEN po.status = 'CANCELLED' THEN 1 END) as cancelled_pos,
    AVG(CASE 
        WHEN po.actual_delivery_date IS NOT NULL AND po.expected_delivery_date IS NOT NULL 
        THEN (po.actual_delivery_date - po.expected_delivery_date)
        ELSE NULL
    END) as avg_delivery_delay_days,
    COUNT(CASE 
        WHEN po.actual_delivery_date <= po.expected_delivery_date THEN 1 
    END)::DECIMAL / NULLIF(COUNT(CASE WHEN po.actual_delivery_date IS NOT NULL THEN 1 END), 0) * 100 
        as on_time_delivery_rate
FROM purchase.purchase_orders po
WHERE po.status NOT IN ('DRAFT', 'CANCELLED')
GROUP BY po.vendor_id, po.vendor_name, po.organization_id, po.currency;

COMMENT ON VIEW purchase.v_vendor_performance IS 'Vendor performance metrics and KPIs';

-- View: Purchase order details with line items
CREATE OR REPLACE VIEW purchase.v_purchase_order_details AS
SELECT 
    po.id as po_id,
    po.organization_id,
    po.po_number,
    po.po_date,
    po.vendor_id,
    po.vendor_name,
    po.status as po_status,
    po.total_amount as po_total_amount,
    po.currency,
    pol.id as line_id,
    pol.line_number,
    pol.product_id,
    pol.product_name,
    pol.product_sku,
    pol.quantity,
    pol.unit_price,
    pol.line_total,
    pol.received_quantity,
    pol.quantity - COALESCE(pol.received_quantity, 0) as pending_quantity,
    pol.status as line_status
FROM purchase.purchase_orders po
LEFT JOIN purchase.purchase_order_lines pol ON po.id = pol.po_id;

COMMENT ON VIEW purchase.v_purchase_order_details IS 'Detailed purchase order information with line items';

-- View: Invoice matching summary
CREATE OR REPLACE VIEW purchase.v_invoice_matching_summary AS
SELECT 
    pi.id as invoice_id,
    pi.invoice_number,
    pi.invoice_date,
    pi.po_id,
    po.po_number,
    pi.receipt_id,
    pr.receipt_number,
    pi.vendor_id,
    pi.vendor_name,
    pi.total_amount as invoice_total,
    pi.matching_status,
    pi.price_variance,
    pi.quantity_variance,
    pi.status as invoice_status,
    pi.payment_status,
    pi.balance_amount,
    COUNT(pil.id) as line_count,
    COUNT(CASE WHEN pil.variance_status != 'MATCHED' THEN 1 END) as variance_count
FROM purchase.purchase_invoices pi
JOIN purchase.purchase_orders po ON pi.po_id = po.id
LEFT JOIN purchase.purchase_receipts pr ON pi.receipt_id = pr.id
LEFT JOIN purchase.purchase_invoice_lines pil ON pi.id = pil.invoice_id
GROUP BY pi.id, pi.invoice_number, pi.invoice_date, pi.po_id, po.po_number,
         pi.receipt_id, pr.receipt_number, pi.vendor_id, pi.vendor_name,
         pi.total_amount, pi.matching_status, pi.price_variance,
         pi.quantity_variance, pi.status, pi.payment_status, pi.balance_amount;

COMMENT ON VIEW purchase.v_invoice_matching_summary IS 'Three-way matching summary for purchase invoices';

--rollback DROP VIEW IF EXISTS purchase.v_purchase_summary;
--rollback DROP VIEW IF EXISTS purchase.v_pending_receipts;
--rollback DROP VIEW IF EXISTS purchase.v_pending_invoices;
--rollback DROP VIEW IF EXISTS purchase.v_vendor_performance;
--rollback DROP VIEW IF EXISTS purchase.v_purchase_order_details;
--rollback DROP VIEW IF EXISTS purchase.v_invoice_matching_summary;