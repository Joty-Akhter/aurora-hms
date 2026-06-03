--liquibase formatted sql

--changeset easyops:091-insert-default-system-configuration context:data
--comment: Insert default system configuration
INSERT INTO system.configurations (key, value, description, category) VALUES
('system.name', 'EasyOps ERP', 'System name', 'general'),
('system.version', '1.0.0', 'System version', 'general'),
('system.timezone', 'UTC', 'Default timezone', 'general'),
('auth.password.min_length', '8', 'Minimum password length', 'security'),
('auth.password.require_uppercase', 'true', 'Require uppercase letters in password', 'security'),
('auth.password.require_lowercase', 'true', 'Require lowercase letters in password', 'security'),
('auth.password.require_numbers', 'true', 'Require numbers in password', 'security'),
('auth.password.require_symbols', 'true', 'Require symbols in password', 'security'),
('auth.session.timeout', '3600', 'Session timeout in seconds', 'security'),
('auth.max_login_attempts', '5', 'Maximum login attempts before lockout', 'security'),
('auth.lockout.duration', '900', 'Account lockout duration in seconds', 'security');

--changeset easyops:092-create-default-organization context:data
--comment: Create a default organization
INSERT INTO admin.organizations (name, code, description) VALUES
('EasyOps Demo Organization', 'DEMO_ORG', 'Default demo organization for testing');

--changeset easyops:093-create-default-system-admin-user context:data
--comment: Create a default system administrator user
-- Password: Admin123! (BCrypt hashed)
INSERT INTO users.users (username, email, password_hash, first_name, last_name, is_active, is_verified) VALUES
('admin', 'admin@easyops.com', '$2a$10$0CXzkDSCu/KwC1h6N.iuD.ezBcN0e/IHFytZu3cg2eZIiPV7QvQii', 'System', 'Administrator', true, true) 
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash;

--changeset easyops:094-assign-system-admin-role-to-admin-user context:data
--comment: Assign system administrator role to admin user
INSERT INTO rbac.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users.users u, rbac.roles r
WHERE u.username = 'admin' AND r.code = 'SYSTEM_ADMIN';

--changeset easyops:095-assign-admin-user-to-default-organization context:data
--comment: Assign admin user to default organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary)
SELECT u.id, o.id, 'OWNER', true
FROM users.users u, admin.organizations o
WHERE u.username = 'admin' AND o.code = 'DEMO_ORG';

--changeset easyops:096-insert-default-email-templates context:data
--comment: Insert default email templates
INSERT INTO notifications.email_templates (name, subject, body_html, body_text, variables) VALUES
('user_invitation', 
 'You''re invited to join {{organization.name}}', 
 '<html><body><h2>You''re Invited!</h2><p>{{invited_by.name}} has invited you to join {{organization.name}} as a {{invitation.role}}.</p><p><a href="{{invitation.link}}" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Accept Invitation</a></p><p>This invitation expires on {{invitation.expiresAt}}.</p></body></html>',
 'You''re invited to join {{organization.name}} by {{invited_by.name}}. Click here to accept: {{invitation.link}}. This invitation expires on {{invitation.expiresAt}}.',
 ARRAY['organization.name', 'invited_by.name', 'invitation.link', 'invitation.role', 'invitation.expiresAt']),

('password_reset', 
 'Reset your password for {{system.name}}', 
 '<html><body><h2>Password Reset Request</h2><p>Hello {{user.name}},</p><p>We received a request to reset your password. Click the button below to reset it:</p><p><a href="{{reset.link}}" style="background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Reset Password</a></p><p>This link expires in 1 hour ({{reset.expiresAt}}).</p><p>If you didn''t request this, please ignore this email or contact support.</p></body></html>',
 'Hello {{user.name}}, click here to reset your password: {{reset.link}}. This link expires at {{reset.expiresAt}}. If you didn''t request this, please ignore this email.',
 ARRAY['user.name', 'reset.link', 'reset.expiresAt', 'system.name']),

('welcome_email', 
 'Welcome to {{organization.name}}!', 
 '<html><body><h2>Welcome to {{organization.name}}!</h2><p>Hello {{user.name}},</p><p>We''re excited to have you on board! Here are some quick links to get you started:</p><ul><li><a href="{{getting_started.link}}">Getting Started Guide</a></li><li>Support Email: {{support.email}}</li></ul><p>If you have any questions, don''t hesitate to reach out!</p></body></html>',
 'Welcome to {{organization.name}}, {{user.name}}! Get started here: {{getting_started.link}}. Need help? Contact us at {{support.email}}.',
 ARRAY['user.name', 'organization.name', 'getting_started.link', 'support.email']),

('organization_suspended', 
 'Your organization has been suspended', 
 '<html><body><h2>Organization Suspended</h2><p>Your organization {{organization.name}} has been suspended.</p><p><strong>Reason:</strong> {{suspension.reason}}</p><p><strong>Date:</strong> {{suspension.date}}</p><p>To resolve this issue, please contact us at {{contact.email}}.</p></body></html>',
 'Your organization {{organization.name}} has been suspended. Reason: {{suspension.reason}}. Contact: {{contact.email}}.',
 ARRAY['organization.name', 'suspension.reason', 'suspension.date', 'contact.email']);

--changeset easyops:097-insert-default-notification-preferences context:data
--comment: Insert default notification preferences for existing users
INSERT INTO notifications.notification_preferences (user_id, email_enabled, in_app_enabled)
SELECT id, true, true FROM users.users
ON CONFLICT (user_id) DO NOTHING;

--changeset easyops:098-insert-default-system-settings context:data
--comment: Insert default system settings for Phase 0.3
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('email', 'smtp.host', 'smtp.gmail.com', 'string', 'SMTP server host'),
('email', 'smtp.port', '587', 'integer', 'SMTP server port'),
('email', 'smtp.username', '', 'string', 'SMTP username'),
('email', 'smtp.password', '', 'string', 'SMTP password (encrypted)'),
('email', 'smtp.from', 'noreply@easyops.com', 'string', 'Default from email address'),
('email', 'smtp.use_tls', 'true', 'boolean', 'Use TLS for SMTP'),
('monitoring', 'health_check.interval', '30', 'integer', 'Health check interval in seconds'),
('monitoring', 'metrics.retention_days', '30', 'integer', 'Metrics retention period in days'),
('monitoring', 'alerts.enabled', 'true', 'boolean', 'Enable system alerts'),
('notifications', 'max_notifications_per_user', '1000', 'integer', 'Maximum notifications per user'),
('notifications', 'retention_days', '90', 'integer', 'Notification retention period in days'),
('webhooks', 'max_webhooks_per_org', '10', 'integer', 'Maximum webhooks per organization'),
('webhooks', 'retry_max_attempts', '3', 'integer', 'Maximum webhook retry attempts');

--changeset easyops:099-insert-accounting-settings context:data
--comment: Insert sample payment methods configuration
INSERT INTO system.settings (category, key, value, data_type, description) VALUES
('accounting', 'payment_methods', '["CASH","CHECK","WIRE","CARD","ACH"]', 'json', 'Available payment methods'),
('accounting', 'default_payment_terms', '30', 'integer', 'Default payment terms in days'),
('accounting', 'enable_credit_limit', 'true', 'boolean', 'Enable customer credit limit checking')
ON CONFLICT (key) DO NOTHING;

--changeset easyops:100-insert-standard-coa-templates context:data
--comment: Insert standard CoA template
INSERT INTO accounting.coa_templates (name, description, industry) VALUES
('Standard Business', 'Standard chart of accounts for general business', 'Standard'),
('Retail Business', 'Chart of accounts optimized for retail operations', 'Retail'),
('Manufacturing', 'Chart of accounts for manufacturing companies', 'Manufacturing'),
('Service Company', 'Chart of accounts for service-based businesses', 'Services');
