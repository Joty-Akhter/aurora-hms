--liquibase formatted sql

--changeset easyops:041-create-crm-activities-campaigns-views splitStatements:false

-- =====================================================
-- View: Campaign Summary with Performance Metrics
-- =====================================================
CREATE OR REPLACE VIEW crm.v_campaign_summary AS
SELECT 
    c.campaign_id,
    c.organization_id,
    c.campaign_number,
    c.campaign_name,
    c.campaign_type,
    c.status,
    c.start_date,
    c.end_date,
    c.budgeted_cost,
    c.actual_cost,
    c.currency,
    c.expected_response_count,
    c.expected_revenue,
    c.owner_id,
    c.priority,
    
    -- Member Counts
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) AS total_members,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_sent = true) AS sent_count,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND responded = true) AS response_count,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND converted = true) AS conversion_count,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_opened = true) AS opened_count,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_clicked = true) AS clicked_count,
    
    -- Performance Metrics
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_sent = true) > 0 THEN
            ROUND(100.0 * (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_opened = true) / 
                  (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_sent = true), 2)
        ELSE 0
    END AS open_rate_percent,
    
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_sent = true) > 0 THEN
            ROUND(100.0 * (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_clicked = true) / 
                  (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND email_sent = true), 2)
        ELSE 0
    END AS click_rate_percent,
    
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) > 0 THEN
            ROUND(100.0 * (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND responded = true) / 
                  (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id), 2)
        ELSE 0
    END AS response_rate_percent,
    
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) > 0 THEN
            ROUND(100.0 * (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND converted = true) / 
                  (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id), 2)
        ELSE 0
    END AS conversion_rate_percent,
    
    -- ROI Calculation
    CASE 
        WHEN c.actual_cost > 0 AND c.expected_revenue IS NOT NULL THEN
            ROUND(((c.expected_revenue - c.actual_cost) / c.actual_cost) * 100, 2)
        ELSE NULL
    END AS roi_percent,
    
    c.created_at,
    c.updated_at
FROM crm.campaigns c;

-- =====================================================
-- View: Campaign Performance by Type
-- =====================================================
CREATE OR REPLACE VIEW crm.v_campaign_performance_by_type AS
SELECT 
    c.organization_id,
    c.campaign_type,
    COUNT(*) AS campaign_count,
    COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) AS active_campaigns,
    COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) AS completed_campaigns,
    
    -- Total Members
    (SELECT COUNT(*) FROM crm.campaign_members cm 
     JOIN crm.campaigns c2 ON cm.campaign_id = c2.campaign_id 
     WHERE c2.campaign_type = c.campaign_type 
     AND c2.organization_id = c.organization_id) AS total_members,
    
    -- Total Conversions
    (SELECT COUNT(*) FROM crm.campaign_members cm 
     JOIN crm.campaigns c2 ON cm.campaign_id = c2.campaign_id 
     WHERE c2.campaign_type = c.campaign_type 
     AND c2.organization_id = c.organization_id 
     AND cm.converted = true) AS total_conversions,
    
    -- Average Metrics
    AVG(c.actual_cost) AS avg_cost,
    AVG(c.expected_revenue) AS avg_expected_revenue,
    
    -- Budget Totals
    SUM(c.budgeted_cost) AS total_budgeted_cost,
    SUM(c.actual_cost) AS total_actual_cost,
    SUM(c.expected_revenue) AS total_expected_revenue
    
FROM crm.campaigns c
GROUP BY c.organization_id, c.campaign_type;

-- =====================================================
-- View: Task Summary
-- =====================================================
CREATE OR REPLACE VIEW crm.v_task_summary AS
SELECT 
    t.task_id,
    t.organization_id,
    t.subject,
    t.description,
    t.task_type,
    t.status,
    t.priority,
    t.due_date,
    t.reminder_date,
    t.completed_date,
    t.assigned_to,
    t.assigned_by,
    
    -- Related Records
    t.lead_id,
    l.first_name AS lead_first_name,
    l.last_name AS lead_last_name,
    l.company AS lead_company,
    
    t.contact_id,
    c.first_name AS contact_first_name,
    c.last_name AS contact_last_name,
    
    t.account_id,
    a.account_name,
    
    t.opportunity_id,
    o.opportunity_name,
    
    t.campaign_id,
    cp.campaign_name,
    
    -- Status Indicators
    CASE 
        WHEN t.status = 'COMPLETED' THEN true
        WHEN t.due_date < CURRENT_DATE AND t.status != 'COMPLETED' THEN false
        ELSE NULL
    END AS is_overdue,
    
    CASE 
        WHEN t.due_date = CURRENT_DATE AND t.status != 'COMPLETED' THEN true
        ELSE false
    END AS is_due_today,
    
    t.created_at,
    t.updated_at
FROM crm.tasks t
LEFT JOIN crm.leads l ON t.lead_id = l.lead_id
LEFT JOIN crm.contacts c ON t.contact_id = c.contact_id
LEFT JOIN crm.accounts a ON t.account_id = a.account_id
LEFT JOIN crm.opportunities o ON t.opportunity_id = o.opportunity_id
LEFT JOIN crm.campaigns cp ON t.campaign_id = cp.campaign_id;

-- =====================================================
-- View: Task Statistics by User
-- =====================================================
CREATE OR REPLACE VIEW crm.v_task_stats_by_user AS
SELECT 
    t.organization_id,
    t.assigned_to,
    
    -- Today
    COUNT(CASE WHEN t.due_date = CURRENT_DATE AND t.status != 'COMPLETED' THEN 1 END) AS due_today,
    
    -- This Week
    COUNT(CASE 
        WHEN t.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days' 
        AND t.status != 'COMPLETED' THEN 1 
    END) AS due_this_week,
    
    -- Overdue
    COUNT(CASE WHEN t.due_date < CURRENT_DATE AND t.status != 'COMPLETED' THEN 1 END) AS overdue,
    
    -- By Status
    COUNT(CASE WHEN t.status = 'NOT_STARTED' THEN 1 END) AS not_started,
    COUNT(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 END) AS in_progress,
    COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) AS completed,
    COUNT(CASE WHEN t.status = 'WAITING' THEN 1 END) AS waiting,
    
    -- By Priority
    COUNT(CASE WHEN t.priority = 'HIGH' THEN 1 END) AS high_priority,
    COUNT(CASE WHEN t.priority = 'MEDIUM' THEN 1 END) AS medium_priority,
    COUNT(CASE WHEN t.priority = 'LOW' THEN 1 END) AS low_priority,
    
    -- Total
    COUNT(*) AS total_tasks
    
FROM crm.tasks t
GROUP BY t.organization_id, t.assigned_to;

-- =====================================================
-- View: Event Summary
-- =====================================================
CREATE OR REPLACE VIEW crm.v_event_summary AS
SELECT 
    e.event_id,
    e.organization_id,
    e.subject,
    e.description,
    e.event_type,
    e.start_datetime,
    e.end_datetime,
    e.all_day,
    e.location,
    e.meeting_url,
    e.status,
    e.organizer_id,
    
    -- Related Records
    e.lead_id,
    l.first_name AS lead_first_name,
    l.last_name AS lead_last_name,
    
    e.contact_id,
    c.first_name AS contact_first_name,
    c.last_name AS contact_last_name,
    
    e.account_id,
    a.account_name,
    
    e.opportunity_id,
    o.opportunity_name,
    
    e.campaign_id,
    cp.campaign_name,
    
    -- Duration
    EXTRACT(EPOCH FROM (e.end_datetime - e.start_datetime)) / 3600 AS duration_hours,
    
    -- Status Indicators
    CASE 
        WHEN e.start_datetime < CURRENT_TIMESTAMP AND e.end_datetime > CURRENT_TIMESTAMP THEN true
        ELSE false
    END AS is_happening_now,
    
    CASE 
        WHEN DATE(e.start_datetime) = CURRENT_DATE THEN true
        ELSE false
    END AS is_today,
    
    e.created_at,
    e.updated_at
FROM crm.events e
LEFT JOIN crm.leads l ON e.lead_id = l.lead_id
LEFT JOIN crm.contacts c ON e.contact_id = c.contact_id
LEFT JOIN crm.accounts a ON e.account_id = a.account_id
LEFT JOIN crm.opportunities o ON e.opportunity_id = o.opportunity_id
LEFT JOIN crm.campaigns cp ON e.campaign_id = cp.campaign_id;

-- =====================================================
-- View: Event Calendar View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_event_calendar AS
SELECT 
    e.event_id,
    e.organization_id,
    e.subject,
    e.event_type,
    e.start_datetime,
    e.end_datetime,
    e.all_day,
    e.status,
    e.organizer_id,
    DATE(e.start_datetime) AS event_date,
    EXTRACT(HOUR FROM e.start_datetime) AS start_hour,
    EXTRACT(HOUR FROM e.end_datetime) AS end_hour,
    
    -- Related info
    CASE 
        WHEN e.lead_id IS NOT NULL THEN 'Lead'
        WHEN e.contact_id IS NOT NULL THEN 'Contact'
        WHEN e.account_id IS NOT NULL THEN 'Account'
        WHEN e.opportunity_id IS NOT NULL THEN 'Opportunity'
        WHEN e.campaign_id IS NOT NULL THEN 'Campaign'
        ELSE 'General'
    END AS related_type,
    
    COALESCE(
        (SELECT company FROM crm.leads WHERE lead_id = e.lead_id),
        (SELECT account_name FROM crm.accounts WHERE account_id = e.account_id),
        'N/A'
    ) AS related_name
    
FROM crm.events e
WHERE e.status != 'CANCELLED';

-- =====================================================
-- View: Activities Dashboard Stats
-- =====================================================
CREATE OR REPLACE VIEW crm.v_activities_dashboard_stats AS
SELECT 
    t.organization_id,
    
    -- Task Stats
    (SELECT COUNT(*) FROM crm.tasks WHERE organization_id = t.organization_id AND status != 'COMPLETED') AS open_tasks,
    (SELECT COUNT(*) FROM crm.tasks WHERE organization_id = t.organization_id AND due_date < CURRENT_DATE AND status != 'COMPLETED') AS overdue_tasks,
    (SELECT COUNT(*) FROM crm.tasks WHERE organization_id = t.organization_id AND due_date = CURRENT_DATE AND status != 'COMPLETED') AS tasks_due_today,
    
    -- Event Stats
    (SELECT COUNT(*) FROM crm.events WHERE organization_id = t.organization_id AND DATE(start_datetime) = CURRENT_DATE) AS events_today,
    (SELECT COUNT(*) FROM crm.events WHERE organization_id = t.organization_id AND DATE(start_datetime) BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days') AS events_this_week,
    
    -- Campaign Stats
    (SELECT COUNT(*) FROM crm.campaigns WHERE organization_id = t.organization_id AND status = 'ACTIVE') AS active_campaigns,
    (SELECT COUNT(*) FROM crm.campaigns WHERE organization_id = t.organization_id) AS total_campaigns,
    
    -- Email Template Stats
    (SELECT COUNT(*) FROM crm.email_templates WHERE organization_id = t.organization_id AND is_active = true) AS active_email_templates
    
FROM (SELECT DISTINCT organization_id FROM crm.tasks) t;

-- =====================================================
-- View: Campaign ROI Analysis
-- =====================================================
CREATE OR REPLACE VIEW crm.v_campaign_roi_analysis AS
SELECT 
    c.campaign_id,
    c.organization_id,
    c.campaign_name,
    c.campaign_type,
    c.budgeted_cost,
    c.actual_cost,
    c.expected_revenue,
    c.currency,
    
    -- Member Stats
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) AS total_members,
    (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND converted = true) AS conversions,
    
    -- Cost per Member
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) > 0 THEN
            c.actual_cost / (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id)
        ELSE NULL
    END AS cost_per_member,
    
    -- Cost per Conversion
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND converted = true) > 0 THEN
            c.actual_cost / (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id AND converted = true)
        ELSE NULL
    END AS cost_per_conversion,
    
    -- ROI
    CASE 
        WHEN c.actual_cost > 0 THEN
            ROUND(((c.expected_revenue - c.actual_cost) / c.actual_cost) * 100, 2)
        ELSE NULL
    END AS roi_percent,
    
    -- Revenue per Member
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id) > 0 THEN
            c.expected_revenue / (SELECT COUNT(*) FROM crm.campaign_members WHERE campaign_id = c.campaign_id)
        ELSE NULL
    END AS revenue_per_member
    
FROM crm.campaigns c
WHERE c.status IN ('ACTIVE', 'COMPLETED');


