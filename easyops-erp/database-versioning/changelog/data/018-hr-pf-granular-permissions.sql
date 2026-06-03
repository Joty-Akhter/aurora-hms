--liquibase formatted sql

--changeset easyops:106-add-hr-pf-granular-permissions context:data
--comment: Add granular PF permissions for policy, compliance, approvals, filing, remittance and corrections.
INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Policy Manage', 'PF_POLICY_MANAGE', 'pf_policy', 'manage', 'Manage EPF policy and eligibility configuration'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_POLICY_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Compliance Manage', 'PF_COMPLIANCE_MANAGE', 'pf_compliance', 'manage', 'Manage PF compliance records and filing status'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_COMPLIANCE_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Approval Manage', 'PF_APPROVAL_MANAGE', 'pf_approval', 'manage', 'Review, approve or reject PF withdrawal workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_APPROVAL_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Filing Manage', 'PF_FILING_MANAGE', 'pf_filing', 'manage', 'Generate and verify PF ECR/challan filing artifacts'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_FILING_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Remittance Manage', 'PF_REMITTANCE_MANAGE', 'pf_remittance', 'manage', 'Post PF liabilities and maintain remittance payment lifecycle'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_REMITTANCE_MANAGE');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'PF Correction Manage', 'PF_CORRECTION_MANAGE', 'pf_correction', 'manage', 'Perform PF reversals/adjustments and correction workflows'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'PF_CORRECTION_MANAGE');

--changeset easyops:107-assign-hr-pf-granular-permissions context:data
--comment: Grant granular PF permissions to SYSTEM_ADMIN and ORG_ADMIN.
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'PF_POLICY_MANAGE',
    'PF_COMPLIANCE_MANAGE',
    'PF_APPROVAL_MANAGE',
    'PF_FILING_MANAGE',
    'PF_REMITTANCE_MANAGE',
    'PF_CORRECTION_MANAGE'
)
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM rbac.role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
