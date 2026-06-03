--liquibase formatted sql

--changeset easyops:014-hr-loans-rbac-permissions context:data
--comment: Granular HR loan permissions (NF-03): hr_loans resource

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans View', 'HR_LOANS_VIEW', 'hr_loans', 'view', 'View loan applications, accounts, and reports'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans Apply', 'HR_LOANS_APPLY', 'hr_loans', 'apply', 'Create and submit loan applications on behalf of employees'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_APPLY');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans HR Approve', 'HR_LOANS_HR_APPROVE', 'hr_loans', 'hr_approve', 'First-step approval (HR) on loan applications'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_HR_APPROVE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans Finance Approve', 'HR_LOANS_FINANCE_APPROVE', 'hr_loans', 'finance_approve', 'Second-step approval (Finance) on loan applications'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_FINANCE_APPROVE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans Payroll Recoveries', 'HR_LOANS_PAYROLL_RECOVERIES', 'hr_loans', 'payroll_recoveries', 'Preview payroll loan recoveries'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_PAYROLL_RECOVERIES');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Loans Manage', 'HR_LOANS_MANAGE', 'hr_loans', 'manage', 'Full loan operations: disbursement, settlement, payroll confirm'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_LOANS_MANAGE');

--changeset easyops:015-grant-hr-loans-perms-to-admins context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HR_LOANS_VIEW', 'HR_LOANS_APPLY', 'HR_LOANS_HR_APPROVE', 'HR_LOANS_FINANCE_APPROVE',
    'HR_LOANS_PAYROLL_RECOVERIES', 'HR_LOANS_MANAGE'
)
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rbac.role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HR_LOANS_VIEW', 'HR_LOANS_APPLY', 'HR_LOANS_HR_APPROVE', 'HR_LOANS_FINANCE_APPROVE',
    'HR_LOANS_PAYROLL_RECOVERIES', 'HR_LOANS_MANAGE'
)
WHERE r.code = 'ORG_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM rbac.role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
