package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.PharmacyLocation;
import com.easyops.hospitalpharmacy.integration.HospitalServiceClient;
import com.easyops.hospitalpharmacy.integration.dto.DispenseClinicalScreenResponsePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClinicalSafetyAtDispenseServiceTest {

    private final UUID patientId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    private PharmacyIntegrationProperties integrationProperties;
    private RecordingHospitalClient hospitalClient;
    private ClinicalSafetyAtDispenseService service;

    @BeforeEach
    void setUp() {
        integrationProperties = new PharmacyIntegrationProperties();
        hospitalClient = new RecordingHospitalClient();
        service = new ClinicalSafetyAtDispenseService(integrationProperties, hospitalClient);
    }

    @Test
    void skipsWhenDisabled() {
        integrationProperties.getClinicalSafety().setCheckAtDispenseEnabled(false);
        DispenseOrder order = orderWithPatient();
        DispenseLineRequest line = new DispenseLineRequest();
        assertThatCode(() -> service.assertDispenseAllowed(order, sampleDrug(), line, actorId, orgId))
                .doesNotThrowAnyException();
        assertThat(hospitalClient.callCount.get()).isZero();
    }

    @Test
    void skipsWhenNoPatient() {
        integrationProperties.getClinicalSafety().setCheckAtDispenseEnabled(true);
        DispenseOrder order = DispenseOrder.builder()
                .patientId(null)
                .pharmacyLocation(PharmacyLocation.builder()
                        .id(UUID.randomUUID())
                        .name("P")
                        .type("OPD")
                        .active(true)
                        .build())
                .status(DispenseOrder.Status.IN_PROGRESS)
                .contextType(DispenseOrder.ContextType.WALK_IN)
                .build();
        DispenseLineRequest line = new DispenseLineRequest();
        assertThatCode(() -> service.assertDispenseAllowed(order, sampleDrug(), line, actorId, orgId))
                .doesNotThrowAnyException();
        assertThat(hospitalClient.callCount.get()).isZero();
    }

    @Test
    void blocksWhenMajorInteractionAndNoOverride() {
        integrationProperties.getClinicalSafety().setCheckAtDispenseEnabled(true);
        hospitalClient.nextResponse = majorInteractionScreen();

        DispenseLineRequest line = new DispenseLineRequest();
        assertThatThrownBy(() -> service.assertDispenseAllowed(orderWithPatient(), sampleDrug(), line, actorId, orgId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clinical safety screening");
        assertThat(hospitalClient.callCount.get()).isEqualTo(1);
    }

    @Test
    void allowsWhenOverrideProvided() {
        integrationProperties.getClinicalSafety().setCheckAtDispenseEnabled(true);
        hospitalClient.nextResponse = majorInteractionScreen();

        DispenseLineRequest line = new DispenseLineRequest();
        line.setClinicalSafetyOverrideReason("Attending reviewed — proceed with dispense.");
        assertThatCode(() -> service.assertDispenseAllowed(orderWithPatient(), sampleDrug(), line, actorId, orgId))
                .doesNotThrowAnyException();
    }

    @Test
    void crossReactivityOnlyDoesNotBlockWhenPolicyOff() {
        integrationProperties.getClinicalSafety().setCheckAtDispenseEnabled(true);
        integrationProperties.getClinicalSafety().setBlockOnCrossReactivityAllergies(false);
        DispenseClinicalScreenResponsePayload screen = new DispenseClinicalScreenResponsePayload();
        DispenseClinicalScreenResponsePayload.AllergyCheckPayload allergies =
                new DispenseClinicalScreenResponsePayload.AllergyCheckPayload();
        allergies.setHasAllergies(true);
        DispenseClinicalScreenResponsePayload.AllergyDetailPayload cross =
                new DispenseClinicalScreenResponsePayload.AllergyDetailPayload();
        cross.setMatchType("CROSS_REACTIVITY");
        allergies.setAllergies(List.of(cross));
        allergies.setSummary("cross only");
        screen.setAllergies(allergies);
        screen.setInteractions(new DispenseClinicalScreenResponsePayload.DrugInteractionPayload());
        screen.getInteractions().setHasInteractions(false);
        hospitalClient.nextResponse = screen;

        DispenseLineRequest line = new DispenseLineRequest();
        assertThatCode(() -> service.assertDispenseAllowed(orderWithPatient(), sampleDrug(), line, actorId, orgId))
                .doesNotThrowAnyException();
    }

    private static DispenseClinicalScreenResponsePayload majorInteractionScreen() {
        DispenseClinicalScreenResponsePayload screen = new DispenseClinicalScreenResponsePayload();
        DispenseClinicalScreenResponsePayload.DrugInteractionPayload di =
                new DispenseClinicalScreenResponsePayload.DrugInteractionPayload();
        di.setHasInteractions(true);
        DispenseClinicalScreenResponsePayload.InteractionDetailPayload det =
                new DispenseClinicalScreenResponsePayload.InteractionDetailPayload();
        det.setSeverity("MAJOR");
        di.setInteractions(List.of(det));
        di.setSummary("major interaction");
        screen.setInteractions(di);
        DispenseClinicalScreenResponsePayload.AllergyCheckPayload al =
                new DispenseClinicalScreenResponsePayload.AllergyCheckPayload();
        al.setHasAllergies(false);
        screen.setAllergies(al);
        return screen;
    }

    private DispenseOrder orderWithPatient() {
        return DispenseOrder.builder()
                .patientId(patientId)
                .pharmacyLocation(PharmacyLocation.builder()
                        .id(UUID.randomUUID())
                        .name("P")
                        .type("OPD")
                        .active(true)
                        .build())
                .status(DispenseOrder.Status.IN_PROGRESS)
                .contextType(DispenseOrder.ContextType.PATIENT_PRESCRIPTION)
                .build();
    }

    private static Drug sampleDrug() {
        return Drug.builder()
                .genericName("Aspirin")
                .brandName("BrandX")
                .build();
    }

    private static final class RecordingHospitalClient extends HospitalServiceClient {
        final AtomicInteger callCount = new AtomicInteger();
        DispenseClinicalScreenResponsePayload nextResponse;

        RecordingHospitalClient() {
            super(new RestTemplate());
        }

        @Override
        public DispenseClinicalScreenResponsePayload postDispenseClinicalScreen(
                UUID actorUserId,
                UUID organizationId,
                UUID patientId,
                String medicationCode,
                String medicationName) {
            callCount.incrementAndGet();
            return nextResponse;
        }
    }
}
