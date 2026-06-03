--liquibase formatted sql

--changeset easyops:086-insert-default-system-roles context:data
--comment: Insert default system roles
INSERT INTO rbac.roles (name, code, description, is_system_role) VALUES
('System Administrator', 'SYSTEM_ADMIN', 'Full system access', true),
('Organization Administrator', 'ORG_ADMIN', 'Organization-level administration', true),
('User', 'USER', 'Standard user access', true),
('Guest', 'GUEST', 'Limited guest access', true);
