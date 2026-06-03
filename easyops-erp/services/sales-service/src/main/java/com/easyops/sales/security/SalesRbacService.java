package com.easyops.sales.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Phase 2 — aligns with {@code SALES_VIEW} / {@code SALES_MANAGE} ({@code sales}/{@code view|manage}).
 */
@Service
public class SalesRbacService {

    private static final String[][] SALES_VIEW = {
            {"sales", "view"},
            {"sales", "manage"},
    };

    private static final String[][] SALES_MANAGE = {
            {"sales", "manage"},
    };

    private final RbacPermissionClient rbac;

    public SalesRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireSalesView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, SALES_VIEW, "sales_view");
    }

    public void requireSalesManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, SALES_MANAGE, "sales_manage");
    }
}
