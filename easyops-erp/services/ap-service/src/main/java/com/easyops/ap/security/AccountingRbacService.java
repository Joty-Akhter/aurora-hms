package com.easyops.ap.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Same catalog as accounting UI: {@code ACCOUNTING_VIEW} / {@code ACCOUNTING_MANAGE}.
 */
@Service
public class AccountingRbacService {

    private static final String[][] ACCOUNTING_VIEW = {
            {"accounting", "view"},
            {"accounting", "manage"},
    };

    private static final String[][] ACCOUNTING_MANAGE = {
            {"accounting", "manage"},
    };

    private final RbacPermissionClient rbac;

    public AccountingRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireAccountingView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, ACCOUNTING_VIEW, "accounting_view");
    }

    public void requireAccountingManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, ACCOUNTING_MANAGE, "accounting_manage");
    }
}
