package com.easyops.users.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Phase 2 — user CRUD RBAC (Wave 1). Aligns with {@code USER_VIEW} / {@code USER_MANAGE} and system admin permissions.
 */
@Service
public class UserManagementRbacService {

    private static final String[][] USER_READ = {
            {"users", "view"},
            {"users", "manage"},
            {"system", "view"},
    };

    private static final String[][] USER_WRITE = {
            {"users", "manage"},
            {"system", "configure"},
    };

    private final RbacPermissionClient rbac;

    public UserManagementRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireUserRead(UUID actorUserId, UUID rbacOrganizationContext) {
        rbac.requireAnyResourceAction(actorUserId, rbacOrganizationContext, USER_READ, "user_read");
    }

    public void requireUserWrite(UUID actorUserId, UUID rbacOrganizationContext) {
        rbac.requireAnyResourceAction(actorUserId, rbacOrganizationContext, USER_WRITE, "user_write");
    }
}
