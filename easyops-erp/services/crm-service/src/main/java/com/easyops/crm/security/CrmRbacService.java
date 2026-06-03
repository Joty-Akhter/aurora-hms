package com.easyops.crm.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Aligns with {@code CRM_VIEW} / {@code CRM_MANAGE} ({@code crm}/
 * {@code view|manage} in {@code 011-rbac-module-permissions.sql}).
 */
@Service
public class CrmRbacService {

    private static final String[][] CRM_VIEW = {
            {"crm", "view"},
            {"crm", "manage"},
    };

    private static final String[][] CRM_MANAGE = {
            {"crm", "manage"},
    };

    private final RbacPermissionClient rbac;

    public CrmRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireCrmView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, CRM_VIEW, "crm_view");
    }

    public void requireCrmManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, CRM_MANAGE, "crm_manage");
    }
}
