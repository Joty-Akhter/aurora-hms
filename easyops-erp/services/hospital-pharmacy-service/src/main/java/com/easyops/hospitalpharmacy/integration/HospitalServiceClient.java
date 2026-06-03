package com.easyops.hospitalpharmacy.integration;

import com.easyops.hospitalpharmacy.integration.dto.DispenseClinicalScreenRequestPayload;
import com.easyops.hospitalpharmacy.integration.dto.DispenseClinicalScreenResponsePayload;
import com.easyops.hospitalpharmacy.integration.dto.EncounterLookupResponse;
import com.easyops.hospitalpharmacy.integration.dto.InHouseDispenseFillPayload;
import com.easyops.hospitalpharmacy.integration.dto.InHouseDispenseFillResponsePayload;
import com.easyops.hospitalpharmacy.integration.dto.PatientLookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Calls hospital-service in-house fill sync (WS-B).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HospitalServiceClient {

    private static final String URL = "http://hospital-service/api/integrations/pharmacy/in-house-dispense-fill";
    private static final String CLINICAL_SCREEN_URL =
            "http://hospital-service/api/integrations/pharmacy/dispense-clinical-screen";

    private final RestTemplate loadBalancedRestTemplate;

    public InHouseDispenseFillResponsePayload postInHouseDispenseFill(
            UUID actorUserId,
            UUID organizationId,
            InHouseDispenseFillPayload body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        HttpEntity<InHouseDispenseFillPayload> entity = new HttpEntity<>(body, headers);
        return loadBalancedRestTemplate.postForObject(URL, entity, InHouseDispenseFillResponsePayload.class);
    }

    /**
     * WS-I — combined interaction + allergy screening for a drug about to be dispensed.
     */
    public DispenseClinicalScreenResponsePayload postDispenseClinicalScreen(
            UUID actorUserId,
            UUID organizationId,
            UUID patientId,
            String medicationCode,
            String medicationName) {
        DispenseClinicalScreenRequestPayload body = DispenseClinicalScreenRequestPayload.builder()
                .patientId(patientId)
                .medicationCode(medicationCode)
                .medicationName(medicationName)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", actorUserId.toString());
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        HttpEntity<DispenseClinicalScreenRequestPayload> entity = new HttpEntity<>(body, headers);
        return loadBalancedRestTemplate.postForObject(CLINICAL_SCREEN_URL, entity, DispenseClinicalScreenResponsePayload.class);
    }

    /**
     * Resolves a patient UUID from MRN / card-style identifiers (e.g. HOSP-2026-000001) via hospital-service.
     */
    public UUID lookupPatientIdByMrn(String mrn) {
        if (mrn == null || mrn.isBlank()) {
            throw new IllegalArgumentException("MRN must not be blank");
        }
        String enc = UriUtils.encodePathSegment(mrn.trim(), StandardCharsets.UTF_8);
        URI uri = URI.create("http://hospital-service/api/patients/mrn/" + enc);
        try {
            PatientLookupResponse body = loadBalancedRestTemplate.getForObject(uri, PatientLookupResponse.class);
            if (body == null || body.getPatientId() == null) {
                throw new IllegalStateException("hospital-service returned no patientId for MRN: " + mrn);
            }
            return body.getPatientId();
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("No patient found for MRN: " + mrn);
        }
    }

    /** Resolves an encounter UUID from its human-readable encounter number. */
    public UUID lookupEncounterIdByNumber(String encounterNumber) {
        if (encounterNumber == null || encounterNumber.isBlank()) {
            throw new IllegalArgumentException("Encounter number must not be blank");
        }
        String enc = UriUtils.encodePathSegment(encounterNumber.trim(), StandardCharsets.UTF_8);
        URI uri = URI.create("http://hospital-service/api/encounters/number/" + enc);
        try {
            EncounterLookupResponse body = loadBalancedRestTemplate.getForObject(uri, EncounterLookupResponse.class);
            if (body == null || body.getEncounterId() == null) {
                throw new IllegalStateException("hospital-service returned no encounterId for number: " + encounterNumber);
            }
            return body.getEncounterId();
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("No encounter found for number: " + encounterNumber);
        }
    }
}
