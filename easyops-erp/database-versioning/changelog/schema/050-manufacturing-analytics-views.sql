--liquibase formatted sql

--changeset easyops:050-create-manufacturing-analytics-views splitStatements:false

-- =====================================================
-- View: Manufacturing Dashboard KPIs
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_manufacturing_dashboard AS
SELECT 
    wo.organization_id,
    
    -- Work Order Metrics
    COUNT(wo.work_order_id) AS total_work_orders,
    COUNT(CASE WHEN wo.status = 'IN_PROGRESS' THEN 1 END) AS active_work_orders,
    COUNT(CASE WHEN wo.status = 'COMPLETED' THEN 1 END) AS completed_work_orders,
    COUNT(CASE WHEN wo.planned_end_date < CURRENT_TIMESTAMP AND wo.status IN ('RELEASED', 'IN_PROGRESS') THEN 1 END) AS overdue_work_orders,
    
    -- Production Quantities
    SUM(wo.quantity_planned) AS total_planned_quantity,
    SUM(wo.quantity_completed) AS total_completed_quantity,
    SUM(wo.quantity_scrapped) AS total_scrapped_quantity,
    
    -- Overall Yield
    CASE 
        WHEN SUM(wo.quantity_planned) > 0 
        THEN (SUM(wo.quantity_completed) / SUM(wo.quantity_planned) * 100)
        ELSE 0
    END AS overall_yield_percentage,
    
    -- Overall Scrap Rate
    CASE 
        WHEN SUM(wo.quantity_completed + wo.quantity_scrapped) > 0 
        THEN (SUM(wo.quantity_scrapped) / SUM(wo.quantity_completed + wo.quantity_scrapped) * 100)
        ELSE 0
    END AS overall_scrap_rate,
    
    -- Cost Metrics
    SUM(wo.material_cost) AS total_material_cost,
    SUM(wo.labor_cost) AS total_labor_cost,
    SUM(wo.overhead_cost) AS total_overhead_cost,
    SUM(wo.total_cost) AS total_manufacturing_cost,
    
    -- On-Time Delivery
    COUNT(CASE WHEN wo.status = 'COMPLETED' AND wo.actual_end_date <= wo.planned_end_date THEN 1 END) AS on_time_completions,
    COUNT(CASE WHEN wo.status = 'COMPLETED' AND wo.actual_end_date > wo.planned_end_date THEN 1 END) AS late_completions,
    
    -- Quality Metrics (if quality data exists)
    (SELECT COUNT(*) FROM manufacturing.quality_inspections qi 
     WHERE qi.organization_id = wo.organization_id 
     AND qi.inspection_date >= CURRENT_DATE - INTERVAL '30 days'
    ) AS inspections_last_30_days,
    
    (SELECT COUNT(*) FROM manufacturing.non_conformances nc 
     WHERE nc.organization_id = wo.organization_id 
     AND nc.status IN ('OPEN', 'IN_REVIEW')
    ) AS open_non_conformances
FROM manufacturing.work_orders wo
GROUP BY wo.organization_id;

-- =====================================================
-- View: OEE (Overall Equipment Effectiveness) Metrics
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_oee_metrics AS
SELECT 
    woo.organization_id,
    woo.work_center_code,
    woo.work_center_name,
    DATE(woo.actual_start) AS production_date,
    
    -- Availability = Actual Production Time / Planned Production Time
    CASE 
        WHEN SUM(woo.total_time_planned) > 0 
        THEN (SUM(COALESCE(woo.total_time_actual, 0)) / SUM(woo.total_time_planned) * 100)
        ELSE 0
    END AS availability_percentage,
    
    -- Performance = (Ideal Cycle Time × Total Count) / Actual Production Time
    CASE 
        WHEN SUM(woo.total_time_actual) > 0 
        THEN (SUM(woo.total_time_planned) / SUM(woo.total_time_actual) * 100)
        ELSE 0
    END AS performance_percentage,
    
    -- Quality = Good Count / Total Count
    CASE 
        WHEN SUM(woo.quantity_completed + woo.quantity_rejected) > 0 
        THEN (SUM(woo.quantity_completed) / SUM(woo.quantity_completed + woo.quantity_rejected) * 100)
        ELSE 0
    END AS quality_percentage,
    
    -- OEE = Availability × Performance × Quality
    (
        CASE 
            WHEN SUM(woo.total_time_planned) > 0 
            THEN (SUM(COALESCE(woo.total_time_actual, 0)) / SUM(woo.total_time_planned))
            ELSE 0
        END *
        CASE 
            WHEN SUM(woo.total_time_actual) > 0 
            THEN (SUM(woo.total_time_planned) / SUM(woo.total_time_actual))
            ELSE 0
        END *
        CASE 
            WHEN SUM(woo.quantity_completed + woo.quantity_rejected) > 0 
            THEN (SUM(woo.quantity_completed) / SUM(woo.quantity_completed + woo.quantity_rejected))
            ELSE 0
        END * 100
    ) AS oee_percentage,
    
    -- Supporting Metrics
    SUM(woo.total_time_planned) AS total_planned_time,
    SUM(woo.total_time_actual) AS total_actual_time,
    SUM(woo.quantity_completed) AS total_good_quantity,
    SUM(woo.quantity_rejected) AS total_rejected_quantity,
    COUNT(woo.operation_id) AS total_operations
FROM manufacturing.work_order_operations woo
WHERE woo.status = 'COMPLETED'
AND woo.actual_start IS NOT NULL
GROUP BY woo.organization_id, woo.work_center_code, woo.work_center_name, DATE(woo.actual_start);

-- =====================================================
-- View: Production Performance Trends
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_production_trends AS
SELECT 
    wo.organization_id,
    DATE_TRUNC('day', wo.actual_end_date) AS production_date,
    DATE_TRUNC('week', wo.actual_end_date) AS production_week,
    DATE_TRUNC('month', wo.actual_end_date) AS production_month,
    
    -- Daily Metrics
    COUNT(wo.work_order_id) AS work_orders_completed,
    SUM(wo.quantity_completed) AS total_quantity_produced,
    SUM(wo.quantity_scrapped) AS total_quantity_scrapped,
    
    -- Quality Metrics
    CASE 
        WHEN SUM(wo.quantity_completed + wo.quantity_scrapped) > 0 
        THEN (SUM(wo.quantity_completed) / SUM(wo.quantity_completed + wo.quantity_scrapped) * 100)
        ELSE 0
    END AS daily_yield_percentage,
    
    -- Cost Metrics
    SUM(wo.total_cost) AS total_production_cost,
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.total_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS cost_per_unit,
    
    -- On-Time Performance
    COUNT(CASE WHEN wo.actual_end_date <= wo.planned_end_date THEN 1 END) AS on_time_count,
    COUNT(CASE WHEN wo.actual_end_date > wo.planned_end_date THEN 1 END) AS late_count,
    
    CASE 
        WHEN COUNT(*) > 0 
        THEN (COUNT(CASE WHEN wo.actual_end_date <= wo.planned_end_date THEN 1 END)::DECIMAL / COUNT(*) * 100)
        ELSE 0
    END AS on_time_percentage
FROM manufacturing.work_orders wo
WHERE wo.status = 'COMPLETED'
AND wo.actual_end_date IS NOT NULL
GROUP BY wo.organization_id, 
         DATE_TRUNC('day', wo.actual_end_date),
         DATE_TRUNC('week', wo.actual_end_date),
         DATE_TRUNC('month', wo.actual_end_date);

-- =====================================================
-- View: Cost Analysis by Product
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_cost_analysis_by_product AS
SELECT 
    wo.organization_id,
    wo.product_id,
    wo.product_code,
    wo.product_name,
    
    -- Quantity Metrics
    COUNT(wo.work_order_id) AS total_work_orders,
    SUM(wo.quantity_completed) AS total_quantity_produced,
    
    -- Cost Breakdown
    SUM(wo.material_cost) AS total_material_cost,
    SUM(wo.labor_cost) AS total_labor_cost,
    SUM(wo.overhead_cost) AS total_overhead_cost,
    SUM(wo.total_cost) AS total_cost,
    
    -- Average Costs
    AVG(wo.material_cost) AS avg_material_cost_per_wo,
    AVG(wo.labor_cost) AS avg_labor_cost_per_wo,
    AVG(wo.overhead_cost) AS avg_overhead_cost_per_wo,
    AVG(wo.total_cost) AS avg_total_cost_per_wo,
    
    -- Cost Per Unit
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.material_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS material_cost_per_unit,
    
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.labor_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS labor_cost_per_unit,
    
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.overhead_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS overhead_cost_per_unit,
    
    CASE 
        WHEN SUM(wo.quantity_completed) > 0 
        THEN SUM(wo.total_cost) / SUM(wo.quantity_completed)
        ELSE 0
    END AS total_cost_per_unit,
    
    -- Cost Distribution
    CASE 
        WHEN SUM(wo.total_cost) > 0 
        THEN (SUM(wo.material_cost) / SUM(wo.total_cost) * 100)
        ELSE 0
    END AS material_cost_percentage,
    
    CASE 
        WHEN SUM(wo.total_cost) > 0 
        THEN (SUM(wo.labor_cost) / SUM(wo.total_cost) * 100)
        ELSE 0
    END AS labor_cost_percentage,
    
    CASE 
        WHEN SUM(wo.total_cost) > 0 
        THEN (SUM(wo.overhead_cost) / SUM(wo.total_cost) * 100)
        ELSE 0
    END AS overhead_cost_percentage
FROM manufacturing.work_orders wo
WHERE wo.status IN ('COMPLETED', 'CLOSED')
GROUP BY wo.organization_id, wo.product_id, wo.product_code, wo.product_name;

-- =====================================================
-- View: Manufacturing Performance Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_manufacturing_performance AS
SELECT 
    wo.organization_id,
    
    -- Overall Metrics (Last 30 Days)
    COUNT(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) AS completed_last_30_days,
    SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN wo.quantity_completed ELSE 0 END) AS units_produced_last_30_days,
    
    -- Throughput
    CASE 
        WHEN COUNT(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) > 0 
        THEN SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN wo.quantity_completed ELSE 0 END) / 
             COUNT(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END)
        ELSE 0
    END AS avg_units_per_work_order,
    
    -- Cycle Time (Average)
    AVG(
        CASE 
            WHEN wo.actual_end_date IS NOT NULL AND wo.actual_start_date IS NOT NULL
            AND wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days'
            THEN EXTRACT(EPOCH FROM (wo.actual_end_date - wo.actual_start_date)) / 3600
            ELSE NULL
        END
    ) AS avg_cycle_time_hours,
    
    -- Lead Time (Planned vs Actual)
    AVG(
        CASE 
            WHEN wo.actual_end_date IS NOT NULL 
            AND wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days'
            THEN EXTRACT(EPOCH FROM (wo.actual_end_date - wo.created_at)) / 86400
            ELSE NULL
        END
    ) AS avg_lead_time_days,
    
    -- Quality Performance
    CASE 
        WHEN SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                      THEN wo.quantity_completed + wo.quantity_scrapped ELSE 0 END) > 0 
        THEN (SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                       THEN wo.quantity_completed ELSE 0 END)::DECIMAL / 
              SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                       THEN wo.quantity_completed + wo.quantity_scrapped ELSE 0 END) * 100)
        ELSE 0
    END AS first_pass_yield_30_days,
    
    -- On-Time Delivery
    CASE 
        WHEN COUNT(CASE WHEN wo.status = 'COMPLETED' 
                        AND wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) > 0 
        THEN (COUNT(CASE WHEN wo.status = 'COMPLETED' 
                         AND wo.actual_end_date <= wo.planned_end_date 
                         AND wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END)::DECIMAL /
              COUNT(CASE WHEN wo.status = 'COMPLETED' 
                         AND wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) * 100)
        ELSE 0
    END AS on_time_delivery_30_days,
    
    -- Capacity Utilization
    CASE 
        WHEN SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                      THEN EXTRACT(EPOCH FROM (wo.planned_end_date - wo.planned_start_date)) ELSE 0 END) > 0 
        THEN (SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                       THEN EXTRACT(EPOCH FROM (wo.actual_end_date - wo.actual_start_date)) ELSE 0 END) /
              SUM(CASE WHEN wo.actual_end_date >= CURRENT_DATE - INTERVAL '30 days' 
                       THEN EXTRACT(EPOCH FROM (wo.planned_end_date - wo.planned_start_date)) ELSE 0 END) * 100)
        ELSE 0
    END AS capacity_utilization_30_days
FROM manufacturing.work_orders wo
GROUP BY wo.organization_id;


