package com.easyops.pharma.controller;

import com.easyops.pharma.entity.Division;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerritoryControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getAllDivisions_forbiddenWhenViewDenied() {
        PharmaRbacService rbac = new PharmaRbacService(new DenyingRbacClient("pharma_view"));
        TerritoryController controller = new TerritoryController(null, null, rbac);

        assertThatThrownBy(() -> controller.getAllDivisions(USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createDivision_forbiddenWhenManageDenied() {
        PharmaRbacService rbac = new PharmaRbacService(new DenyingRbacClient("pharma_manage"));
        TerritoryController controller = new TerritoryController(null, null, rbac);

        Division body = new Division();
        body.setOrganizationId(ORG);
        body.setName("RBAC");

        assertThatThrownBy(() -> controller.createDivision(USER_HEADER, body))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "pharma-test");
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
