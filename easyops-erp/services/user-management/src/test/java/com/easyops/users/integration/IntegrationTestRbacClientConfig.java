package com.easyops.users.integration;

import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Avoids Mockito {@code @MockBean} on {@link RbacPermissionClient} (breaks on Java 25 inline mocks).
 * Delegates to a no-op override for integration tests (no live rbac-service).
 */
@TestConfiguration
public class IntegrationTestRbacClientConfig {

    @Bean
    @Primary
    public RbacPermissionClient rbacPermissionClientForIntegrationTests(
            RestTemplate restTemplate,
            MeterRegistry meterRegistry) {
        return new RbacPermissionClient(
                restTemplate,
                meterRegistry,
                "http://127.0.0.1:9",
                "user-management-service-test") {
            @Override
            public void requireAnyResourceAction(
                    UUID userId,
                    UUID organizationId,
                    String[][] resourceActionPairs,
                    String metricOperation) {
                requireAuthenticatedUser(userId);
            }
        };
    }
}
