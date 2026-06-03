package com.easyops.manufacturing.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Aligns with {@code MANUFACTURING_VIEW} / {@code MANUFACTURING_MANAGE} ({@code manufacturing}/
 * {@code view|manage} in {@code 011-rbac-module-permissions.sql}).
 */
@Service
public class ManufacturingRbacService {

    private static final String[][] MANUFACTURING_VIEW = {
            {"manufacturing", "view"},
            {"manufacturing", "manage"},
    };

    private static final String[][] MANUFACTURING_MANAGE = {
            {"manufacturing", "manage"},
    };

    private final RbacPermissionClient rbac;

    public ManufacturingRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireManufacturingView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, MANUFACTURING_VIEW, "manufacturing_view");
    }

    public void requireManufacturingManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, MANUFACTURING_MANAGE, "manufacturing_manage");
    }
}
