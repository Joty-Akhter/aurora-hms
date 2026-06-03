package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.PrescriptionRequest;
import com.easyops.hospital.dto.request.PrescriptionTransmitRequest;
import com.easyops.hospital.service.EPrescribingService;
import com.easyops.hospital.service.FormularyService;
import com.easyops.hospital.service.PDMPService;
import com.easyops.hospital.service.PrescriptionRefillService;
import com.easyops.hospital.service.PrescriptionService;
import com.easyops.hospital.service.PriorAuthorizationService;
import com.easyops.hospital.service.RbacPermissionService;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 0/1 gap closure: forbidden paths return {@link HttpStatus#FORBIDDEN} before any
 * downstream service runs. Uses a real {@link RbacPermissionService} with a stub
 * {@link RbacPermissionClient} (avoids Mockito mocking JDK 25 + concrete @Service classes).
 */
class PrescriptionControllerRbacTest {

    private static final UUID USER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void getPrescriptionById_forbiddenWhenViewDenied() {
        RbacPermissionService rbac = new RbacPermissionService(new DenyingRbacClient("rx_view"));
        PrescriptionController controller = newController(rbac);

        assertThatThrownBy(() -> controller.getPrescriptionById(UUID.randomUUID(), USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createPrescription_forbiddenWhenPrescribeDenied() {
        RbacPermissionService rbac = new RbacPermissionService(new DenyingRbacClient("rx_prescribe"));
        PrescriptionController controller = newController(rbac);

        PrescriptionRequest body = PrescriptionRequest.builder()
                .patientId(UUID.randomUUID())
                .prescribingProviderId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() -> controller.createPrescription(body, USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void transmitPrescription_forbiddenWhenTransmitDenied() {
        RbacPermissionService rbac = new RbacPermissionService(new DenyingRbacClient("rx_transmit"));
        PrescriptionController controller = newController(rbac);

        PrescriptionTransmitRequest body = PrescriptionTransmitRequest.builder()
                .overrideInteractions(false)
                .overrideAllergies(false)
                .build();

        assertThatThrownBy(() -> controller.transmitPrescription(UUID.randomUUID(), body, USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    /**
     * RBAC runs first; downstream services are not used when RBAC denies — null is safe.
     */
    private static PrescriptionController newController(RbacPermissionService rbac) {
        return new PrescriptionController(
                (PrescriptionService) null,
                (PrescriptionRefillService) null,
                (FormularyService) null,
                (PriorAuthorizationService) null,
                (PDMPService) null,
                (EPrescribingService) null,
                rbac);
    }

    /** Fails {@link RbacPermissionClient#requireAnyResourceAction} when the metric matches {@code denyMetric}. */
    private static class DenyingRbacClient extends RbacPermissionClient {
        private final String denyMetric;

        DenyingRbacClient(String denyMetric) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-test");
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
