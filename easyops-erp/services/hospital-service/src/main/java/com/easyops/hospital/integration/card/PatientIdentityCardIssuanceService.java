package com.easyops.hospital.integration.card;

import com.easyops.hospital.config.LoadBalancedRestTemplateConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues (or reuses) a PATIENT_IDENTITY card via {@code hospital-card-management-service} after registration.
 */
@Service
@Slf4j
public class PatientIdentityCardIssuanceService {

    private static final String OWNER_TYPE_PATIENT = "PATIENT";

    private final RestTemplate loadBalancedRestTemplate;

    @Value("${hospital.patient-identity-card.enabled:true}")
    private boolean enabled;

    @Value("${hospital.patient-identity-card.auto-issue-on-registration:true}")
    private boolean autoIssueOnRegistration;

    @Value("${hospital.patient-identity-card.card-product-id:a0000001-0001-4000-8000-000000000001}")
    private UUID cardProductId;

    @Value("${hospital.patient-identity-card.internal-service-key:easyops-internal-hospital-card-key}")
    private String internalServiceKey;

    public PatientIdentityCardIssuanceService(
            @Qualifier(LoadBalancedRestTemplateConfig.BEAN_NAME) RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

    /**
     * @param mrn used as {@code cardNumber} when issuing (human-readable; matches requirements “may equal MRN”).
     * @param organizationId forwarded as {@code X-Organization-Id} so RBAC checks in card-management resolve org-scoped permissions.
     */
    public PatientIdentityCardIssuanceResult issueOrResolveForNewPatient(UUID patientId, UUID actorUserId, String mrn, UUID organizationId) {
        if (!enabled) {
            return PatientIdentityCardIssuanceResult.builder()
                    .status("DISABLED")
                    .message("Patient identity card integration is disabled")
                    .build();
        }
        if (!autoIssueOnRegistration) {
            return PatientIdentityCardIssuanceResult.builder()
                    .status("SKIPPED")
                    .message("Auto-issue disabled; use manual card issue from Hospital Card Management")
                    .build();
        }

        try {
            CardSummary existing = null;
            try {
                existing = findExistingCard(patientId, actorUserId, organizationId);
            } catch (RestClientException searchEx) {
                // Search may fail with 403 when misconfigured; still attempt direct issue.
                log.warn("Patient identity card search failed for patient {}, attempting issue: {}",
                        patientId, searchEx.getMessage());
            }
            if (existing != null) {
                return PatientIdentityCardIssuanceResult.builder()
                        .status("ISSUED")
                        .cardId(existing.getId())
                        .cardNumber(existing.getCardNumber())
                        .message("Existing patient identity card")
                        .build();
            }

            CardSummary created = issueCard(patientId, actorUserId, mrn, organizationId);
            return PatientIdentityCardIssuanceResult.builder()
                    .status("ISSUED")
                    .cardId(created.getId())
                    .cardNumber(created.getCardNumber())
                    .build();
        } catch (RestClientException ex) {
            log.warn("Patient identity card issuance failed for patient {}: {}", patientId, ex.getMessage());
            return PatientIdentityCardIssuanceResult.builder()
                    .status("FAILED")
                    .message(friendlyCardServiceError(ex))
                    .build();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String friendlyCardServiceError(RestClientException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("Service Instance cannot be null")) {
            return "Hospital Card Management service is not available. The patient was saved; start hospital-card-management-service "
                    + "(e.g. via start-hospital-components.sh) and issue the card from Hospital → Cards, or set "
                    + "hospital.patient-identity-card.auto-issue-on-registration=false to skip auto-issue locally.";
        }
        return truncate(msg, 500);
    }

    public PatientIdentityCardIssuanceResult resolveExistingForPatient(UUID patientId, UUID actorUserId, UUID organizationId) {
        if (!enabled) {
            return PatientIdentityCardIssuanceResult.builder()
                    .status("DISABLED")
                    .message("Patient identity card integration is disabled")
                    .build();
        }
        try {
            CardSummary existing = findExistingCard(patientId, actorUserId, organizationId);
            if (existing == null) {
                return PatientIdentityCardIssuanceResult.builder()
                        .status("FAILED")
                        .message("No patient identity card exists for this patient")
                        .build();
            }
            return PatientIdentityCardIssuanceResult.builder()
                    .status("ISSUED")
                    .cardId(existing.getId())
                    .cardNumber(existing.getCardNumber())
                    .message("Existing patient identity card")
                    .build();
        } catch (RestClientException ex) {
            log.warn("Resolve patient identity card failed for patient {}: {}", patientId, ex.getMessage());
            return PatientIdentityCardIssuanceResult.builder()
                    .status("FAILED")
                    .message(truncate(ex.getMessage(), 500))
                    .build();
        }
    }

    public PatientIdentityCardIssuanceResult replaceForPatient(UUID patientId, UUID actorUserId, String reason, UUID organizationId) {
        if (!enabled) {
            return PatientIdentityCardIssuanceResult.builder()
                    .status("DISABLED")
                    .message("Patient identity card integration is disabled")
                    .build();
        }
        try {
            CardSummary existing = findExistingCard(patientId, actorUserId, organizationId);
            if (existing == null) {
                return PatientIdentityCardIssuanceResult.builder()
                        .status("FAILED")
                        .message("No patient identity card exists for replacement")
                        .build();
            }
            String replacePath = useInternalApi()
                    ? "/api/hospital-card-management/internal/cards/"
                    : "/api/hospital-card-management/cards/";
            String url = "http://hospital-card-management-service" + replacePath + existing.getId() + "/replace";
            Map<String, Object> body = new HashMap<>();
            if (reason != null && !reason.isBlank()) {
                body.put("reason", reason.trim());
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers(actorUserId, organizationId));
            ResponseEntity<CardSummary> response = loadBalancedRestTemplate.exchange(
                    url, HttpMethod.POST, entity, CardSummary.class);
            CardSummary replaced = response.getBody();
            if (replaced == null) {
                throw new IllegalStateException("Empty body from card replace");
            }
            return PatientIdentityCardIssuanceResult.builder()
                    .status("REPLACED")
                    .cardId(replaced.getId())
                    .cardNumber(replaced.getCardNumber())
                    .message("Patient identity card replaced")
                    .build();
        } catch (RestClientException ex) {
            log.warn("Replace patient identity card failed for patient {}: {}", patientId, ex.getMessage());
            return PatientIdentityCardIssuanceResult.builder()
                    .status("FAILED")
                    .message(truncate(ex.getMessage(), 500))
                    .build();
        }
    }

    private boolean useInternalApi() {
        return internalServiceKey != null && !internalServiceKey.isBlank();
    }

    private HttpHeaders headers(UUID actorUserId, UUID organizationId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (useInternalApi()) {
            h.set("X-Internal-Service-Key", internalServiceKey.trim());
        } else {
            h.set("X-User-Id", actorUserId.toString());
            if (organizationId != null) {
                h.set("X-Organization-Id", organizationId.toString());
            }
        }
        return h;
    }

    private String cardsBasePath() {
        return useInternalApi()
                ? "http://hospital-card-management-service/api/hospital-card-management/internal/cards"
                : "http://hospital-card-management-service/api/hospital-card-management/cards";
    }

    private CardSummary findExistingCard(UUID patientId, UUID actorUserId, UUID organizationId) {
        CardSummary active = findExistingCardByStatus(patientId, actorUserId, "ACTIVE", organizationId);
        if (active != null) {
            return active;
        }
        return findExistingCardByStatus(patientId, actorUserId, "ISSUED", organizationId);
    }

    private CardSummary findExistingCardByStatus(UUID patientId, UUID actorUserId, String status, UUID organizationId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(cardsBasePath() + "/search")
                .queryParam("ownerReferenceId", patientId.toString())
                .queryParam("ownerType", OWNER_TYPE_PATIENT)
                .queryParam("cardProductId", cardProductId.toString())
                .queryParam("status", status)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(headers(actorUserId, organizationId));
        ResponseEntity<PagedCardsResponse> response = loadBalancedRestTemplate.exchange(
                url, HttpMethod.GET, entity, PagedCardsResponse.class);

        PagedCardsResponse body = response.getBody();
        if (body == null || body.getContent() == null || body.getContent().isEmpty()) {
            return null;
        }
        return body.getContent().get(0);
    }

    private CardSummary issueCard(UUID patientId, UUID actorUserId, String mrn, UUID organizationId) {
        String url = cardsBasePath();

        Map<String, Object> body = new HashMap<>();
        body.put("cardProductId", cardProductId.toString());
        body.put("ownerType", OWNER_TYPE_PATIENT);
        body.put("ownerReferenceId", patientId.toString());
        if (mrn != null && !mrn.isBlank()) {
            body.put("cardNumber", mrn);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers(actorUserId, organizationId));
        ResponseEntity<CardSummary> response = loadBalancedRestTemplate.exchange(
                url, HttpMethod.POST, entity, CardSummary.class);
        CardSummary created = response.getBody();
        if (created == null) {
            throw new IllegalStateException("Empty body from card issue");
        }
        return created;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PagedCardsResponse {
        @JsonProperty("content")
        private List<CardSummary> content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CardSummary {
        private UUID id;
        private String cardNumber;
    }
}
