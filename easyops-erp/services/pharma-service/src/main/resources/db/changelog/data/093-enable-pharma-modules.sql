--liquibase formatted sql

--changeset easyops:093-enable-pharma-modules
--comment: When pharma-service starts, enable pharma and its dependencies in frontend module config
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('modules', 'modules.pharma', 'true', 'boolean', 'Pharma module - enabled when pharma-service runs'),
('modules', 'modules.inventory', 'true', 'boolean', 'Inventory module - pharma dependency'),
('modules', 'modules.accounting', 'true', 'boolean', 'Accounting module - pharma dependency'),
('modules', 'modules.hr', 'true', 'boolean', 'HR module - pharma dependency')
ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = CURRENT_TIMESTAMP;
