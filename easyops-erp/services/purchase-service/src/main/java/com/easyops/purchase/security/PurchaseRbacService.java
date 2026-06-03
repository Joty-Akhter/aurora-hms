package com.easyops.purchase.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Phase 2 — aligns with {@code PURCHASE_VIEW} / {@code PURCHASE_MANAGE} ({@code purchase}/{@code view|manage}).
 */
@Service
public class PurchaseRbacService {

    private static final String[][] PURCHASE_VIEW = {
            {"purchase", "view"},
            {"purchase", "manage"},
    };

    private static final String[][] PURCHASE_MANAGE = {
            {"purchase", "manage"},
    };

    private final RbacPermissionClient rbac;

    public PurchaseRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requirePurchaseView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PURCHASE_VIEW, "purchase_view");
    }

    public void requirePurchaseManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PURCHASE_MANAGE, "purchase_manage");
    }
}
