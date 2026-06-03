export type AccessAction = 'view' | 'manage' | 'admin';

export interface AccessRequirement {
  resource: string;
  action: AccessAction;
}

interface AccessRule extends AccessRequirement {
  pattern: RegExp;
}

const accessRules: AccessRule[] = [
  { pattern: /^\/$/, resource: 'dashboard', action: 'view' },
  { pattern: /^\/dashboard/, resource: 'dashboard', action: 'view' },
  { pattern: /^\/organizations/, resource: 'organizations', action: 'view' },
  { pattern: /^\/users/, resource: 'users', action: 'view' },
  { pattern: /^\/roles/, resource: 'roles', action: 'view' },
  { pattern: /^\/permissions/, resource: 'permissions', action: 'view' },
  { pattern: /^\/accounting/, resource: 'accounting', action: 'view' },
  { pattern: /^\/sales/, resource: 'sales', action: 'view' },
  { pattern: /^\/inventory/, resource: 'inventory', action: 'view' },
  { pattern: /^\/purchase/, resource: 'purchase', action: 'view' },
  { pattern: /^\/hr/, resource: 'hr', action: 'view' },
  { pattern: /^\/crm/, resource: 'crm', action: 'view' },
  { pattern: /^\/manufacturing/, resource: 'manufacturing', action: 'view' },
  { pattern: /^\/hospital/, resource: 'hospital', action: 'view' },
  { pattern: /^\/system/, resource: 'system', action: 'view' },
  { pattern: /^\/settings/, resource: 'system', action: 'view' },
  { pattern: /^\/profile/, resource: 'system', action: 'view' },
];

export const getAccessRequirementForPath = (path: string): AccessRequirement | null => {
  const normalizedPath = path || '/';
  const rule = accessRules.find((accessRule) => accessRule.pattern.test(normalizedPath));
  return rule ? { resource: rule.resource, action: rule.action } : null;
};

