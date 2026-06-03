-- Add payment reminder tables
-- This script can be run multiple times safely

-- Payment Reminder Configuration
CREATE TABLE IF NOT EXISTS accounting.reminder_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL UNIQUE REFERENCES admin.organizations(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT false,
    days_before_due INTEGER, -- Reminder before due date (e.g., -7 = 7 days before)
    days_after_due_level1 INTEGER DEFAULT 1, -- First overdue reminder
    days_after_due_level2 INTEGER DEFAULT 7, -- Second overdue reminder
    days_after_due_level3 INTEGER DEFAULT 14, -- Third overdue reminder
    email_template_before_due VARCHAR(2000),
    email_template_level1 VARCHAR(2000),
    email_template_level2 VARCHAR(2000),
    email_template_level3 VARCHAR(2000),
    send_copy_to_email VARCHAR(255), -- CC email for all reminders
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Payment Reminder History
CREATE TABLE IF NOT EXISTS accounting.reminder_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES accounting.ar_invoices(id) ON DELETE CASCADE,
    sent_date DATE NOT NULL,
    reminder_level INTEGER NOT NULL, -- 0=before due, 1,2,3=overdue levels
    recipient_email VARCHAR(255) NOT NULL,
    email_sent BOOLEAN DEFAULT false,
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_reminder_config_org ON accounting.reminder_config(organization_id);
CREATE INDEX IF NOT EXISTS idx_reminder_history_org ON accounting.reminder_history(organization_id);
CREATE INDEX IF NOT EXISTS idx_reminder_history_invoice ON accounting.reminder_history(invoice_id);
CREATE INDEX IF NOT EXISTS idx_reminder_history_sent_date ON accounting.reminder_history(sent_date);

-- Comments
COMMENT ON TABLE accounting.reminder_config IS 'Payment reminder configuration per organization';
COMMENT ON TABLE accounting.reminder_history IS 'History of sent payment reminders';

