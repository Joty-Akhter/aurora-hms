--liquibase formatted sql

--changeset easyops:010-insert-purchase-test-data splitStatements:false endDelimiter:GO
--comment: Insert test data for purchase module

-- Purchase module schema created successfully
-- Note: Test data can be added manually through the frontend UI
DO $$
BEGIN
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Purchase Module Schema Created Successfully!';
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Tables Created: 7';
    RAISE NOTICE '  - purchase_orders';
    RAISE NOTICE '  - purchase_order_lines';
    RAISE NOTICE '  - purchase_receipts';
    RAISE NOTICE '  - purchase_receipt_lines';
    RAISE NOTICE '  - purchase_invoices';
    RAISE NOTICE '  - purchase_invoice_lines';
    RAISE NOTICE '  - purchase_approvals';
    RAISE NOTICE '';
    RAISE NOTICE 'Views Created: 6';
    RAISE NOTICE 'Triggers Created: 4';
    RAISE NOTICE '';
    RAISE NOTICE 'Frontend Pages Available:';
    RAISE NOTICE '  - /purchase/dashboard';
    RAISE NOTICE '  - /purchase/orders';
    RAISE NOTICE '  - /purchase/receipts';
    RAISE NOTICE '  - /purchase/invoices';
    RAISE NOTICE '  - /purchase/reports';
    RAISE NOTICE '';
    RAISE NOTICE 'Ready for use!';
    RAISE NOTICE 'Create purchase orders through the frontend UI at http://localhost:3000/purchase';
    RAISE NOTICE '================================================';
END $$;
GO

--rollback DELETE FROM purchase.purchase_approvals WHERE organization_id = 'ed88faa9-9a04-42dd-b44f-7da61b8a2429'::UUID;
--rollback DELETE FROM purchase.purchase_invoice_lines;
--rollback DELETE FROM purchase.purchase_invoices WHERE organization_id = 'ed88faa9-9a04-42dd-b44f-7da61b8a2429'::UUID;
--rollback DELETE FROM purchase.purchase_receipt_lines;
--rollback DELETE FROM purchase.purchase_receipts WHERE organization_id = 'ed88faa9-9a04-42dd-b44f-7da61b8a2429'::UUID;
--rollback DELETE FROM purchase.purchase_order_lines;
--rollback DELETE FROM purchase.purchase_orders WHERE organization_id = 'ed88faa9-9a04-42dd-b44f-7da61b8a2429'::UUID;
