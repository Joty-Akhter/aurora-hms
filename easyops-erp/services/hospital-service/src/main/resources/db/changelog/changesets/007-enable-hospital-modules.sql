--liquibase formatted sql

--changeset easyops:007-enable-hospital-modules
--comment: When hospital-service starts, enable hospital module in frontend module config
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('modules', 'modules.hospital', 'true', 'boolean', 'Hospital module - enabled when hospital-service runs')
ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = CURRENT_TIMESTAMP;
