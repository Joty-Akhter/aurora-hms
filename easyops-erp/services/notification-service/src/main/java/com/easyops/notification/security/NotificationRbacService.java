package com.easyops.notification.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Maps to {@code SYSTEM_VIEW} / {@code SYSTEM_CONFIG} ({@code system} / {@code view|configure}) from default RBAC seeds.
 */
@Service
public class NotificationRbacService {

    private static final String RESOURCE_SYSTEM = "system";

    private static final String[][] SYSTEM_READ = {
            {RESOURCE_SYSTEM, "view"},
            {RESOURCE_SYSTEM, "configure"},
    };

    private static final String[][] SYSTEM_CONFIGURE = {
            {RESOURCE_SYSTEM, "configure"},
    };

    private final RbacPermissionClient rbac;

    public NotificationRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireSystemRead(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, SYSTEM_READ, "notification_system_read");
    }

    public void requireSystemConfigure(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, SYSTEM_CONFIGURE, "notification_system_configure");
    }
}
