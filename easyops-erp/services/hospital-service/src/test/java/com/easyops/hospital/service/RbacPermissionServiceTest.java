package com.easyops.hospital.service;

import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 — role separation: prescribe vs transmit use different alternative sets; client enforces.
 */
class RbacPermissionServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    @Test
    void requirePrescriptionView_delegatesWithRxViewMetric() {
        CapturingRbacClient rbac = new CapturingRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        subject.requirePrescriptionView(userId, orgId);
        assertThat(rbac.lastMetric).isEqualTo("rx_view");
    }

    @Test
    void requirePrescriptionPrescribe_delegatesWithRxPrescribeMetric() {
        CapturingRbacClient rbac = new CapturingRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        subject.requirePrescriptionPrescribe(userId, orgId);
        assertThat(rbac.lastMetric).isEqualTo("rx_prescribe");
    }

    @Test
    void requirePrescriptionTransmit_delegatesWithRxTransmitMetric() {
        CapturingRbacClient rbac = new CapturingRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        subject.requirePrescriptionTransmit(userId, orgId);
        assertThat(rbac.lastMetric).isEqualTo("rx_transmit");
    }

    @Test
    void transmitAlternatives_includeTransmitNotPrescribeAction() {
        CapturingRbacClient rbac = new CapturingRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        subject.requirePrescriptionTransmit(userId, orgId);
        assertThat(Arrays.stream(rbac.lastPairs).map(a -> a[1])).contains("transmit");
        assertThat(Arrays.stream(rbac.lastPairs).map(a -> a[1])).doesNotContain("prescribe");
    }

    @Test
    void prescribeAlternatives_includePrescribeNotTransmitAction() {
        CapturingRbacClient rbac = new CapturingRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        subject.requirePrescriptionPrescribe(userId, orgId);
        assertThat(Arrays.stream(rbac.lastPairs).map(a -> a[1])).contains("prescribe");
        assertThat(Arrays.stream(rbac.lastPairs).map(a -> a[1])).doesNotContain("transmit");
    }

    @Test
    void requirePrescriptionTransmit_forbiddenWhenClientDenies() {
        DenyingTransmitRbacClient rbac = new DenyingTransmitRbacClient();
        RbacPermissionService subject = new RbacPermissionService(rbac);
        assertThatThrownBy(() -> subject.requirePrescriptionTransmit(userId, orgId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    /** Records last call for assertions (no Mockito — compatible with JDK 25). */
    private static class CapturingRbacClient extends RbacPermissionClient {
        String[][] lastPairs;
        String lastMetric;

        CapturingRbacClient() {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-test");
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            this.lastPairs = resourceActionPairs;
            this.lastMetric = metricOperation;
        }
    }

    private static class DenyingTransmitRbacClient extends RbacPermissionClient {
        DenyingTransmitRbacClient() {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-test");
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            if ("rx_transmit".equals(metricOperation)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
            }
        }
    }
}
