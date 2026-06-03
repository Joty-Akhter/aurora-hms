package com.easyops.hr.controller;

import com.easyops.hr.entity.Employee;
import com.easyops.hr.security.HrRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getAllEmployees_forbiddenWhenViewDenied() {
        HrRbacService rbac = new HrRbacService(new DenyingRbacClient("hr_view"));
        EmployeeController controller = new EmployeeController(null, rbac);

        assertThatThrownBy(() -> controller.getAllEmployees(USER_HEADER, ORG, null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAllEmployees_pagedForbiddenWhenViewDenied() {
        HrRbacService rbac = new HrRbacService(new DenyingRbacClient("hr_view"));
        EmployeeController controller = new EmployeeController(null, rbac);

        assertThatThrownBy(() -> controller.getAllEmployees(USER_HEADER, ORG, null, null, null, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createEmployee_forbiddenWhenManageDenied() {
        HrRbacService rbac = new HrRbacService(new DenyingRbacClient("hr_manage"));
        EmployeeController controller = new EmployeeController(null, rbac);

        Employee body = new Employee();
        body.setOrganizationId(ORG);
        body.setName("RBAC");

        assertThatThrownBy(() -> controller.createEmployee(USER_HEADER, body))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hr-test");
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
