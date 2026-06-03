--liquibase formatted sql

--changeset easyops:027-create-notifications-schema context:notifications
--comment: Create notifications schema
CREATE SCHEMA IF NOT EXISTS notifications;

-- Grant permissions to development user
GRANT ALL PRIVILEGES ON SCHEMA notifications TO easyops;

--changeset easyops:028-create-notifications-table context:notifications
--comment: Create in-app notifications table
CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES admin.organizations(id) ON DELETE CASCADE,
    type VARCHAR(50) DEFAULT 'INFO',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_notifications_user_id ON notifications.notifications(user_id);
CREATE INDEX idx_notifications_org_id ON notifications.notifications(organization_id);
CREATE INDEX idx_notifications_is_read ON notifications.notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications.notifications(created_at);
CREATE INDEX idx_notifications_priority ON notifications.notifications(priority);

--changeset easyops:029-create-email-logs-table context:notifications
--comment: Create email logs table
CREATE TABLE notifications.email_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    to_email VARCHAR(255) NOT NULL,
    from_email VARCHAR(255),
    subject VARCHAR(255),
    template_name VARCHAR(100),
    template_variables JSONB,
    status VARCHAR(50) DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    failed_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    organization_id UUID REFERENCES admin.organizations(id),
    user_id UUID REFERENCES users.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_logs_to_email ON notifications.email_logs(to_email);
CREATE INDEX idx_email_logs_status ON notifications.email_logs(status);
CREATE INDEX idx_email_logs_user_id ON notifications.email_logs(user_id);
CREATE INDEX idx_email_logs_org_id ON notifications.email_logs(organization_id);
CREATE INDEX idx_email_logs_created_at ON notifications.email_logs(created_at);

--changeset easyops:030-create-email-templates-table context:notifications
--comment: Create email templates table
CREATE TABLE notifications.email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_html TEXT NOT NULL,
    body_text TEXT,
    variables TEXT[],
    locale VARCHAR(10) DEFAULT 'en-US',
    organization_id UUID REFERENCES admin.organizations(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_templates_name ON notifications.email_templates(name);
CREATE INDEX idx_email_templates_org_id ON notifications.email_templates(organization_id);
CREATE INDEX idx_email_templates_is_active ON notifications.email_templates(is_active);

--changeset easyops:031-create-notification-preferences-table context:notifications
--comment: Create notification preferences table
CREATE TABLE notifications.notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN DEFAULT true,
    in_app_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT false,
    notification_types JSONB,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

--changeset easyops:032-create-notifications-triggers context:notifications
--comment: Create updated_at triggers for notifications tables
CREATE TRIGGER update_email_templates_updated_at BEFORE UPDATE ON notifications.email_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notification_preferences_updated_at BEFORE UPDATE ON notifications.notification_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:033-grant-notifications-permissions context:notifications
--comment: Grant permissions on notifications tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notifications TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notifications TO easyops;
