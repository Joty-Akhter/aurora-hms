package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.ReplaceStaffCardRequest;
import com.easyops.hospitalcard.api.dto.StaffCardActionRequest;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffCardControllerRbacTest {

    private static final String USER_HEADER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void verify_forbiddenWhenViewDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_view"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.verify("CARD-001", null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void issue_forbiddenWhenManageDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_manage"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.issue(null, USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void replace_forbiddenWhenManageDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_manage"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.replace(UUID.randomUUID(), new ReplaceStaffCardRequest(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void suspend_forbiddenWhenManageDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_manage"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.suspend(UUID.randomUUID(), new StaffCardActionRequest(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void revoke_forbiddenWhenManageDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_manage"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.revoke(UUID.randomUUID(), new StaffCardActionRequest(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void reprint_forbiddenWhenManageDenied() {
        HospitalCardRbacService rbac = new HospitalCardRbacService(new DenyingRbacClient("hospital_card_manage"));
        StaffCardController controller = new StaffCardController(null, rbac);

        assertThatThrownBy(() -> controller.reprint(UUID.randomUUID(), new StaffCardActionRequest(), USER_HEADER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-card-test");
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
