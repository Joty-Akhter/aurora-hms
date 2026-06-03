--liquibase formatted sql

--changeset easyops:109-insert-base-modules-config context:data
--comment: Base/general enabled modules - core modules always shown. Module-specific services (pharma, hospital) override via their own Liquibase.
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('modules', 'modules.dashboard', 'true', 'boolean', 'Dashboard module - always enabled'),
('modules', 'modules.organizations', 'true', 'boolean', 'Organizations module - always enabled'),
('modules', 'modules.users', 'true', 'boolean', 'Users module - always enabled'),
('modules', 'modules.roles', 'true', 'boolean', 'Roles module - always enabled'),
('modules', 'modules.permissions', 'true', 'boolean', 'Permissions module - always enabled')
ON CONFLICT (key) DO NOTHING;
