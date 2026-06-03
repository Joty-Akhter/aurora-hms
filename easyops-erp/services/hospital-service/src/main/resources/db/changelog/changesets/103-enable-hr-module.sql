--liquibase formatted sql

--changeset hospital-service:103-enable-hr-module splitStatements:false
--comment: Enable HR module in frontend module config. hr-service has no own Liquibase runner so this
--         is seeded here. pharma-service does this as a side-effect of its own migration, but Aurora
--         HMS runs without pharma-service.
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('modules', 'modules.hr',         'true', 'boolean', 'HR module - enabled when hr-service is deployed'),
('modules', 'modules.accounting', 'true', 'boolean', 'Accounting module'),
('modules', 'modules.inventory',  'true', 'boolean', 'Inventory module')
ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = CURRENT_TIMESTAMP;
