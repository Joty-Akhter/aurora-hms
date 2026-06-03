--liquibase formatted sql

--changeset easyops:054-create-hr-reporting-schema splitStatements:false

-- =====================================================
-- REPORTING SCHEMA
-- Phase 7: Reporting, Analytics & Dashboards
-- =====================================================

-- Scheduled Reports Table
CREATE TABLE hr.scheduled_reports (
    scheduled_report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    report_name VARCHAR(200) NOT NULL,
    report_type VARCHAR(100) NOT NULL,
    report_config JSONB, -- JSON structure for report configuration
    schedule_frequency VARCHAR(50) NOT NULL, -- daily, weekly, monthly, quarterly, yearly
    schedule_day INTEGER, -- Day of week/month for scheduling
    schedule_time VARCHAR(10), -- Time in HH:mm format
    recipients JSONB, -- JSON array of email addresses
    format VARCHAR(50) DEFAULT 'json', -- json, pdf, excel, csv
    is_active BOOLEAN DEFAULT true,
    last_run_date TIMESTAMP,
    next_run_date DATE,
    run_count INTEGER DEFAULT 0,
    last_run_status VARCHAR(50), -- success, failed, skipped
    last_error TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scheduled_report_organization ON hr.scheduled_reports(organization_id);
CREATE INDEX idx_scheduled_report_active ON hr.scheduled_reports(is_active);
CREATE INDEX idx_scheduled_report_next_run ON hr.scheduled_reports(next_run_date);

