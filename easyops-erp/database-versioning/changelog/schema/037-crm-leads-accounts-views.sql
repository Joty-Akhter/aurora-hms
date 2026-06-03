--liquibase formatted sql

--changeset easyops:037-create-crm-views splitStatements:false

-- =====================================================
-- Lead Summary View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_lead_summary AS
SELECT 
    l.lead_id,
    l.organization_id,
    l.lead_number,
    l.first_name,
    l.last_name,
    l.email,
    l.phone,
    l.company,
    l.job_title,
    l.status,
    l.rating,
    l.lead_score,
    ls.source_name,
    l.owner_id,
    l.created_at,
    l.qualified_at,
    l.converted_at,
    COUNT(DISTINCT la.activity_id) as activity_count,
    MAX(la.activity_date) as last_activity_date
FROM crm.leads l
LEFT JOIN crm.lead_sources ls ON l.lead_source_id = ls.source_id
LEFT JOIN crm.lead_activities la ON l.lead_id = la.lead_id
GROUP BY l.lead_id, l.organization_id, l.lead_number, l.first_name, l.last_name, 
         l.email, l.phone, l.company, l.job_title, l.status, l.rating, l.lead_score,
         ls.source_name, l.owner_id, l.created_at, l.qualified_at, l.converted_at;

-- =====================================================
-- Lead Conversion Metrics View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_lead_conversion_metrics AS
SELECT 
    organization_id,
    COUNT(*) FILTER (WHERE status = 'NEW') as new_leads,
    COUNT(*) FILTER (WHERE status = 'CONTACTED') as contacted_leads,
    COUNT(*) FILTER (WHERE status = 'QUALIFIED') as qualified_leads,
    COUNT(*) FILTER (WHERE status = 'CONVERTED') as converted_leads,
    COUNT(*) FILTER (WHERE status = 'LOST') as lost_leads,
    COUNT(*) FILTER (WHERE status = 'UNQUALIFIED') as unqualified_leads,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE status = 'CONVERTED') / 
        NULLIF(COUNT(*), 0), 2
    ) as conversion_rate,
    AVG(lead_score) as avg_lead_score
FROM crm.leads
GROUP BY organization_id;

-- =====================================================
-- Lead Source Performance View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_lead_source_performance AS
SELECT 
    ls.organization_id,
    ls.source_id,
    ls.source_name,
    ls.source_type,
    COUNT(l.lead_id) as total_leads,
    COUNT(l.lead_id) FILTER (WHERE l.status = 'CONVERTED') as converted_leads,
    ROUND(
        100.0 * COUNT(l.lead_id) FILTER (WHERE l.status = 'CONVERTED') / 
        NULLIF(COUNT(l.lead_id), 0), 2
    ) as conversion_rate,
    AVG(l.lead_score) as avg_lead_score,
    COUNT(l.lead_id) FILTER (WHERE l.rating = 'HOT') as hot_leads,
    COUNT(l.lead_id) FILTER (WHERE l.rating = 'WARM') as warm_leads,
    COUNT(l.lead_id) FILTER (WHERE l.rating = 'COLD') as cold_leads
FROM crm.lead_sources ls
LEFT JOIN crm.leads l ON ls.source_id = l.lead_source_id
GROUP BY ls.organization_id, ls.source_id, ls.source_name, ls.source_type;

-- =====================================================
-- Account Summary View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_account_summary AS
SELECT 
    a.account_id,
    a.organization_id,
    a.account_number,
    a.account_name,
    a.account_type,
    a.industry,
    a.owner_id,
    a.is_active,
    a.customer_since,
    COUNT(DISTINCT c.contact_id) as contact_count,
    COUNT(DISTINCT c.contact_id) FILTER (WHERE c.is_primary = true) as primary_contacts
FROM crm.accounts a
LEFT JOIN crm.contacts c ON a.account_id = c.account_id
GROUP BY a.account_id, a.organization_id, a.account_number, a.account_name,
         a.account_type, a.industry, a.owner_id, a.is_active, a.customer_since;

-- =====================================================
-- Contact Summary View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_contact_summary AS
SELECT 
    c.contact_id,
    c.organization_id,
    c.account_id,
    c.first_name,
    c.last_name,
    c.email,
    c.phone,
    c.mobile,
    c.job_title,
    c.is_primary,
    c.is_active,
    a.account_name,
    a.account_type
FROM crm.contacts c
LEFT JOIN crm.accounts a ON c.account_id = a.account_id;

-- =====================================================
-- CRM Dashboard Stats View
-- =====================================================
CREATE OR REPLACE VIEW crm.v_crm_dashboard_stats AS
SELECT 
    organization_id,
    COUNT(DISTINCT lead_id) as total_leads,
    COUNT(DISTINCT lead_id) FILTER (WHERE status = 'NEW') as new_leads,
    COUNT(DISTINCT lead_id) FILTER (WHERE status = 'QUALIFIED') as qualified_leads,
    COUNT(DISTINCT lead_id) FILTER (WHERE status = 'CONVERTED') as converted_leads,
    COUNT(DISTINCT lead_id) FILTER (WHERE created_at >= CURRENT_DATE - INTERVAL '30 days') as leads_last_30d,
    COUNT(DISTINCT lead_id) FILTER (WHERE converted_at >= CURRENT_DATE - INTERVAL '30 days') as conversions_last_30d
FROM crm.leads
GROUP BY organization_id;

--rollback DROP VIEW IF EXISTS crm.v_crm_dashboard_stats;
--rollback DROP VIEW IF EXISTS crm.v_contact_summary;
--rollback DROP VIEW IF EXISTS crm.v_account_summary;
--rollback DROP VIEW IF EXISTS crm.v_lead_source_performance;
--rollback DROP VIEW IF EXISTS crm.v_lead_conversion_metrics;
--rollback DROP VIEW IF EXISTS crm.v_lead_summary;


