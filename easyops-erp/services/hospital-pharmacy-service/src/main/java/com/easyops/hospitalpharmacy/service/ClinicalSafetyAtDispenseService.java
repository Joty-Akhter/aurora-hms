package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.config.PharmacyIntegrationProperties;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.integration.HospitalServiceClient;
import com.easyops.hospitalpharmacy.integration.dto.DispenseClinicalScreenResponsePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Phase P4 — WS-I: optional dispense-time interaction + allergy screening via hospital-service (I1),
 * severity policy (I2), and documented override on the line (I3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalSafetyAtDispenseService {

    private final PharmacyIntegrationProperties integrationProperties;
    private final HospitalServiceClient hospitalServiceClient;

    public void assertDispenseAllowed(
            DispenseOrder order,
            Drug drug,
            DispenseLineRequest lineRequest,
            UUID actorUserId,
            UUID organizationId) {
        PharmacyIntegrationProperties.ClinicalSafety cfg = integrationProperties.getClinicalSafety();
        if (!cfg.isCheckAtDispenseEnabled()) {
            return;
        }
        if (order.getPatientId() == null) {
            return;
        }

        DispenseClinicalScreenResponsePayload screen;
        try {
            screen = hospitalServiceClient.postDispenseClinicalScreen(
                    actorUserId,
                    organizationId,
                    order.getPatientId(),
                    null,
                    buildMedicationLabel(drug));
        } catch (RestClientException ex) {
            log.warn("Clinical safety screening call failed: {}", ex.getMessage());
            throw new IllegalArgumentException(
                    "Clinical safety screening is unavailable (hospital-service). Fix connectivity or disable "
                            + "hospital.pharmacy.integration.clinical-safety.check-at-dispense-enabled.");
        }

        boolean blockInteractions = hasBlockingInteraction(screen, cfg);
        boolean blockAllergies = hasBlockingAllergy(screen, cfg);

        if (!blockInteractions && !blockAllergies) {
            return;
        }

        String override = lineRequest.getClinicalSafetyOverrideReason();
        if (!StringUtils.hasText(override)) {
            StringBuilder msg = new StringBuilder("Clinical safety screening requires review before dispensing this line.");
            if (blockInteractions && screen.getInteractions() != null) {
                msg.append(" Interactions: ").append(screen.getInteractions().getSummary());
            }
            if (blockAllergies && screen.getAllergies() != null) {
                msg.append(" Allergies: ").append(screen.getAllergies().getSummary());
            }
            msg.append(" Provide clinicalSafetyOverrideReason to document override, or choose a different SKU.");
            throw new IllegalArgumentException(msg.toString());
        }
    }

    private static boolean hasBlockingInteraction(
            DispenseClinicalScreenResponsePayload screen,
            PharmacyIntegrationProperties.ClinicalSafety cfg) {
        if (screen.getInteractions() == null
                || !Boolean.TRUE.equals(screen.getInteractions().getHasInteractions())) {
            return false;
        }
        List<DispenseClinicalScreenResponsePayload.InteractionDetailPayload> details =
                screen.getInteractions().getInteractions();
        if (details == null || details.isEmpty()) {
            return false;
        }
        Set<String> block = cfg.getBlockInteractionSeverities().stream()
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        for (DispenseClinicalScreenResponsePayload.InteractionDetailPayload d : details) {
            if (d.getSeverity() == null) {
                continue;
            }
            if (block.contains(d.getSeverity().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlockingAllergy(
            DispenseClinicalScreenResponsePayload screen,
            PharmacyIntegrationProperties.ClinicalSafety cfg) {
        if (screen.getAllergies() == null
                || !Boolean.TRUE.equals(screen.getAllergies().getHasAllergies())) {
            return false;
        }
        List<DispenseClinicalScreenResponsePayload.AllergyDetailPayload> details = screen.getAllergies().getAllergies();
        if (details == null || details.isEmpty()) {
            return false;
        }
        boolean blockCross = cfg.isBlockOnCrossReactivityAllergies();
        for (DispenseClinicalScreenResponsePayload.AllergyDetailPayload d : details) {
            String mt = d.getMatchType();
            if (mt != null && "CROSS_REACTIVITY".equalsIgnoreCase(mt.trim()) && !blockCross) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static String buildMedicationLabel(Drug drug) {
        String generic = drug.getGenericName() != null ? drug.getGenericName() : "";
        String brand = drug.getBrandName() != null ? drug.getBrandName() : "";
        if (!brand.isEmpty()) {
            return generic + " (" + brand + ")";
        }
        return generic;
    }
}
