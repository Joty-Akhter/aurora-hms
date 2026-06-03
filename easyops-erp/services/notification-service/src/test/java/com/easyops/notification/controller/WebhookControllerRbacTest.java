package com.easyops.notification.controller;

import com.easyops.notification.security.NotificationRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getWebhook_forbiddenWhenReadDenied() {
        NotificationRbacService rbac = new NotificationRbacService(new DenyingRbacClient("notification_system_read"));
        WebhookController controller = new WebhookController(null, rbac);

        assertThatThrownBy(() -> controller.getWebhook(UUID.randomUUID(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createWebhook_forbiddenWhenConfigureDenied() {
        NotificationRbacService rbac = new NotificationRbacService(new DenyingRbacClient("notification_system_configure"));
        WebhookController controller = new WebhookController(null, rbac);

        assertThatThrownBy(() -> controller.createWebhook(null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getDeliveryHistory_forbiddenWhenReadDenied() {
        NotificationRbacService rbac = new NotificationRbacService(new DenyingRbacClient("notification_system_read"));
        WebhookController controller = new WebhookController(null, rbac);

        assertThatThrownBy(() -> controller.getDeliveryHistory(UUID.randomUUID(), PageRequest.of(0, 10), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "notification-test");
            this.denyMetric = denyMetric;
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            if (denyMetric.equals(metricOperation)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
            }
        }
    }
}
