package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getById_forbiddenWhenViewDenied() {
        HospitalSchedulingRbacService rbac = new HospitalSchedulingRbacService(
                new DenyingRbacClient("hospital_scheduling_appt_staff_read"),
                false
        );
        AppointmentController controller = new AppointmentController(null, rbac);

        assertThatThrownBy(() -> controller.getById(UUID.randomUUID(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void create_forbiddenWhenManageDenied() {
        HospitalSchedulingRbacService rbac = new HospitalSchedulingRbacService(
                new DenyingRbacClient("hospital_scheduling_appt_book"),
                false
        );
        AppointmentController controller = new AppointmentController(null, rbac);

        assertThatThrownBy(() -> controller.create(null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void complete_forbiddenWhenStatusUpdateDenied() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        HospitalSchedulingRbacService rbac = new HospitalSchedulingRbacService(
                new DenyingRbacClient("hospital_scheduling_appt_status_update"),
                false
        );
        AppointmentController controller = new AppointmentController(null, rbac);

        assertThatThrownBy(() -> controller.complete(id, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-scheduling-test");
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
