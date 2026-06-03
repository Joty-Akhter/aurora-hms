--liquibase formatted sql

--changeset aurora-hms:019-hr-self-service-permissions context:data
--comment: HMS Phase A — granular HR self-service permissions (optional grants alongside linked Employee.userId)

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Self Payslip View', 'HR_SELF_PAYSLIP_VIEW', 'hr_self', 'payslip_view', 'View own payslips without HR_VIEW'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_SELF_PAYSLIP_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'HR Self Salary Summary View', 'HR_SELF_SALARY_SUMMARY_VIEW', 'hr_self', 'salary_summary_view', 'View own salary summary without HR_VIEW'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'HR_SELF_SALARY_SUMMARY_VIEW');

INSERT INTO rbac.permissions (name, code, resource, action, description)
SELECT 'Leave Self Submit', 'LEAVE_SELF_SUBMIT', 'leave_self', 'submit', 'Submit leave requests without HR_MANAGE'
WHERE NOT EXISTS (SELECT 1 FROM rbac.permissions WHERE code = 'LEAVE_SELF_SUBMIT');

--changeset aurora-hms:019b-assign-hr-self-perms-to-user context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HR_SELF_PAYSLIP_VIEW',
    'HR_SELF_SALARY_SUMMARY_VIEW',
    'LEAVE_SELF_SUBMIT'
)
WHERE r.code = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM rbac.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

--changeset aurora-hms:019c-assign-hr-self-perms-to-admins context:data
INSERT INTO rbac.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac.roles r
JOIN rbac.permissions p ON p.code IN (
    'HR_SELF_PAYSLIP_VIEW',
    'HR_SELF_SALARY_SUMMARY_VIEW',
    'LEAVE_SELF_SUBMIT'
)
WHERE r.code IN ('SYSTEM_ADMIN', 'ORG_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM rbac.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
