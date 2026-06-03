package com.easyops.rbac.config;

import com.easyops.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Ensures the SYSTEM_ADMIN role always has every permission assigned on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemAdminPermissionInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final RoleRepository roleRepository;

    private volatile boolean initialized = false;

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }

        int assigned = roleRepository.assignAllPermissionsToRole("SYSTEM_ADMIN");
        if (assigned > 0) {
            log.info("Assigned {} permissions to SYSTEM_ADMIN role during initialization", assigned);
        } else {
            log.info("SYSTEM_ADMIN role already has all permissions assigned or role not found");
        }

        initialized = true;
    }
}

