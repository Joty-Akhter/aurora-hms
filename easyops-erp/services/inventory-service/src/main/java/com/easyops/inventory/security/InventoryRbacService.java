package com.easyops.inventory.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Phase 2 — Wave 3 pilot: aligns with {@code INVENTORY_VIEW} / {@code INVENTORY_MANAGE}
 * ({@code inventory}/{@code view|manage} in {@code 011-rbac-module-permissions.sql}).
 */
@Service
public class InventoryRbacService {

    private static final String[][] INVENTORY_VIEW = {
            {"inventory", "view"},
            {"inventory", "manage"},
    };

    private static final String[][] INVENTORY_MANAGE = {
            {"inventory", "manage"},
    };

    private final RbacPermissionClient rbac;

    public InventoryRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireInventoryView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, INVENTORY_VIEW, "inventory_view");
    }

    public void requireInventoryManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, INVENTORY_MANAGE, "inventory_manage");
    }
}
