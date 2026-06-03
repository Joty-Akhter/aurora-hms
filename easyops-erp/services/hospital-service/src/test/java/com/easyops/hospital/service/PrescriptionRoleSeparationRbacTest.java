package com.easyops.hospital.service;

import com.easyops.rbac.client.RbacPermissionClient;
import com.easyops.rbac.client.dto.RbacPermissionDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 — role separation using effective permission sets aligned with Liquibase seeds
 * {@code 035-prescribing-authority-role.sql}, {@code 037-prescribing-authority-hospital-view.sql},
 * and {@code 036-phase4-clinical-roles.sql}.
 */
class PrescriptionRoleSeparationRbacTest {

    private static final UUID USER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ORG = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static RbacPermissionDto perm(String resource, String action) {
        RbacPermissionDto d = new RbacPermissionDto();
        d.setResource(resource);
        d.setAction(action);
        d.setIsActive(true);
        return d;
    }

    /** {@code PHARMACIST_DISPENSER} — 036: dispense + rx view + hospital view (no prescribe/transmit). */
    private static final List<RbacPermissionDto> PHARMACIST_DISPENSER_LIKE = List.of(
            perm("hospital.pharmacy", "dispense"),
            perm("hospital.prescription", "view"),
            perm("hospital", "view")
    );

    /** {@code E_PRESCRIBING_TRANSMITTER} — 036: hospital view + rx view + transmit (no prescribe). */
    private static final List<RbacPermissionDto> E_PRESCRIBING_TRANSMITTER_LIKE = List.of(
            perm("hospital", "view"),
            perm("hospital.prescription", "view"),
            perm("hospital.prescription", "transmit")
    );

    /** {@code PRESCRIBING_AUTHORITY} — 035 + 037: rx view + prescribe + transmit + {@code HOSPITAL_VIEW}. */
    private static final List<RbacPermissionDto> PRESCRIBING_AUTHORITY_LIKE = List.of(
            perm("hospital", "view"),
            perm("hospital.prescription", "view"),
            perm("hospital.prescription", "prescribe"),
            perm("hospital.prescription", "transmit")
    );

    private static class FixedPermissionsRbacClient extends RbacPermissionClient {
        private final List<RbacPermissionDto> permissions;

        FixedPermissionsRbacClient(List<RbacPermissionDto> permissions) {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-test");
            this.permissions = permissions;
        }

        @Override
        public List<RbacPermissionDto> fetchUserPermissions(UUID userId, UUID organizationId) {
            return permissions;
        }
    }

    @Test
    void pharmacistDispenser_canView_cannotPrescribeOrTransmit() {
        RbacPermissionService rbac = new RbacPermissionService(new FixedPermissionsRbacClient(PHARMACIST_DISPENSER_LIKE));

        assertThatCode(() -> rbac.requirePrescriptionView(USER, ORG)).doesNotThrowAnyException();

        assertThatThrownBy(() -> rbac.requirePrescriptionPrescribe(USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        assertThatThrownBy(() -> rbac.requirePrescriptionTransmit(USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void ePrescribingTransmitter_canViewAndTransmit_cannotPrescribe() {
        RbacPermissionService rbac = new RbacPermissionService(new FixedPermissionsRbacClient(E_PRESCRIBING_TRANSMITTER_LIKE));

        assertThatCode(() -> rbac.requirePrescriptionView(USER, ORG)).doesNotThrowAnyException();
        assertThatCode(() -> rbac.requirePrescriptionTransmit(USER, ORG)).doesNotThrowAnyException();

        assertThatThrownBy(() -> rbac.requirePrescriptionPrescribe(USER, ORG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void prescribingAuthority_canViewPrescribeAndTransmit() {
        RbacPermissionService rbac = new RbacPermissionService(new FixedPermissionsRbacClient(PRESCRIBING_AUTHORITY_LIKE));

        assertThatCode(() -> rbac.requirePrescriptionView(USER, ORG)).doesNotThrowAnyException();
        assertThatCode(() -> rbac.requirePrescriptionPrescribe(USER, ORG)).doesNotThrowAnyException();
        assertThatCode(() -> rbac.requirePrescriptionTransmit(USER, ORG)).doesNotThrowAnyException();
    }
}
