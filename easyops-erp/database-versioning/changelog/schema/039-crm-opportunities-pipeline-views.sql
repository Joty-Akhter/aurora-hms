--liquibase formatted sql

--changeset easyops:039-create-crm-opportunities-pipeline-views splitStatements:false endDelimiter:GO

-- =====================================================
-- View: Opportunity Summary with Related Data
-- =====================================================
CREATE OR REPLACE VIEW crm.v_opportunity_summary AS
SELECT 
    o.opportunity_id,
    o.organization_id,
    o.opportunity_number,
    o.opportunity_name,
    o.amount,
    o.currency,
    o.probability,
    o.expected_revenue,
    o.close_date,
    o.expected_close_date,
    o.status,
    o.type,
    o.priority,
    o.next_step,
    o.days_in_stage,
    o.total_days_open,
    
    -- Stage Info
    s.stage_name,
    s.stage_order,
    s.probability AS stage_probability,
    
    -- Account Info
    a.account_name,
    a.industry AS account_industry,
    
    -- Contact Info
    c.first_name AS contact_first_name,
    c.last_name AS contact_last_name,
    c.email AS contact_email,
    
    -- Lead Source
    ls.source_name,
    o.source_campaign,
    
    -- Product Count and Total
    (SELECT COUNT(*) FROM crm.opportunity_products WHERE opportunity_id = o.opportunity_id) AS product_count,
    (SELECT COALESCE(SUM(line_total), 0) FROM crm.opportunity_products WHERE opportunity_id = o.opportunity_id) AS products_total,
    
    -- Activity Count
    (SELECT COUNT(*) FROM crm.opportunity_activities WHERE opportunity_id = o.opportunity_id) AS activity_count,
    (SELECT MAX(activity_date) FROM crm.opportunity_activities WHERE opportunity_id = o.opportunity_id) AS last_activity_date,
    
    o.created_at,
    o.updated_at
FROM crm.opportunities o
LEFT JOIN crm.opportunity_stages s ON o.stage_id = s.stage_id
LEFT JOIN crm.accounts a ON o.account_id = a.account_id
LEFT JOIN crm.contacts c ON o.contact_id = c.contact_id
LEFT JOIN crm.lead_sources ls ON o.lead_source_id = ls.source_id;

-- =====================================================
-- View: Pipeline Analysis by Stage
-- =====================================================
CREATE OR REPLACE VIEW crm.v_pipeline_by_stage AS
SELECT 
    o.organization_id,
    s.stage_id,
    s.stage_name,
    s.stage_order,
    s.probability AS stage_probability,
    COUNT(o.opportunity_id) AS opportunity_count,
    SUM(o.amount) AS total_amount,
    SUM(o.expected_revenue) AS total_expected_revenue,
    AVG(o.amount) AS avg_amount,
    AVG(o.days_in_stage) AS avg_days_in_stage,
    MIN(o.amount) AS min_amount,
    MAX(o.amount) AS max_amount
FROM crm.opportunity_stages s
LEFT JOIN crm.opportunities o ON s.stage_id = o.stage_id AND o.status = 'OPEN'
GROUP BY o.organization_id, s.stage_id, s.stage_name, s.stage_order, s.probability
ORDER BY s.stage_order;

-- =====================================================
-- View: Sales Forecast
-- =====================================================
CREATE OR REPLACE VIEW crm.v_sales_forecast AS
SELECT 
    o.organization_id,
    DATE_TRUNC('month', o.expected_close_date) AS forecast_month,
    DATE_TRUNC('quarter', o.expected_close_date) AS forecast_quarter,
    EXTRACT(YEAR FROM o.expected_close_date) AS forecast_year,
    
    -- Open Opportunities
    COUNT(CASE WHEN o.status = 'OPEN' THEN 1 END) AS open_count,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.amount ELSE 0 END) AS open_amount,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.expected_revenue ELSE 0 END) AS open_weighted_amount,
    
    -- Won Opportunities
    COUNT(CASE WHEN o.status = 'WON' THEN 1 END) AS won_count,
    SUM(CASE WHEN o.status = 'WON' THEN o.amount ELSE 0 END) AS won_amount,
    
    -- Lost Opportunities
    COUNT(CASE WHEN o.status = 'LOST' THEN 1 END) AS lost_count,
    SUM(CASE WHEN o.status = 'LOST' THEN o.amount ELSE 0 END) AS lost_amount,
    
    -- Total
    COUNT(*) AS total_count,
    SUM(o.amount) AS total_amount
FROM crm.opportunities o
WHERE o.expected_close_date IS NOT NULL
GROUP BY o.organization_id, forecast_month, forecast_quarter, forecast_year
ORDER BY forecast_year, forecast_month;

-- =====================================================
-- View: Win/Loss Analysis
-- =====================================================
CREATE OR REPLACE VIEW crm.v_win_loss_analysis AS
SELECT 
    o.organization_id,
    o.status,
    DATE_TRUNC('month', o.close_date) AS close_month,
    
    -- Count and Amount
    COUNT(*) AS opportunity_count,
    SUM(o.amount) AS total_amount,
    AVG(o.amount) AS avg_amount,
    
    -- Win Rate (for closed opportunities)
    CASE 
        WHEN COUNT(*) > 0 THEN 
            ROUND(100.0 * COUNT(CASE WHEN o.status = 'WON' THEN 1 END) / COUNT(*), 2)
        ELSE 0 
    END AS win_rate_percent,
    
    -- Sales Cycle
    AVG(o.total_days_open) AS avg_sales_cycle_days,
    
    -- By Type
    COUNT(CASE WHEN o.type = 'NEW_BUSINESS' THEN 1 END) AS new_business_count,
    COUNT(CASE WHEN o.type = 'EXISTING_BUSINESS' THEN 1 END) AS existing_business_count,
    COUNT(CASE WHEN o.type = 'RENEWAL' THEN 1 END) AS renewal_count,
    COUNT(CASE WHEN o.type = 'UPSELL' THEN 1 END) AS upsell_count,
    
    -- By Source
    ls.source_name AS top_lead_source
FROM crm.opportunities o
LEFT JOIN crm.lead_sources ls ON o.lead_source_id = ls.source_id
WHERE o.status IN ('WON', 'LOST')
GROUP BY o.organization_id, o.status, close_month, ls.source_name
ORDER BY close_month DESC;

-- =====================================================
-- View: Opportunity Product Details
-- =====================================================
CREATE OR REPLACE VIEW crm.v_opportunity_products_detail AS
SELECT 
    op.opp_product_id,
    op.opportunity_id,
    op.organization_id,
    
    -- Opportunity Info
    o.opportunity_number,
    o.opportunity_name,
    o.status AS opportunity_status,
    
    -- Product Info
    op.product_code,
    op.product_name,
    op.product_description,
    op.quantity,
    op.unit_price,
    op.discount_percent,
    op.discount_amount,
    op.tax_percent,
    
    -- Calculations
    op.subtotal,
    op.discount_total,
    op.tax_amount,
    op.line_total,
    
    op.line_order,
    op.notes,
    op.created_at,
    op.updated_at
FROM crm.opportunity_products op
JOIN crm.opportunities o ON op.opportunity_id = o.opportunity_id
ORDER BY o.opportunity_number, op.line_order;

-- =====================================================
-- View: Sales Performance by Owner
-- =====================================================
CREATE OR REPLACE VIEW crm.v_sales_performance_by_owner AS
SELECT 
    o.organization_id,
    o.owner_id,
    DATE_TRUNC('month', CURRENT_DATE) AS current_month,
    
    -- This Month - Open
    COUNT(CASE WHEN o.status = 'OPEN' THEN 1 END) AS open_opportunities,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.amount ELSE 0 END) AS open_pipeline_value,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.expected_revenue ELSE 0 END) AS open_weighted_value,
    
    -- This Month - Won
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('month', o.close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN 1 
    END) AS won_this_month,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('month', o.close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_amount_this_month,
    
    -- This Quarter - Won
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('quarter', o.close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN 1 
    END) AS won_this_quarter,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('quarter', o.close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_amount_this_quarter,
    
    -- This Year - Won
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND EXTRACT(YEAR FROM o.close_date) = EXTRACT(YEAR FROM CURRENT_DATE) 
        THEN 1 
    END) AS won_this_year,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND EXTRACT(YEAR FROM o.close_date) = EXTRACT(YEAR FROM CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_amount_this_year,
    
    -- Overall Win Rate
    CASE 
        WHEN COUNT(CASE WHEN o.status IN ('WON', 'LOST') THEN 1 END) > 0 THEN
            ROUND(100.0 * COUNT(CASE WHEN o.status = 'WON' THEN 1 END) / 
                  COUNT(CASE WHEN o.status IN ('WON', 'LOST') THEN 1 END), 2)
        ELSE 0
    END AS overall_win_rate,
    
    -- Average Deal Size
    AVG(CASE WHEN o.status = 'WON' THEN o.amount END) AS avg_deal_size,
    
    -- Average Sales Cycle
    AVG(CASE WHEN o.status = 'WON' THEN o.total_days_open END) AS avg_sales_cycle_days
    
FROM crm.opportunities o
GROUP BY o.organization_id, o.owner_id;

-- =====================================================
-- View: Opportunity Activities Timeline
-- =====================================================
CREATE OR REPLACE VIEW crm.v_opportunity_activities_timeline AS
SELECT 
    oa.activity_id,
    oa.opportunity_id,
    oa.organization_id,
    
    -- Opportunity Info
    o.opportunity_number,
    o.opportunity_name,
    o.status AS opportunity_status,
    
    -- Activity Info
    oa.activity_type,
    oa.subject,
    oa.description,
    oa.activity_date,
    oa.duration_minutes,
    oa.status AS activity_status,
    oa.priority,
    oa.outcome,
    oa.next_action,
    
    -- Assignment
    oa.assigned_to,
    oa.completed_by,
    
    oa.created_at,
    oa.updated_at
FROM crm.opportunity_activities oa
JOIN crm.opportunities o ON oa.opportunity_id = o.opportunity_id
ORDER BY oa.activity_date DESC;

-- =====================================================
-- View: Pipeline Dashboard Statistics
-- =====================================================
CREATE OR REPLACE VIEW crm.v_pipeline_dashboard_stats AS
SELECT 
    o.organization_id,
    
    -- Open Opportunities
    COUNT(CASE WHEN o.status = 'OPEN' THEN 1 END) AS total_open_opportunities,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.amount ELSE 0 END) AS total_pipeline_value,
    SUM(CASE WHEN o.status = 'OPEN' THEN o.expected_revenue ELSE 0 END) AS total_weighted_value,
    
    -- This Month Closing
    COUNT(CASE 
        WHEN o.status = 'OPEN' 
        AND DATE_TRUNC('month', o.expected_close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN 1 
    END) AS closing_this_month_count,
    SUM(CASE 
        WHEN o.status = 'OPEN' 
        AND DATE_TRUNC('month', o.expected_close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN o.expected_revenue 
        ELSE 0 
    END) AS closing_this_month_value,
    
    -- This Quarter Closing
    COUNT(CASE 
        WHEN o.status = 'OPEN' 
        AND DATE_TRUNC('quarter', o.expected_close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN 1 
    END) AS closing_this_quarter_count,
    SUM(CASE 
        WHEN o.status = 'OPEN' 
        AND DATE_TRUNC('quarter', o.expected_close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN o.expected_revenue 
        ELSE 0 
    END) AS closing_this_quarter_value,
    
    -- Won This Month
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('month', o.close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN 1 
    END) AS won_this_month_count,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('month', o.close_date) = DATE_TRUNC('month', CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_this_month_value,
    
    -- Won This Quarter
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('quarter', o.close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN 1 
    END) AS won_this_quarter_count,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND DATE_TRUNC('quarter', o.close_date) = DATE_TRUNC('quarter', CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_this_quarter_value,
    
    -- Won This Year
    COUNT(CASE 
        WHEN o.status = 'WON' 
        AND EXTRACT(YEAR FROM o.close_date) = EXTRACT(YEAR FROM CURRENT_DATE) 
        THEN 1 
    END) AS won_this_year_count,
    SUM(CASE 
        WHEN o.status = 'WON' 
        AND EXTRACT(YEAR FROM o.close_date) = EXTRACT(YEAR FROM CURRENT_DATE) 
        THEN o.amount 
        ELSE 0 
    END) AS won_this_year_value,
    
    -- Average Metrics
    AVG(CASE WHEN o.status = 'OPEN' THEN o.amount END) AS avg_opportunity_size,
    AVG(CASE WHEN o.status = 'OPEN' THEN o.days_in_stage END) AS avg_days_in_stage,
    
    -- Win Rate
    CASE 
        WHEN COUNT(CASE WHEN o.status IN ('WON', 'LOST') THEN 1 END) > 0 THEN
            ROUND(100.0 * COUNT(CASE WHEN o.status = 'WON' THEN 1 END) / 
                  COUNT(CASE WHEN o.status IN ('WON', 'LOST') THEN 1 END), 2)
        ELSE 0
    END AS overall_win_rate
    
FROM crm.opportunities o
GROUP BY o.organization_id;

-- =====================================================
-- View: Opportunities Closing Soon (Next 30 Days)
-- =====================================================
CREATE OR REPLACE VIEW crm.v_opportunities_closing_soon AS
SELECT 
    o.opportunity_id,
    o.organization_id,
    o.opportunity_number,
    o.opportunity_name,
    o.amount,
    o.expected_revenue,
    o.expected_close_date,
    o.owner_id,
    s.stage_name,
    a.account_name,
    o.next_step,
    o.days_in_stage,
    (o.expected_close_date - CURRENT_DATE) AS days_until_close
FROM crm.opportunities o
LEFT JOIN crm.opportunity_stages s ON o.stage_id = s.stage_id
LEFT JOIN crm.accounts a ON o.account_id = a.account_id
WHERE o.status = 'OPEN'
  AND o.expected_close_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
ORDER BY o.expected_close_date;

GO

