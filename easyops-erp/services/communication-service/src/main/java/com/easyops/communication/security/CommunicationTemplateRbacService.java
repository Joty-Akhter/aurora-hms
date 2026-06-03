package com.easyops.communication.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CommunicationTemplateRbacService {

    private static final String RESOURCE = "communication_template";
    private static final String OPERATIONS_RESOURCE = "communication_operations";
    /** MainLayout menu uses resource {@code communication} (view/manage). */
    private static final String MENU_RESOURCE = "communication";

    private static final String[][] TEMPLATE_READ = {
            {MENU_RESOURCE, "view"},
            {MENU_RESOURCE, "manage"},
            {RESOURCE, "view"},
            {RESOURCE, "manage"},
    };

    private static final String[][] TEMPLATE_MANAGE = {
            {MENU_RESOURCE, "manage"},
            {RESOURCE, "manage"},
    };

    private static final String[][] OPERATIONS_READ = {
            {MENU_RESOURCE, "view"},
            {MENU_RESOURCE, "manage"},
            {OPERATIONS_RESOURCE, "view"},
            {OPERATIONS_RESOURCE, "manage"},
            {RESOURCE, "view"},
            {RESOURCE, "manage"},
    };

    private static final String[][] OPERATIONS_MANAGE = {
            {MENU_RESOURCE, "manage"},
            {OPERATIONS_RESOURCE, "manage"},
            {RESOURCE, "manage"},
    };

    private final RbacPermissionClient rbac;

    public CommunicationTemplateRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireRead(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, TEMPLATE_READ, "communication_template_read");
    }

    public void requireManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, TEMPLATE_MANAGE, "communication_template_manage");
    }

    public void requireOperationsRead(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, OPERATIONS_READ, "communication_operations_read");
    }

    public void requireOperationsManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, OPERATIONS_MANAGE, "communication_operations_manage");
    }
}
