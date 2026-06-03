# User-Organization Assignment Guide

## Problem: User with ORG_ADMIN Role but No Organization

When you assign the ORG_ADMIN role to a user without assigning them to an organization, the user **does not belong to any organization**.

---

## Understanding the Two Systems

### 1. RBAC Role Assignment (`rbac.user_roles`)
- Stores which roles a user has
- `organizationId` can be `null` (global roles) or a specific organization ID
- When you assign ORG_ADMIN without specifying organization, it's stored as a **global role** (`organizationId = null`)

### 2. User-Organization Membership (`admin.user_organizations`)
- Stores which organizations a user belongs to
- This is **separate** from role assignments
- Required for organization-scoped features to work

---

## What Happens When User Has No Organization

### Current Behavior:
1. ✅ User has ORG_ADMIN role (stored in `rbac.user_roles` with `organizationId = null`)
2. ❌ User is NOT in `admin.user_organizations` table
3. ❌ User has no `currentOrganizationId` set
4. ⚠️ When fetching roles with `organizationId = null`, user gets ORG_ADMIN role
5. ⚠️ But ORG_ADMIN permissions are meant to work **within an organization context**
6. ❌ Without organization assignment, ORG_ADMIN permissions won't work properly

---

## Solution: Assign User to an Organization

### Option 1: Direct SQL (Quick Fix)

```sql
-- Find the user ID
SELECT id, username, email FROM users.users WHERE username = 'your-username';

-- Find an organization ID (or use the default DEMO_ORG)
SELECT id, name, code FROM admin.organizations WHERE code = 'DEMO_ORG';

-- Assign user to organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary, status)
VALUES (
    'USER_UUID_HERE',           -- Replace with actual user UUID
    'ORG_UUID_HERE',            -- Replace with actual organization UUID
    'ADMIN',                    -- Organization role (ADMIN, MEMBER, etc.)
    true,                       -- Set as primary organization
    'ACTIVE'                    -- Status
)
ON CONFLICT (user_id, organization_id) DO NOTHING;
```

### Option 2: Use Organization Invitation System

If the invitation system is implemented, you can:
1. Send an invitation to the user's email
2. User accepts the invitation
3. User is automatically added to the organization

### Option 3: Update Role Assignment to Include Organization

When assigning the ORG_ADMIN role, **also specify the organizationId**:

```javascript
// In the frontend, when assigning roles
const assignRolesToUser = async (userId, roleIds, organizationId) => {
  await rbacService.assignRolesToUser({
    userId: userId,
    roleIds: roleIds,
    organizationId: organizationId  // ← Include this!
  });
};
```

This will:
- Store the role with the organization context
- But you still need to add the user to `admin.user_organizations` for full functionality

---

## Recommended Workflow

### For ORG_ADMIN Users:

1. **Create/Select Organization**
   - Ensure the target organization exists
   - Note the organization ID

2. **Assign User to Organization** (in `admin.user_organizations`)
   ```sql
   INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary, status)
   SELECT 
       u.id,
       o.id,
       'ADMIN',  -- Organization role
       true,      -- Primary organization
       'ACTIVE'
   FROM users.users u, admin.organizations o
   WHERE u.username = 'your-username'
     AND o.code = 'YOUR_ORG_CODE'
   ON CONFLICT (user_id, organization_id) DO UPDATE
   SET role = 'ADMIN', is_primary = true, status = 'ACTIVE';
   ```

3. **Assign ORG_ADMIN Role with Organization Context**
   - When assigning the role, include the `organizationId` in the request
   - This ensures the role is scoped to that organization

4. **Verify**
   ```sql
   -- Check user's organizations
   SELECT uo.*, o.name, o.code
   FROM admin.user_organizations uo
   JOIN admin.organizations o ON o.id = uo.organization_id
   WHERE uo.user_id = 'USER_UUID_HERE';
   
   -- Check user's roles
   SELECT ur.*, r.code, r.name
   FROM rbac.user_roles ur
   JOIN rbac.roles r ON r.id = ur.role_id
   WHERE ur.user_id = 'USER_UUID_HERE';
   ```

---

## Best Practices

1. **Always assign users to organizations** when giving them ORG_ADMIN role
2. **Set primary organization** (`is_primary = true`) for users with single organization
3. **Use organization context** when assigning roles (include `organizationId`)
4. **Verify both**:
   - User is in `admin.user_organizations`
   - Role is assigned in `rbac.user_roles` (preferably with organizationId)

---

## Current System Behavior

### When `currentOrganizationId` is null:
- `getUserRoles(userId, null)` returns **ALL roles** (including global ORG_ADMIN)
- `getUserPermissions(userId, null)` returns **ALL permissions** from all roles
- User appears to have ORG_ADMIN permissions, but they're not scoped

### When `currentOrganizationId` is set:
- `getUserRoles(userId, orgId)` returns roles for that org + global roles
- `getUserPermissions(userId, orgId)` returns permissions scoped to that org
- ORG_ADMIN permissions work correctly within the organization

---

## Fixing the Current User

To fix a user who has ORG_ADMIN but no organization:

```sql
-- Step 1: Find user and organization
SELECT id, username FROM users.users WHERE username = 'your-username';
SELECT id, name, code FROM admin.organizations LIMIT 1;  -- Use first org or specific one

-- Step 2: Add user to organization
INSERT INTO admin.user_organizations (user_id, organization_id, role, is_primary, status, joined_at)
VALUES (
    'USER_UUID',      -- From step 1
    'ORG_UUID',       -- From step 1
    'ADMIN',
    true,
    'ACTIVE',
    NOW()
)
ON CONFLICT (user_id, organization_id) DO NOTHING;

-- Step 3: (Optional) Update role assignment to include organization
-- This ensures the role is properly scoped
UPDATE rbac.user_roles ur
SET organization_id = 'ORG_UUID'
FROM rbac.roles r
WHERE ur.user_id = 'USER_UUID'
  AND ur.role_id = r.id
  AND r.code = 'ORG_ADMIN'
  AND ur.organization_id IS NULL;
```

---

## Summary

**Answer to your question:** The user belongs to **NO organization** until explicitly assigned in the `admin.user_organizations` table.

**To fix:** Add the user to an organization using one of the methods above.

**For proper ORG_ADMIN functionality:**
1. User must be in `admin.user_organizations` table
2. ORG_ADMIN role should ideally be assigned with `organizationId` specified
3. User needs to have a `currentOrganizationId` set (usually from primary organization)

---

**Last Updated:** 2024
