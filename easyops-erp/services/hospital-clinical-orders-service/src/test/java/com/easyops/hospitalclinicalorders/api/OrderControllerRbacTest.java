package com.easyops.hospitalclinicalorders.api;

import com.easyops.hospitalclinicalorders.security.HospitalClinicalOrdersRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getById_forbiddenWhenViewDenied() {
        HospitalClinicalOrdersRbacService rbac = new HospitalClinicalOrdersRbacService(new DenyingRbacClient("hospital_clinical_orders_view"));
        OrderController controller = new OrderController(null, null, rbac);

        assertThatThrownBy(() -> controller.getById(UUID.randomUUID(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void cancel_forbiddenWhenManageDenied() {
        HospitalClinicalOrdersRbacService rbac = new HospitalClinicalOrdersRbacService(new DenyingRbacClient("hospital_clinical_orders_manage"));
        OrderController controller = new OrderController(null, null, rbac);

        assertThatThrownBy(() -> controller.cancel(UUID.randomUUID(), null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-clinical-orders-test");
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
