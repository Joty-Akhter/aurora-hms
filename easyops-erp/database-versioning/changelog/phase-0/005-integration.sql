--liquibase formatted sql

--changeset easyops:034-create-integration-schema context:integration
--comment: Create integration schema
CREATE SCHEMA IF NOT EXISTS integration;

-- Grant permissions to development user
GRANT ALL PRIVILEGES ON SCHEMA integration TO easyops;

--changeset easyops:035-create-webhooks-table context:integration
--comment: Create webhooks table
CREATE TABLE integration.webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255),
    events TEXT[],
    is_active BOOLEAN DEFAULT true,
    retry_count INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhooks_org_id ON integration.webhooks(organization_id);
CREATE INDEX idx_webhooks_is_active ON integration.webhooks(is_active);

--changeset easyops:036-create-webhook-deliveries-table context:integration
--comment: Create webhook deliveries table
CREATE TABLE integration.webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID NOT NULL REFERENCES integration.webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    delivered_at TIMESTAMP WITH TIME ZONE,
    retry_attempt INTEGER DEFAULT 0,
    success BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_deliveries_webhook_id ON integration.webhook_deliveries(webhook_id);
CREATE INDEX idx_webhook_deliveries_created_at ON integration.webhook_deliveries(created_at);
CREATE INDEX idx_webhook_deliveries_success ON integration.webhook_deliveries(success);

--changeset easyops:037-create-system-metrics-table context:integration
--comment: Create system metrics table
CREATE TABLE system.metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION,
    metric_type VARCHAR(50),
    tags JSONB,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_service_name ON system.metrics(service_name);
CREATE INDEX idx_metrics_metric_name ON system.metrics(metric_name);
CREATE INDEX idx_metrics_recorded_at ON system.metrics(recorded_at);

--changeset easyops:038-create-alerts-table context:integration
--comment: Create alerts table
CREATE TABLE system.alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_name VARCHAR(255) NOT NULL,
    alert_type VARCHAR(50),
    service_name VARCHAR(100),
    message TEXT NOT NULL,
    details JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    acknowledged_by UUID REFERENCES users.users(id),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_status ON system.alerts(status);
CREATE INDEX idx_alerts_service_name ON system.alerts(service_name);
CREATE INDEX idx_alerts_alert_type ON system.alerts(alert_type);
CREATE INDEX idx_alerts_created_at ON system.alerts(created_at);

--changeset easyops:039-create-service-health-table context:integration
--comment: Create service health tracking table
CREATE TABLE system.service_health (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_check_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    error_message TEXT,
    metadata JSONB
);

CREATE INDEX idx_service_health_service_name ON system.service_health(service_name);
CREATE INDEX idx_service_health_status ON system.service_health(status);
CREATE INDEX idx_service_health_last_check ON system.service_health(last_check_at);

--changeset easyops:040-create-settings-table context:integration
--comment: Create system settings table
CREATE TABLE system.settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(100),
    key VARCHAR(100) UNIQUE NOT NULL,
    value TEXT,
    data_type VARCHAR(50),
    is_encrypted BOOLEAN DEFAULT false,
    description TEXT,
    is_readonly BOOLEAN DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users.users(id)
);

CREATE INDEX idx_settings_key ON system.settings(key);
CREATE INDEX idx_settings_category ON system.settings(category);

--changeset easyops:041-create-integration-triggers context:integration
--comment: Create updated_at triggers for integration tables
CREATE TRIGGER update_webhooks_updated_at BEFORE UPDATE ON integration.webhooks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_settings_updated_at BEFORE UPDATE ON system.settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:042-grant-integration-permissions context:integration
--comment: Grant permissions on integration tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA integration TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA integration TO easyops;
