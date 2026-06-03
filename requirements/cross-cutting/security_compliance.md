# Security & Compliance

## Roles & Access Control (RBAC)
- Role hierarchy and custom roles
- Permissions on entities, fields, and actions (view, create, update, delete, approve)
- Branch/warehouse/company scope restriction
- Segregation of duties (SoD) rules and conflict detection

## Authentication
- Email/password, SSO (SAML/OIDC), 2FA (TOTP/SMS/email)
- Session management, device management, IP allow/deny lists

## Audit & Traceability
- Immutable audit logs for CRUD, login, approvals, configuration changes
- Who/when/what before and after values, correlation ID per request
- Exportable audit reports; retention policy controls

## Data Protection
- At-rest encryption for sensitive fields; TLS in transit
- Field-level masking and access justification workflows
- Backup, restore, and key rotation requirements

## Compliance
- GDPR/PDPA data subject rights (export, delete, rectify)
- E-invoicing, tax regulations (country-specific)
- Logging and time synchronization standards
