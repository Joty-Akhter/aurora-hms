--liquibase formatted sql

--changeset easyops:043-create-crm-support-analytics-views splitStatements:false

-- =====================================================
-- View: Case Summary with SLA Status
-- =====================================================
CREATE OR REPLACE VIEW crm.v_case_summary AS
SELECT 
    c.case_id,
    c.organization_id,
    c.case_number,
    c.subject,
    c.description,
    c.case_type,
    c.status,
    c.priority,
    c.origin,
    c.category,
    c.subcategory,
    c.customer_rating,
    
    -- Contact/Account Info
    c.contact_id,
    con.first_name AS contact_first_name,
    con.last_name AS contact_last_name,
    con.email AS contact_email,
    
    c.account_id,
    a.account_name,
    
    -- Assignment
    c.assigned_to,
    c.assigned_at,
    
    -- SLA Info
    c.sla_id,
    sla.policy_name AS sla_policy_name,
    c.first_response_at,
    c.first_response_due,
    c.resolution_due,
    c.sla_breached,
    
    -- SLA Status
    CASE 
        WHEN c.first_response_at IS NULL AND c.first_response_due < CURRENT_TIMESTAMP THEN 'FIRST_RESPONSE_BREACHED'
        WHEN c.status NOT IN ('RESOLVED', 'CLOSED') AND c.resolution_due < CURRENT_TIMESTAMP THEN 'RESOLUTION_BREACHED'
        WHEN c.first_response_at IS NOT NULL AND c.status IN ('RESOLVED', 'CLOSED') THEN 'COMPLIANT'
        ELSE 'ON_TRACK'
    END AS sla_status,
    
    -- Time Metrics (in hours)
    CASE 
        WHEN c.first_response_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.first_response_at - c.created_at)) / 3600
        ELSE NULL
    END AS first_response_time_hours,
    
    CASE 
        WHEN c.resolved_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.resolved_at - c.created_at)) / 3600
        ELSE 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - c.created_at)) / 3600
    END AS resolution_time_hours,
    
    -- Resolution
    c.resolved_by,
    c.resolved_at,
    c.closed_at,
    c.resolution,
    
    -- Comment Count
    (SELECT COUNT(*) FROM crm.case_comments WHERE case_id = c.case_id) AS comment_count,
    
    -- Timestamps
    c.created_at,
    c.updated_at
FROM crm.cases c
LEFT JOIN crm.contacts con ON c.contact_id = con.contact_id
LEFT JOIN crm.accounts a ON c.account_id = a.account_id
LEFT JOIN crm.sla_policies sla ON c.sla_id = sla.sla_id;

-- =====================================================
-- View: Case Statistics by Agent
-- =====================================================
CREATE OR REPLACE VIEW crm.v_case_stats_by_agent AS
SELECT 
    c.organization_id,
    c.assigned_to,
    
    -- Total Cases
    COUNT(*) AS total_cases,
    
    -- By Status
    COUNT(CASE WHEN c.status = 'NEW' THEN 1 END) AS new_cases,
    COUNT(CASE WHEN c.status = 'OPEN' THEN 1 END) AS open_cases,
    COUNT(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 END) AS in_progress_cases,
    COUNT(CASE WHEN c.status = 'RESOLVED' THEN 1 END) AS resolved_cases,
    COUNT(CASE WHEN c.status = 'CLOSED' THEN 1 END) AS closed_cases,
    
    -- By Priority
    COUNT(CASE WHEN c.priority = 'CRITICAL' THEN 1 END) AS critical_cases,
    COUNT(CASE WHEN c.priority = 'HIGH' THEN 1 END) AS high_priority_cases,
    COUNT(CASE WHEN c.priority = 'MEDIUM' THEN 1 END) AS medium_priority_cases,
    COUNT(CASE WHEN c.priority = 'LOW' THEN 1 END) AS low_priority_cases,
    
    -- SLA Metrics
    COUNT(CASE WHEN c.sla_breached = true THEN 1 END) AS sla_breached_count,
    ROUND(100.0 * COUNT(CASE WHEN c.sla_breached = false AND c.status IN ('RESOLVED', 'CLOSED') THEN 1 END) / 
          NULLIF(COUNT(CASE WHEN c.status IN ('RESOLVED', 'CLOSED') THEN 1 END), 0), 2) AS sla_compliance_rate,
    
    -- Average Metrics
    AVG(CASE 
        WHEN c.first_response_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.first_response_at - c.created_at)) / 3600
        ELSE NULL
    END) AS avg_first_response_hours,
    
    AVG(CASE 
        WHEN c.resolved_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.resolved_at - c.created_at)) / 3600
        ELSE NULL
    END) AS avg_resolution_hours,
    
    -- Customer Satisfaction
    AVG(c.customer_rating) AS avg_customer_rating,
    COUNT(CASE WHEN c.customer_rating IS NOT NULL THEN 1 END) AS rated_cases_count
    
FROM crm.cases c
GROUP BY c.organization_id, c.assigned_to;

-- =====================================================
-- View: Knowledge Base Article Performance
-- =====================================================
CREATE OR REPLACE VIEW crm.v_kb_article_performance AS
SELECT 
    kb.article_id,
    kb.organization_id,
    kb.article_number,
    kb.title,
    kb.category,
    kb.subcategory,
    kb.status,
    kb.is_public,
    kb.is_featured,
    kb.view_count,
    kb.helpful_count,
    kb.not_helpful_count,
    
    -- Helpfulness Ratio
    CASE 
        WHEN (kb.helpful_count + kb.not_helpful_count) > 0 THEN
            ROUND(100.0 * kb.helpful_count / (kb.helpful_count + kb.not_helpful_count), 2)
        ELSE NULL
    END AS helpfulness_percentage,
    
    -- Total Feedback
    (kb.helpful_count + kb.not_helpful_count) AS total_feedback,
    
    kb.version,
    kb.author_id,
    kb.published_at,
    kb.last_reviewed_at,
    kb.last_reviewed_by,
    kb.created_at,
    kb.updated_at,
    
    -- Days since last update
    EXTRACT(DAY FROM (CURRENT_TIMESTAMP - kb.updated_at)) AS days_since_update,
    
    -- Days since last review
    CASE 
        WHEN kb.last_reviewed_at IS NOT NULL THEN
            EXTRACT(DAY FROM (CURRENT_TIMESTAMP - kb.last_reviewed_at))
        ELSE NULL
    END AS days_since_review
    
FROM crm.knowledge_base kb;

-- =====================================================
-- View: Support Dashboard Statistics
-- =====================================================
CREATE OR REPLACE VIEW crm.v_support_dashboard_stats AS
SELECT 
    c.organization_id,
    
    -- Today's Cases
    COUNT(CASE WHEN DATE(c.created_at) = CURRENT_DATE THEN 1 END) AS cases_created_today,
    COUNT(CASE WHEN DATE(c.resolved_at) = CURRENT_DATE THEN 1 END) AS cases_resolved_today,
    
    -- Current Status
    COUNT(CASE WHEN c.status = 'NEW' THEN 1 END) AS new_cases,
    COUNT(CASE WHEN c.status IN ('OPEN', 'IN_PROGRESS') THEN 1 END) AS active_cases,
    COUNT(CASE WHEN c.status = 'WAITING_CUSTOMER' THEN 1 END) AS waiting_customer,
    
    -- Priority
    COUNT(CASE WHEN c.priority = 'CRITICAL' AND c.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 END) AS critical_open,
    COUNT(CASE WHEN c.priority = 'HIGH' AND c.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 END) AS high_open,
    
    -- SLA
    COUNT(CASE WHEN c.first_response_at IS NULL AND c.first_response_due < CURRENT_TIMESTAMP THEN 1 END) AS overdue_first_response,
    COUNT(CASE WHEN c.status NOT IN ('RESOLVED', 'CLOSED') AND c.resolution_due < CURRENT_TIMESTAMP THEN 1 END) AS overdue_resolution,
    COUNT(CASE WHEN c.sla_breached = true THEN 1 END) AS total_sla_breached,
    
    -- This Week
    COUNT(CASE 
        WHEN c.created_at >= DATE_TRUNC('week', CURRENT_DATE) THEN 1 
    END) AS cases_this_week,
    COUNT(CASE 
        WHEN c.resolved_at >= DATE_TRUNC('week', CURRENT_DATE) THEN 1 
    END) AS resolved_this_week,
    
    -- This Month
    COUNT(CASE 
        WHEN c.created_at >= DATE_TRUNC('month', CURRENT_DATE) THEN 1 
    END) AS cases_this_month,
    COUNT(CASE 
        WHEN c.resolved_at >= DATE_TRUNC('month', CURRENT_DATE) THEN 1 
    END) AS resolved_this_month,
    
    -- Average Metrics
    AVG(CASE 
        WHEN c.first_response_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.first_response_at - c.created_at)) / 3600
        ELSE NULL
    END) AS avg_first_response_hours,
    
    AVG(CASE 
        WHEN c.resolved_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (c.resolved_at - c.created_at)) / 3600
        ELSE NULL
    END) AS avg_resolution_hours,
    
    AVG(c.customer_rating) AS avg_customer_rating
    
FROM crm.cases c
GROUP BY c.organization_id;

-- =====================================================
-- View: Customer 360 Degree View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_customer_360 AS
SELECT 
    a.account_id,
    a.organization_id,
    a.account_name,
    a.account_type,
    a.industry,
    a.phone,
    a.email,
    
    -- Contact Counts
    (SELECT COUNT(*) FROM crm.contacts WHERE account_id = a.account_id) AS total_contacts,
    (SELECT COUNT(*) FROM crm.contacts WHERE account_id = a.account_id AND is_primary = true) AS primary_contacts,
    
    -- Lead Counts
    (SELECT COUNT(*) FROM crm.leads WHERE company = a.account_name AND organization_id = a.organization_id) AS total_leads,
    (SELECT COUNT(*) FROM crm.leads WHERE company = a.account_name AND organization_id = a.organization_id AND status = 'CONVERTED') AS converted_leads,
    
    -- Opportunity Metrics
    (SELECT COUNT(*) FROM crm.opportunities WHERE account_id = a.account_id) AS total_opportunities,
    (SELECT COUNT(*) FROM crm.opportunities WHERE account_id = a.account_id AND status = 'OPEN') AS open_opportunities,
    (SELECT COUNT(*) FROM crm.opportunities WHERE account_id = a.account_id AND status = 'WON') AS won_opportunities,
    (SELECT SUM(amount) FROM crm.opportunities WHERE account_id = a.account_id AND status = 'WON') AS total_revenue,
    (SELECT SUM(expected_revenue) FROM crm.opportunities WHERE account_id = a.account_id AND status = 'OPEN') AS pipeline_value,
    
    -- Case Metrics
    (SELECT COUNT(*) FROM crm.cases WHERE account_id = a.account_id) AS total_cases,
    (SELECT COUNT(*) FROM crm.cases WHERE account_id = a.account_id AND status NOT IN ('RESOLVED', 'CLOSED')) AS open_cases,
    (SELECT AVG(customer_rating) FROM crm.cases WHERE account_id = a.account_id AND customer_rating IS NOT NULL) AS avg_satisfaction,
    
    -- Activity Summary
    (SELECT MAX(created_at) FROM crm.leads WHERE company = a.account_name AND organization_id = a.organization_id) AS last_lead_date,
    (SELECT MAX(created_at) FROM crm.opportunities WHERE account_id = a.account_id) AS last_opportunity_date,
    (SELECT MAX(created_at) FROM crm.cases WHERE account_id = a.account_id) AS last_case_date,
    
    -- Account Status
    CASE 
        WHEN (SELECT COUNT(*) FROM crm.opportunities WHERE account_id = a.account_id AND status = 'OPEN') > 0 THEN 'ACTIVE_OPPORTUNITY'
        WHEN (SELECT COUNT(*) FROM crm.cases WHERE account_id = a.account_id AND status NOT IN ('RESOLVED', 'CLOSED')) > 0 THEN 'ACTIVE_SUPPORT'
        WHEN (SELECT MAX(created_at) FROM crm.opportunities WHERE account_id = a.account_id) < CURRENT_TIMESTAMP - INTERVAL '90 days' THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END AS account_status,
    
    a.created_at,
    a.updated_at
    
FROM crm.accounts a;

-- =====================================================
-- View: Overall CRM Dashboard
-- =====================================================
CREATE OR REPLACE VIEW crm.v_crm_overall_dashboard AS
SELECT 
    org.id AS organization_id,
    
    -- Leads
    (SELECT COUNT(*) FROM crm.leads WHERE organization_id = org.id) AS total_leads,
    (SELECT COUNT(*) FROM crm.leads WHERE organization_id = org.id AND status = 'NEW') AS new_leads,
    (SELECT COUNT(*) FROM crm.leads WHERE organization_id = org.id AND status = 'CONVERTED') AS converted_leads,
    
    -- Accounts & Contacts
    (SELECT COUNT(*) FROM crm.accounts WHERE organization_id = org.id) AS total_accounts,
    (SELECT COUNT(*) FROM crm.contacts WHERE organization_id = org.id) AS total_contacts,
    
    -- Opportunities
    (SELECT COUNT(*) FROM crm.opportunities WHERE organization_id = org.id AND status = 'OPEN') AS open_opportunities,
    (SELECT SUM(amount) FROM crm.opportunities WHERE organization_id = org.id AND status = 'OPEN') AS total_pipeline_value,
    (SELECT SUM(expected_revenue) FROM crm.opportunities WHERE organization_id = org.id AND status = 'OPEN') AS weighted_pipeline_value,
    
    -- Cases
    (SELECT COUNT(*) FROM crm.cases WHERE organization_id = org.id AND status NOT IN ('RESOLVED', 'CLOSED')) AS open_cases,
    (SELECT COUNT(*) FROM crm.cases WHERE organization_id = org.id AND sla_breached = true) AS sla_breached_cases,
    
    -- Campaigns
    (SELECT COUNT(*) FROM crm.campaigns WHERE organization_id = org.id AND status = 'ACTIVE') AS active_campaigns,
    
    -- Tasks & Events
    (SELECT COUNT(*) FROM crm.tasks WHERE organization_id = org.id AND status != 'COMPLETED') AS open_tasks,
    (SELECT COUNT(*) FROM crm.events WHERE organization_id = org.id AND DATE(start_datetime) = CURRENT_DATE) AS events_today,
    
    -- Knowledge Base
    (SELECT COUNT(*) FROM crm.knowledge_base WHERE organization_id = org.id AND status = 'PUBLISHED') AS published_articles
    
FROM admin.organizations org;

-- =====================================================
-- View: SLA Compliance Report
-- =====================================================
CREATE OR REPLACE VIEW crm.v_sla_compliance_report AS
SELECT 
    c.organization_id,
    c.sla_id,
    sla.policy_name,
    sla.priority,
    
    -- Total Cases
    COUNT(*) AS total_cases,
    
    -- Response SLA
    COUNT(CASE WHEN c.first_response_at <= c.first_response_due THEN 1 END) AS first_response_met,
    COUNT(CASE WHEN c.first_response_at > c.first_response_due OR (c.first_response_at IS NULL AND c.first_response_due < CURRENT_TIMESTAMP) THEN 1 END) AS first_response_breached,
    
    -- Resolution SLA
    COUNT(CASE WHEN c.resolved_at <= c.resolution_due THEN 1 END) AS resolution_met,
    COUNT(CASE WHEN c.resolved_at > c.resolution_due OR (c.status NOT IN ('RESOLVED', 'CLOSED') AND c.resolution_due < CURRENT_TIMESTAMP) THEN 1 END) AS resolution_breached,
    
    -- Compliance Rates
    ROUND(100.0 * COUNT(CASE WHEN c.first_response_at <= c.first_response_due THEN 1 END) / NULLIF(COUNT(*), 0), 2) AS first_response_compliance_rate,
    ROUND(100.0 * COUNT(CASE WHEN c.resolved_at <= c.resolution_due THEN 1 END) / NULLIF(COUNT(CASE WHEN c.status IN ('RESOLVED', 'CLOSED') THEN 1 END), 0), 2) AS resolution_compliance_rate,
    
    -- Average Times
    AVG(EXTRACT(EPOCH FROM (c.first_response_at - c.created_at)) / 60) AS avg_first_response_minutes,
    AVG(EXTRACT(EPOCH FROM (c.resolved_at - c.created_at)) / 60) AS avg_resolution_minutes,
    
    -- SLA Times
    sla.first_response_time AS sla_first_response_minutes,
    sla.resolution_time AS sla_resolution_minutes
    
FROM crm.cases c
JOIN crm.sla_policies sla ON c.sla_id = sla.sla_id
GROUP BY c.organization_id, c.sla_id, sla.policy_name, sla.priority, sla.first_response_time, sla.resolution_time;


