--liquibase formatted sql

--changeset easyops:201-add-sales-triggers context:sales
--comment: Add updated_at triggers for sales tables

CREATE TRIGGER update_sales_customers_updated_at BEFORE UPDATE ON sales.customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sales_products_updated_at BEFORE UPDATE ON sales.products FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sales_quotations_updated_at BEFORE UPDATE ON sales.quotations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sales_quotation_lines_updated_at BEFORE UPDATE ON sales.quotation_lines FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sales_orders_updated_at BEFORE UPDATE ON sales.sales_orders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sales_order_lines_updated_at BEFORE UPDATE ON sales.sales_order_lines FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:202-add-sales-foreign-keys context:sales splitStatements:false
--comment: Add foreign key constraints for sales tables

-- Add foreign key constraints for created_by and updated_by columns
DO $$
BEGIN
    -- Quotations foreign keys
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_quotations_created_by' 
                   AND table_schema = 'sales') THEN
        ALTER TABLE sales.quotations 
        ADD CONSTRAINT fk_quotations_created_by 
        FOREIGN KEY (created_by) REFERENCES users.users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_quotations_updated_by' 
                   AND table_schema = 'sales') THEN
        ALTER TABLE sales.quotations 
        ADD CONSTRAINT fk_quotations_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users.users(id);
    END IF;
    
    -- Sales orders foreign keys
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_sales_orders_created_by' 
                   AND table_schema = 'sales') THEN
        ALTER TABLE sales.sales_orders 
        ADD CONSTRAINT fk_sales_orders_created_by 
        FOREIGN KEY (created_by) REFERENCES users.users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_sales_orders_updated_by' 
                   AND table_schema = 'sales') THEN
        ALTER TABLE sales.sales_orders 
        ADD CONSTRAINT fk_sales_orders_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users.users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_sales_orders_approved_by' 
                   AND table_schema = 'sales') THEN
        ALTER TABLE sales.sales_orders 
        ADD CONSTRAINT fk_sales_orders_approved_by 
        FOREIGN KEY (approved_by) REFERENCES users.users(id);
    END IF;
END $$;

--changeset easyops:203-add-sales-indexes context:sales
--comment: Add additional indexes for sales performance

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_quotations_valid_until ON sales.quotations(valid_until);
CREATE INDEX IF NOT EXISTS idx_quotations_converted_to_order ON sales.quotations(converted_to_order_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_expected_delivery ON sales.sales_orders(expected_delivery_date);
CREATE INDEX IF NOT EXISTS idx_sales_orders_converted_to_invoice ON sales.sales_orders(converted_to_invoice_id);
CREATE INDEX IF NOT EXISTS idx_sales_orders_approved_date ON sales.sales_orders(approved_date);
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_delivered_quantity ON sales.sales_order_lines(delivered_quantity);
CREATE INDEX IF NOT EXISTS idx_sales_order_lines_invoiced_quantity ON sales.sales_order_lines(invoiced_quantity);
