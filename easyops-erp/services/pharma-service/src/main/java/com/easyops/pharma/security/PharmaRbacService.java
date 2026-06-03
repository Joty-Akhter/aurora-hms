package com.easyops.pharma.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Aligns with {@code PHARMA_VIEW} / {@code PHARMA_MANAGE} ({@code pharma}/{@code view|manage} in
 * {@code 015-pharma-permissions.sql}).
 */
@Service
public class PharmaRbacService {

    private static final String[][] PHARMA_VIEW = {
            {"pharma", "view"},
            {"pharma", "manage"},
    };

    private static final String[][] PHARMA_MANAGE = {
            {"pharma", "manage"},
    };

    private final RbacPermissionClient rbac;

    public PharmaRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requirePharmaView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMA_VIEW, "pharma_view");
    }

    public void requirePharmaManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PHARMA_MANAGE, "pharma_manage");
    }
}
