package com.easyops.ar.controller;

import com.easyops.ar.security.AccountingRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getAllInvoices_forbiddenWhenViewDenied() {
        AccountingRbacService rbac = new AccountingRbacService(new DenyingRbacClient("accounting_view"));
        InvoiceController controller = new InvoiceController(null, rbac);

        assertThatThrownBy(() -> controller.getAllInvoices(USER_HEADER, ORG, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "ar-test");
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
