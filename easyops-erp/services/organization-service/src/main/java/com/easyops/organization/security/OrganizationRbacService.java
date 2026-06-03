package com.easyops.organization.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Phase 2 — organization module RBAC (Wave 1). Maps to {@code rbac.permissions}:
 * {@code ORG_VIEW} / {@code ORG_MANAGE} plus {@code SYSTEM_VIEW} / {@code SYSTEM_CONFIG} for platform admins.
 */
@Service
public class OrganizationRbacService {

    private static final String[][] ORG_READ = {
            {"organizations", "view"},
            {"organizations", "manage"},
            {"system", "view"},
    };

    private static final String[][] ORG_WRITE = {
            {"organizations", "manage"},
            {"system", "configure"},
    };

    private final RbacPermissionClient rbac;

    public OrganizationRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireOrgRead(UUID actorUserId, UUID rbacOrganizationContext) {
        rbac.requireAnyResourceAction(actorUserId, rbacOrganizationContext, ORG_READ, "org_read");
    }

    public void requireOrgWrite(UUID actorUserId, UUID rbacOrganizationContext) {
        rbac.requireAnyResourceAction(actorUserId, rbacOrganizationContext, ORG_WRITE, "org_write");
    }
}
