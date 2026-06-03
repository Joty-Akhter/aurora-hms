--liquibase formatted sql

--changeset easyops:049-create-manufacturing-quality-equipment-views splitStatements:false

-- =====================================================
-- View: Quality Inspection Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_quality_inspection_summary AS
SELECT 
    qi.inspection_id,
    qi.organization_id,
    qi.inspection_number,
    qi.work_order_id,
    qi.product_id,
    qi.product_code,
    qi.product_name,
    qi.inspection_type,
    qi.inspection_date,
    qi.inspector_name,
    qi.status,
    qi.overall_result,
    qi.sample_size,
    qi.quantity_inspected,
    qi.quantity_passed,
    qi.quantity_failed,
    qi.defects_found,
    qi.critical_defects,
    qi.major_defects,
    qi.minor_defects,
    
    -- Pass Rate Calculation
    CASE 
        WHEN qi.quantity_inspected > 0 
        THEN (qi.quantity_passed / qi.quantity_inspected * 100)
        ELSE 0
    END AS calculated_pass_rate,
    
    -- Defect Rate Calculation
    CASE 
        WHEN qi.quantity_inspected > 0 
        THEN (qi.defects_found::DECIMAL / qi.quantity_inspected)
        ELSE 0
    END AS calculated_defect_rate,
    
    -- Number of Parameters Tested
    (SELECT COUNT(*) FROM manufacturing.quality_inspection_items WHERE inspection_id = qi.inspection_id) AS parameters_tested,
    
    -- Number of Failed Parameters
    (SELECT COUNT(*) FROM manufacturing.quality_inspection_items WHERE inspection_id = qi.inspection_id AND pass_fail = 'FAIL') AS parameters_failed,
    
    qi.created_at,
    qi.updated_at
FROM manufacturing.quality_inspections qi;

-- =====================================================
-- View: Quality Metrics by Product
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_quality_metrics_by_product AS
SELECT 
    qi.organization_id,
    qi.product_id,
    qi.product_code,
    qi.product_name,
    COUNT(qi.inspection_id) AS total_inspections,
    COUNT(CASE WHEN qi.overall_result = 'PASS' THEN 1 END) AS passed_inspections,
    COUNT(CASE WHEN qi.overall_result = 'FAIL' THEN 1 END) AS failed_inspections,
    SUM(qi.quantity_inspected) AS total_quantity_inspected,
    SUM(qi.quantity_passed) AS total_quantity_passed,
    SUM(qi.quantity_failed) AS total_quantity_failed,
    SUM(qi.defects_found) AS total_defects,
    SUM(qi.critical_defects) AS total_critical_defects,
    SUM(qi.major_defects) AS total_major_defects,
    SUM(qi.minor_defects) AS total_minor_defects,
    
    -- Average Pass Rate
    AVG(CASE 
        WHEN qi.quantity_inspected > 0 
        THEN (qi.quantity_passed / qi.quantity_inspected * 100)
        ELSE 0
    END) AS avg_pass_rate,
    
    -- Average Defect Rate
    AVG(CASE 
        WHEN qi.quantity_inspected > 0 
        THEN (qi.defects_found::DECIMAL / qi.quantity_inspected)
        ELSE 0
    END) AS avg_defect_rate,
    
    -- First Pass Yield
    CASE 
        WHEN SUM(qi.quantity_inspected) > 0 
        THEN (SUM(qi.quantity_passed)::DECIMAL / SUM(qi.quantity_inspected) * 100)
        ELSE 0
    END AS first_pass_yield
FROM manufacturing.quality_inspections qi
WHERE qi.status = 'COMPLETED'
GROUP BY qi.organization_id, qi.product_id, qi.product_code, qi.product_name;

-- =====================================================
-- View: Non-Conformance Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_non_conformance_summary AS
SELECT 
    nc.nc_id,
    nc.organization_id,
    nc.nc_number,
    nc.work_order_id,
    nc.product_id,
    nc.product_code,
    nc.product_name,
    nc.nc_type,
    nc.severity,
    nc.category,
    nc.status,
    nc.description,
    nc.quantity_affected,
    nc.quantity_reworked,
    nc.quantity_scrapped,
    nc.root_cause,
    nc.disposition,
    nc.cost_impact,
    nc.reported_date,
    nc.resolved_date,
    
    -- Resolution Time
    CASE 
        WHEN nc.resolved_date IS NOT NULL 
        THEN EXTRACT(DAY FROM (nc.resolved_date - nc.reported_date))
        ELSE EXTRACT(DAY FROM (CURRENT_TIMESTAMP - nc.reported_date))
    END AS days_to_resolve,
    
    -- Open/Closed Status
    CASE 
        WHEN nc.status IN ('OPEN', 'IN_REVIEW') THEN 'OPEN'
        WHEN nc.status = 'CLOSED' THEN 'CLOSED'
        ELSE 'IN_PROGRESS'
    END AS simplified_status,
    
    nc.created_at
FROM manufacturing.non_conformances nc;

-- =====================================================
-- View: Non-Conformance Analytics
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_non_conformance_analytics AS
SELECT 
    nc.organization_id,
    nc.nc_type,
    nc.severity,
    nc.category,
    nc.root_cause,
    COUNT(*) AS nc_count,
    SUM(nc.quantity_affected) AS total_quantity_affected,
    SUM(nc.quantity_reworked) AS total_quantity_reworked,
    SUM(nc.quantity_scrapped) AS total_quantity_scrapped,
    SUM(nc.cost_impact) AS total_cost_impact,
    
    -- Average Resolution Time
    AVG(CASE 
        WHEN nc.resolved_date IS NOT NULL 
        THEN EXTRACT(DAY FROM (nc.resolved_date - nc.reported_date))
        ELSE NULL
    END) AS avg_resolution_days,
    
    -- Open vs Closed
    COUNT(CASE WHEN nc.status IN ('OPEN', 'IN_REVIEW') THEN 1 END) AS open_count,
    COUNT(CASE WHEN nc.status = 'CLOSED' THEN 1 END) AS closed_count
FROM manufacturing.non_conformances nc
GROUP BY nc.organization_id, nc.nc_type, nc.severity, nc.category, nc.root_cause;

-- =====================================================
-- View: Work Center Utilization
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_work_center_utilization AS
SELECT 
    wc.work_center_id,
    wc.organization_id,
    wc.work_center_code,
    wc.work_center_name,
    wc.work_center_type,
    wc.status,
    wc.capacity_per_hour,
    wc.efficiency_percentage,
    wc.utilization_percentage,
    wc.cost_per_hour,
    
    -- Active Operations Count
    (SELECT COUNT(*) 
     FROM manufacturing.work_order_operations woo 
     WHERE woo.work_center_code = wc.work_center_code 
     AND woo.status IN ('PENDING', 'IN_PROGRESS')
    ) AS active_operations,
    
    -- Completed Operations Count (last 30 days)
    (SELECT COUNT(*) 
     FROM manufacturing.work_order_operations woo 
     WHERE woo.work_center_code = wc.work_center_code 
     AND woo.status = 'COMPLETED'
     AND woo.actual_end >= CURRENT_DATE - INTERVAL '30 days'
    ) AS operations_last_30_days,
    
    -- Average Efficiency (last 30 days)
    (SELECT AVG(
         CASE 
             WHEN woo.total_time_actual > 0 
             THEN (woo.total_time_planned / woo.total_time_actual * 100)
             ELSE NULL
         END
     )
     FROM manufacturing.work_order_operations woo 
     WHERE woo.work_center_code = wc.work_center_code 
     AND woo.status = 'COMPLETED'
     AND woo.actual_end >= CURRENT_DATE - INTERVAL '30 days'
    ) AS actual_efficiency_30_days,
    
    -- Maintenance Status
    wc.last_maintenance_date,
    wc.next_maintenance_date,
    CASE 
        WHEN wc.next_maintenance_date < CURRENT_DATE THEN 'OVERDUE'
        WHEN wc.next_maintenance_date < CURRENT_DATE + INTERVAL '7 days' THEN 'DUE_SOON'
        ELSE 'OK'
    END AS maintenance_status
FROM manufacturing.work_centers wc
WHERE wc.is_active = true;

-- =====================================================
-- View: Maintenance Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_maintenance_summary AS
SELECT 
    em.maintenance_id,
    em.organization_id,
    em.maintenance_number,
    em.work_center_id,
    wc.work_center_code,
    wc.work_center_name,
    em.maintenance_type,
    em.priority,
    em.status,
    em.scheduled_date,
    em.actual_start_date,
    em.actual_end_date,
    em.scheduled_duration_hours,
    em.actual_duration_hours,
    em.downtime_hours,
    em.labor_cost,
    em.parts_cost,
    em.total_cost,
    em.production_loss,
    
    -- Variance
    CASE 
        WHEN em.actual_duration_hours IS NOT NULL AND em.scheduled_duration_hours IS NOT NULL
        THEN (em.actual_duration_hours - em.scheduled_duration_hours)
        ELSE NULL
    END AS duration_variance_hours,
    
    -- On-Time Completion
    CASE 
        WHEN em.status = 'COMPLETED' AND em.actual_end_date <= em.scheduled_date THEN true
        WHEN em.status = 'COMPLETED' AND em.actual_end_date > em.scheduled_date THEN false
        ELSE NULL
    END AS completed_on_time,
    
    -- Overdue Status
    CASE 
        WHEN em.status IN ('SCHEDULED', 'IN_PROGRESS') AND em.scheduled_date < CURRENT_TIMESTAMP THEN 'OVERDUE'
        WHEN em.status IN ('SCHEDULED', 'IN_PROGRESS') AND em.scheduled_date < CURRENT_TIMESTAMP + INTERVAL '24 hours' THEN 'DUE_SOON'
        ELSE 'ON_SCHEDULE'
    END AS schedule_status,
    
    em.created_at
FROM manufacturing.equipment_maintenance em
JOIN manufacturing.work_centers wc ON em.work_center_id = wc.work_center_id;

-- =====================================================
-- View: Quality Dashboard
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_quality_dashboard AS
SELECT 
    qi.organization_id,
    
    -- Today's Inspections
    COUNT(CASE WHEN DATE(qi.inspection_date) = CURRENT_DATE THEN 1 END) AS inspections_today,
    COUNT(CASE WHEN DATE(qi.inspection_date) = CURRENT_DATE AND qi.overall_result = 'PASS' THEN 1 END) AS passed_today,
    COUNT(CASE WHEN DATE(qi.inspection_date) = CURRENT_DATE AND qi.overall_result = 'FAIL' THEN 1 END) AS failed_today,
    
    -- This Week's Inspections
    COUNT(CASE WHEN qi.inspection_date >= DATE_TRUNC('week', CURRENT_DATE) THEN 1 END) AS inspections_this_week,
    
    -- This Month's Inspections
    COUNT(CASE WHEN qi.inspection_date >= DATE_TRUNC('month', CURRENT_DATE) THEN 1 END) AS inspections_this_month,
    
    -- Overall Pass Rate (Last 30 Days)
    AVG(CASE 
        WHEN qi.inspection_date >= CURRENT_DATE - INTERVAL '30 days' 
        AND qi.quantity_inspected > 0 
        THEN (qi.quantity_passed / qi.quantity_inspected * 100)
        ELSE NULL
    END) AS pass_rate_last_30_days,
    
    -- Open Non-Conformances
    (SELECT COUNT(*) 
     FROM manufacturing.non_conformances nc 
     WHERE nc.organization_id = qi.organization_id 
     AND nc.status IN ('OPEN', 'IN_REVIEW')
    ) AS open_non_conformances,
    
    -- Critical Non-Conformances
    (SELECT COUNT(*) 
     FROM manufacturing.non_conformances nc 
     WHERE nc.organization_id = qi.organization_id 
     AND nc.severity = 'CRITICAL'
     AND nc.status IN ('OPEN', 'IN_REVIEW')
    ) AS critical_non_conformances
FROM manufacturing.quality_inspections qi
GROUP BY qi.organization_id;


