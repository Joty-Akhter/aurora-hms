--liquibase formatted sql

--changeset easyops:047-create-manufacturing-work-orders-views splitStatements:false

-- =====================================================
-- View: Work Order Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_work_order_summary AS
SELECT 
    wo.work_order_id,
    wo.organization_id,
    wo.work_order_number,
    wo.product_id,
    wo.product_code,
    wo.product_name,
    wo.order_type,
    wo.status,
    wo.priority,
    wo.quantity_planned,
    wo.quantity_completed,
    wo.quantity_scrapped,
    wo.quantity_in_process,
    wo.completion_percentage,
    
    -- Date Tracking
    wo.planned_start_date,
    wo.planned_end_date,
    wo.actual_start_date,
    wo.actual_end_date,
    
    -- Time Calculations
    CASE 
        WHEN wo.actual_end_date IS NOT NULL AND wo.actual_start_date IS NOT NULL
        THEN EXTRACT(EPOCH FROM (wo.actual_end_date - wo.actual_start_date)) / 3600
        ELSE NULL
    END AS actual_duration_hours,
    
    CASE 
        WHEN wo.planned_end_date IS NOT NULL AND wo.planned_start_date IS NOT NULL
        THEN EXTRACT(EPOCH FROM (wo.planned_end_date - wo.planned_start_date)) / 3600
        ELSE NULL
    END AS planned_duration_hours,
    
    -- On-Time Performance
    CASE 
        WHEN wo.status = 'COMPLETED' AND wo.actual_end_date <= wo.planned_end_date THEN true
        WHEN wo.status = 'COMPLETED' AND wo.actual_end_date > wo.planned_end_date THEN false
        ELSE NULL
    END AS completed_on_time,
    
    -- Days Since Creation
    EXTRACT(DAY FROM (CURRENT_TIMESTAMP - wo.created_at)) AS days_since_created,
    
    -- Days Until Due
    CASE 
        WHEN wo.planned_end_date IS NOT NULL
        THEN EXTRACT(DAY FROM (wo.planned_end_date - CURRENT_TIMESTAMP))
        ELSE NULL
    END AS days_until_due,
    
    -- Cost Summary
    wo.material_cost,
    wo.labor_cost,
    wo.overhead_cost,
    wo.total_cost,
    
    -- Operations Summary
    wo.operations_completed,
    wo.total_operations,
    CASE 
        WHEN wo.total_operations > 0 
        THEN (wo.operations_completed::DECIMAL / wo.total_operations * 100)
        ELSE 0
    END AS operations_completion_percentage,
    
    -- Material Summary
    (SELECT COUNT(*) FROM manufacturing.work_order_materials WHERE work_order_id = wo.work_order_id) AS total_materials,
    (SELECT COUNT(*) FROM manufacturing.work_order_materials WHERE work_order_id = wo.work_order_id AND status = 'CONSUMED') AS materials_consumed,
    
    wo.created_at,
    wo.updated_at
FROM manufacturing.work_orders wo;

-- =====================================================
-- View: Production Progress Dashboard
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_production_progress AS
SELECT 
    wo.organization_id,
    wo.status,
    COUNT(*) AS work_order_count,
    SUM(wo.quantity_planned) AS total_quantity_planned,
    SUM(wo.quantity_completed) AS total_quantity_completed,
    SUM(wo.quantity_scrapped) AS total_quantity_scrapped,
    AVG(wo.completion_percentage) AS avg_completion_percentage,
    SUM(wo.total_cost) AS total_cost,
    
    -- On-Time Performance
    COUNT(CASE WHEN wo.status = 'COMPLETED' AND wo.actual_end_date <= wo.planned_end_date THEN 1 END) AS completed_on_time,
    COUNT(CASE WHEN wo.status = 'COMPLETED' AND wo.actual_end_date > wo.planned_end_date THEN 1 END) AS completed_late,
    
    -- Quality Metrics
    SUM(wo.quantity_completed) AS total_good_units,
    SUM(wo.quantity_scrapped) AS total_scrapped_units,
    CASE 
        WHEN SUM(wo.quantity_planned) > 0 
        THEN (SUM(wo.quantity_completed)::DECIMAL / SUM(wo.quantity_planned) * 100)
        ELSE 0
    END AS yield_percentage
FROM manufacturing.work_orders wo
GROUP BY wo.organization_id, wo.status;

-- =====================================================
-- View: Work Order Material Requirements
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_work_order_material_requirements AS
SELECT 
    wom.work_order_id,
    wo.work_order_number,
    wo.organization_id,
    wom.component_id,
    wom.component_code,
    wom.component_name,
    wom.quantity_required,
    wom.quantity_reserved,
    wom.quantity_issued,
    wom.quantity_consumed,
    wom.quantity_returned,
    wom.quantity_scrapped,
    
    -- Shortage Calculation
    (wom.quantity_required - wom.quantity_reserved) AS quantity_shortage,
    
    -- Status Indicators
    CASE 
        WHEN wom.quantity_reserved >= wom.quantity_required THEN 'FULLY_RESERVED'
        WHEN wom.quantity_reserved > 0 THEN 'PARTIALLY_RESERVED'
        ELSE 'NOT_RESERVED'
    END AS reservation_status,
    
    CASE 
        WHEN wom.quantity_issued >= wom.quantity_required THEN 'FULLY_ISSUED'
        WHEN wom.quantity_issued > 0 THEN 'PARTIALLY_ISSUED'
        ELSE 'NOT_ISSUED'
    END AS issuance_status,
    
    wom.status,
    wom.warehouse_id,
    wom.unit_cost,
    wom.total_cost,
    wom.required_date,
    wom.issued_date
FROM manufacturing.work_order_materials wom
JOIN manufacturing.work_orders wo ON wom.work_order_id = wo.work_order_id;

-- =====================================================
-- View: Work Order Operation Status
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_work_order_operation_status AS
SELECT 
    woo.work_order_id,
    wo.work_order_number,
    wo.organization_id,
    woo.operation_id,
    woo.operation_sequence,
    woo.operation_code,
    woo.operation_name,
    woo.status,
    woo.work_center_code,
    woo.assigned_to,
    
    -- Time Tracking
    woo.total_time_planned,
    woo.total_time_actual,
    CASE 
        WHEN woo.total_time_planned > 0 
        THEN ((woo.total_time_actual - woo.total_time_planned) / woo.total_time_planned * 100)
        ELSE 0
    END AS time_variance_percentage,
    
    -- Efficiency
    CASE 
        WHEN woo.total_time_actual > 0 
        THEN (woo.total_time_planned / woo.total_time_actual * 100)
        ELSE NULL
    END AS efficiency_percentage,
    
    -- Quality
    woo.quantity_completed,
    woo.quantity_rejected,
    woo.quantity_reworked,
    CASE 
        WHEN (woo.quantity_completed + woo.quantity_rejected) > 0 
        THEN (woo.quantity_completed::DECIMAL / (woo.quantity_completed + woo.quantity_rejected) * 100)
        ELSE 0
    END AS quality_percentage,
    
    -- Quality Check
    woo.quality_check_required,
    woo.quality_check_completed,
    woo.quality_result,
    
    -- Dates
    woo.planned_start,
    woo.planned_end,
    woo.actual_start,
    woo.actual_end,
    
    -- On-Time
    CASE 
        WHEN woo.status = 'COMPLETED' AND woo.actual_end <= woo.planned_end THEN true
        WHEN woo.status = 'COMPLETED' AND woo.actual_end > woo.planned_end THEN false
        ELSE NULL
    END AS completed_on_time
FROM manufacturing.work_order_operations woo
JOIN manufacturing.work_orders wo ON woo.work_order_id = wo.work_order_id;

-- =====================================================
-- View: Production Efficiency Metrics
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_production_efficiency AS
SELECT 
    wo.organization_id,
    wo.product_id,
    wo.product_code,
    COUNT(*) AS total_work_orders,
    SUM(wo.quantity_completed) AS total_quantity_produced,
    SUM(wo.quantity_scrapped) AS total_quantity_scrapped,
    
    -- Yield
    CASE 
        WHEN SUM(wo.quantity_planned) > 0 
        THEN (SUM(wo.quantity_completed)::DECIMAL / SUM(wo.quantity_planned) * 100)
        ELSE 0
    END AS overall_yield_percentage,
    
    -- Scrap Rate
    CASE 
        WHEN SUM(wo.quantity_completed + wo.quantity_scrapped) > 0 
        THEN (SUM(wo.quantity_scrapped)::DECIMAL / SUM(wo.quantity_completed + wo.quantity_scrapped) * 100)
        ELSE 0
    END AS scrap_rate_percentage,
    
    -- Average Completion Time
    AVG(
        CASE 
            WHEN wo.actual_end_date IS NOT NULL AND wo.actual_start_date IS NOT NULL
            THEN EXTRACT(EPOCH FROM (wo.actual_end_date - wo.actual_start_date)) / 3600
            ELSE NULL
        END
    ) AS avg_completion_time_hours,
    
    -- Cost Metrics
    AVG(wo.total_cost) AS avg_total_cost_per_wo,
    SUM(wo.total_cost) AS total_production_cost,
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.total_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS cost_per_unit
FROM manufacturing.work_orders wo
WHERE wo.status IN ('COMPLETED', 'CLOSED')
GROUP BY wo.organization_id, wo.product_id, wo.product_code;

-- =====================================================
-- View: Shop Floor Dashboard
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_shop_floor_dashboard AS
SELECT 
    wo.organization_id,
    wo.work_order_id,
    wo.work_order_number,
    wo.product_code,
    wo.product_name,
    wo.status AS work_order_status,
    wo.priority,
    wo.quantity_planned,
    wo.quantity_completed,
    wo.completion_percentage,
    wo.planned_end_date,
    
    -- Current Operation
    (
        SELECT woo.operation_name 
        FROM manufacturing.work_order_operations woo 
        WHERE woo.work_order_id = wo.work_order_id 
        AND woo.status = 'IN_PROGRESS' 
        LIMIT 1
    ) AS current_operation,
    
    (
        SELECT woo.work_center_code 
        FROM manufacturing.work_order_operations woo 
        WHERE woo.work_order_id = wo.work_order_id 
        AND woo.status = 'IN_PROGRESS' 
        LIMIT 1
    ) AS current_work_center,
    
    -- Material Status
    (
        SELECT COUNT(*) 
        FROM manufacturing.work_order_materials wom 
        WHERE wom.work_order_id = wo.work_order_id 
        AND wom.status != 'CONSUMED'
    ) AS materials_pending,
    
    -- Days to Due Date
    CASE 
        WHEN wo.planned_end_date IS NOT NULL
        THEN EXTRACT(DAY FROM (wo.planned_end_date - CURRENT_TIMESTAMP))
        ELSE NULL
    END AS days_until_due,
    
    -- Alert Flag
    CASE 
        WHEN wo.status = 'IN_PROGRESS' AND wo.planned_end_date < CURRENT_TIMESTAMP THEN 'OVERDUE'
        WHEN wo.status = 'IN_PROGRESS' AND wo.planned_end_date < (CURRENT_TIMESTAMP + INTERVAL '1 day') THEN 'DUE_SOON'
        ELSE 'ON_TRACK'
    END AS alert_status,
    
    wo.created_at,
    wo.actual_start_date
FROM manufacturing.work_orders wo
WHERE wo.status IN ('RELEASED', 'IN_PROGRESS', 'PAUSED')
ORDER BY wo.priority DESC, wo.planned_end_date ASC;


