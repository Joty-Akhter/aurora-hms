package com.easyops.hospital.integration.scheduling;

import com.easyops.hospital.config.LoadBalancedRestTemplateConfig;
import com.easyops.hospital.dto.DoctorAppointmentSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * HTTP client for hospital-scheduling-service.
 * Syncs doctor appointment schedule settings (working hours + blackouts) whenever a doctor is saved.
 * All calls are best-effort: a scheduling sync failure logs a warning but does NOT roll back the doctor save.
 */
@Service
@Slf4j
public class SchedulingServiceClient {

    private static final String RESOURCE_TYPE_DOCTOR = "DOCTOR";

    /** dayOfWeek mapping: scheduling service uses 0=Sun..6=Sat */
    private static final Map<String, Integer> DAY_OF_WEEK;
    static {
        DAY_OF_WEEK = new LinkedHashMap<>();
        DAY_OF_WEEK.put("sunday",    0);
        DAY_OF_WEEK.put("monday",    1);
        DAY_OF_WEEK.put("tuesday",   2);
        DAY_OF_WEEK.put("wednesday", 3);
        DAY_OF_WEEK.put("thursday",  4);
        DAY_OF_WEEK.put("friday",    5);
        DAY_OF_WEEK.put("saturday",  6);
    }

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate plainRestTemplate;

    @Value("${services.scheduling.base-url:http://localhost:8093}")
    private String schedulingBaseUrl;

    public SchedulingServiceClient(
            @Qualifier(LoadBalancedRestTemplateConfig.BEAN_NAME) RestTemplate loadBalancedRestTemplate,
            @Qualifier("restTemplate") RestTemplate plainRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
    }

    /**
     * Returns the correct RestTemplate for the configured scheduling base URL.
     * Eureka service names (e.g. http://hospital-scheduling-service) require the load-balanced
     * RestTemplate. Direct host:port URLs (localhost, IP addresses) use the plain one.
     */
    private RestTemplate rt() {
        if (schedulingBaseUrl.startsWith("http://localhost")
                || schedulingBaseUrl.startsWith("https://localhost")
                || schedulingBaseUrl.startsWith("http://127.")
                || schedulingBaseUrl.startsWith("https://127.")) {
            return plainRestTemplate;
        }
        return loadBalancedRestTemplate;
    }

    // ===== Public API =====

    /**
     * Find or create a scheduling resource for this doctor.
     * @return resourceId (UUID string), or null if the call failed
     */
    public String findOrCreateResource(String doctorId, String doctorName, UUID departmentId, String userId, UUID organizationId) {
        try {
            // Search existing resources by externalReferenceId across pages.
            // This avoids false misses when there are >200 doctor resources.
            int page = 0;
            int size = 200;
            while (true) {
                String searchUrl = UriComponentsBuilder.fromHttpUrl(schedulingBaseUrl)
                        .path("/api/hospital-scheduling/resources")
                        .queryParam("resourceType", RESOURCE_TYPE_DOCTOR)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .toUriString();

                ResponseEntity<Map<String, Object>> resp = rt().exchange(
                        searchUrl, HttpMethod.GET,
                        buildHeaders(userId, organizationId),
                        new ParameterizedTypeReference<>() {});

                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    break;
                }
                List<?> content = (List<?>) resp.getBody().get("content");
                if (content == null || content.isEmpty()) {
                    break;
                }
                for (Object item : content) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> r = (Map<String, Object>) item;
                    if (doctorId.equals(r.get("externalReferenceId"))) {
                        String id = (String) r.get("id");
                        log.debug("Found existing scheduling resource {} for doctor {}", id, doctorId);
                        return id;
                    }
                }
                Object totalPagesObj = resp.getBody().get("totalPages");
                int totalPages = totalPagesObj instanceof Number ? ((Number) totalPagesObj).intValue() : 1;
                page++;
                if (page >= totalPages) {
                    break;
                }
            }

            // Not found — create
            return createResource(doctorId, doctorName, departmentId, userId, organizationId);

        } catch (RestClientException e) {
            log.warn("Failed to find/create scheduling resource for doctor {}: {}", doctorId, e.getMessage());
            return null;
        }
    }

    /**
     * Sync working hours from doctor's appointment slots to the scheduling resource.
     * Each slot defines a time range and the days it applies to.
     * Replaces all existing working hours for the resource.
     */
    public void syncWorkingHours(String resourceId, List<DoctorAppointmentSlot> appointmentSlots, String userId, UUID organizationId) {
        if (appointmentSlots == null || appointmentSlots.isEmpty()) return;

        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Map<String, Object>> entryByKey = new LinkedHashMap<>();
        for (DoctorAppointmentSlot slot : appointmentSlots) {
            if (slot.getStartTime() == null || slot.getEndTime() == null) continue;
            if (slot.getDays() == null || slot.getDays().isEmpty()) continue;
            int maxPatients = slot.getMaxPatients() != null && slot.getMaxPatients() > 0 ? slot.getMaxPatients() : 10;
            Integer slotDurationMinutes = null;
            try {
                LocalTime st = LocalTime.parse(slot.getStartTime().trim());
                LocalTime et = LocalTime.parse(slot.getEndTime().trim());
                long minutes = ChronoUnit.MINUTES.between(st, et);
                if (minutes <= 0) {
                    // Overnight session (e.g. 22:00–01:00)
                    minutes += 24 * 60L;
                }
                if (minutes > 0) {
                    // Ceil so slot duration is not truncated to 1 min (which would over-generate slots).
                    slotDurationMinutes = (int) Math.max(1, (minutes + maxPatients - 1) / maxPatients);
                }
            } catch (DateTimeParseException ex) {
                log.warn("Could not parse doctor slot times {} – {}: {}", slot.getStartTime(), slot.getEndTime(), ex.getMessage());
            }
            for (String day : slot.getDays()) {
                Integer dow = DAY_OF_WEEK.get(day.toLowerCase());
                if (dow == null) continue;
                String key = dow + "|" + slot.getStartTime().trim() + "|" + slot.getEndTime().trim();
                Map<String, Object> entry = entryByKey.get(key);
                if (entry == null) {
                    entry = new LinkedHashMap<>();
                    entry.put("dayOfWeek", dow);
                    entry.put("startTime", slot.getStartTime());
                    entry.put("endTime", slot.getEndTime());
                    entry.put("slotsPerInterval", 1);
                    entry.put("maxSlotsPerSegment", maxPatients);
                    if (slotDurationMinutes != null) {
                        entry.put("slotDurationMinutes", slotDurationMinutes);
                    }
                    entryByKey.put(key, entry);
                } else {
                    int existingMax = ((Number) entry.getOrDefault("maxSlotsPerSegment", maxPatients)).intValue();
                    if (maxPatients > existingMax) {
                        entry.put("maxSlotsPerSegment", maxPatients);
                    }
                    if (slotDurationMinutes != null) {
                        int existingDur = ((Number) entry.getOrDefault("slotDurationMinutes", slotDurationMinutes)).intValue();
                        if (slotDurationMinutes < existingDur) {
                            entry.put("slotDurationMinutes", slotDurationMinutes);
                        }
                    }
                }
            }
        }
        entries.addAll(entryByKey.values());

        if (entries.isEmpty()) {
            log.debug("No working-hour entries to sync for resource {}", resourceId);
            return;
        }

        try {
            String url = schedulingBaseUrl + "/api/hospital-scheduling/resources/" + resourceId + "/working-hours";
            Map<String, Object> body = Map.of("entries", entries);

            rt().exchange(
                    url, HttpMethod.POST,
                    buildJsonEntity(body, userId, organizationId),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            log.info("Synced {} working-hour entries for scheduling resource {}", entries.size(), resourceId);
        } catch (RestClientException e) {
            log.warn("Failed to sync working hours for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Sync off-days (blackouts) for the scheduling resource.
     * Fetches existing blackouts, removes ones no longer in the list, adds new ones.
     */
    public void syncBlackouts(String resourceId, List<String> offDays, String userId, UUID organizationId) {
        try {
            UUID resourceUuid = UUID.fromString(resourceId);

            // Fetch current blackouts for this resource
            String listUrl = UriComponentsBuilder.fromHttpUrl(schedulingBaseUrl)
                    .path("/api/hospital-scheduling/blackouts")
                    .queryParam("resourceId", resourceId)
                    .queryParam("size", 500)
                    .toUriString();

            ResponseEntity<Map<String, Object>> resp = rt().exchange(
                    listUrl, HttpMethod.GET,
                    buildHeaders(userId, organizationId),
                    new ParameterizedTypeReference<>() {});

            Set<String> desired = new HashSet<>(offDays != null ? offDays : List.of());
            Set<String> existing = new HashSet<>();
            Map<String, String> existingIdByDate = new HashMap<>(); // date -> blackout UUID

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<?> content = (List<?>) resp.getBody().get("content");
                if (content != null) {
                    for (Object item : content) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> b = (Map<String, Object>) item;
                        String date = (String) b.get("blackoutDate");
                        String id   = (String) b.get("id");
                        if (date != null && id != null) {
                            existing.add(date);
                            existingIdByDate.put(date, id);
                        }
                    }
                }
            }

            // Delete removed dates
            for (String date : existing) {
                if (!desired.contains(date)) {
                    deleteBlackout(existingIdByDate.get(date), userId, organizationId);
                }
            }

            // Create new dates
            for (String date : desired) {
                if (!existing.contains(date)) {
                    createBlackout(resourceUuid, date, userId, organizationId);
                }
            }

        } catch (RestClientException e) {
            log.warn("Failed to sync blackouts for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Fetch available appointment slots for a scheduling resource.
     * Returns an empty list when the scheduling service is unreachable or returns a non-success status.
     */
    public List<Map<String, Object>> getAvailability(
            String resourceId,
            LocalDate fromDate,
            LocalDate toDate,
            String userId,
            UUID organizationId) {
        try {
            return fetchAvailability(resourceId, fromDate, toDate, userId, organizationId);
        } catch (RestClientException e) {
            log.warn("Failed to fetch availability for resource {}: {}", resourceId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Same as {@link #getAvailability} but propagates failures to the caller (for public web booking).
     */
    public List<Map<String, Object>> getAvailabilityRequired(
            String resourceId,
            LocalDate fromDate,
            LocalDate toDate,
            String userId,
            UUID organizationId) {
        return fetchAvailability(resourceId, fromDate, toDate, userId, organizationId);
    }

    private List<Map<String, Object>> fetchAvailability(
            String resourceId,
            LocalDate fromDate,
            LocalDate toDate,
            String userId,
            UUID organizationId) {
        String url = UriComponentsBuilder.fromHttpUrl(schedulingBaseUrl)
                .path("/api/hospital-scheduling/availability")
                .queryParam("resourceId", resourceId)
                .queryParam("fromDate", fromDate.toString())
                .queryParam("toDate", toDate.toString())
                .toUriString();

        ResponseEntity<List<Map<String, Object>>> resp = rt().exchange(
                url,
                HttpMethod.GET,
                buildHeaders(userId, organizationId),
                new ParameterizedTypeReference<>() {});

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            return resp.getBody();
        }
        throw new RestClientException("Scheduling service returned " + resp.getStatusCode());
    }

    /**
     * Book an appointment via the scheduling service.
     */
    public Map<String, Object> createAppointment(
            Map<String, Object> body,
            String userId,
            UUID organizationId) {
        String url = schedulingBaseUrl + "/api/hospital-scheduling/appointments";
        ResponseEntity<Map<String, Object>> resp = rt().exchange(
                url,
                HttpMethod.POST,
                buildJsonEntity(body, userId, organizationId),
                new ParameterizedTypeReference<>() {});
        if (resp.getStatusCode().is2xxSuccessful()) {
            return resp.getBody();
        }
        throw new RestClientException("Scheduling service returned " + resp.getStatusCode());
    }

    // ===== Private helpers =====

    private String createResource(String doctorId, String doctorName, UUID departmentId, String userId, UUID organizationId) {
        String url = schedulingBaseUrl + "/api/hospital-scheduling/resources";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resourceType", RESOURCE_TYPE_DOCTOR);
        body.put("externalReferenceId", doctorId);
        body.put("name", doctorName);
        body.put("status", "ACTIVE");
        if (departmentId != null) {
            body.put("departmentId", departmentId.toString());
        }

        ResponseEntity<Map<String, Object>> resp = rt().exchange(
                url, HttpMethod.POST,
                buildJsonEntity(body, userId, organizationId),
                new ParameterizedTypeReference<>() {});

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String newId = (String) resp.getBody().get("id");
            log.info("Created scheduling resource {} for doctor {}", newId, doctorId);
            return newId;
        }
        return null;
    }

    private void deleteBlackout(String blackoutId, String userId, UUID organizationId) {
        try {
            String url = schedulingBaseUrl + "/api/hospital-scheduling/blackouts/" + blackoutId;
            rt().exchange(url, HttpMethod.DELETE, buildHeaders(userId, organizationId), Void.class);
            log.debug("Deleted blackout {}", blackoutId);
        } catch (RestClientException e) {
            log.warn("Failed to delete blackout {}: {}", blackoutId, e.getMessage());
        }
    }

    private void createBlackout(UUID resourceId, String date, String userId, UUID organizationId) {
        try {
            String url = schedulingBaseUrl + "/api/hospital-scheduling/blackouts";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("resourceId", resourceId.toString());
            body.put("blackoutDate", date);
            body.put("reason", "Doctor off day");

            rt().exchange(url, HttpMethod.POST,
                    buildJsonEntity(body, userId, organizationId),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            log.debug("Created blackout for resource {} on {}", resourceId, date);
        } catch (RestClientException e) {
            log.warn("Failed to create blackout for resource {} on {}: {}", resourceId, date, e.getMessage());
        }
    }


    private HttpEntity<Void> buildHeaders(String userId, UUID organizationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId != null ? userId : "system");
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> buildJsonEntity(T body, String userId, UUID organizationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId != null ? userId : "system");
        if (organizationId != null) {
            headers.set("X-Organization-Id", organizationId.toString());
        }
        return new HttpEntity<>(body, headers);
    }
}
