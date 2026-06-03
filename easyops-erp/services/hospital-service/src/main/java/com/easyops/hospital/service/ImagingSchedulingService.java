package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingOrder;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for scheduling imaging orders.
 * This service integrates with a scheduling system to book appointments for imaging studies.
 * If no scheduling service is available, it can work in standalone mode.
 */
@Service
@Slf4j
public class ImagingSchedulingService {
    
    private final RestTemplate restTemplate;
    
    public ImagingSchedulingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Value("${imaging.scheduling.enabled:false}")
    private boolean schedulingEnabled;
    
    @Value("${imaging.scheduling.endpoint:}")
    private String schedulingEndpoint;
    
    @Value("${imaging.scheduling.standalone-mode:true}")
    private boolean standaloneMode;
    
    /**
     * Schedule an imaging order
     * 
     * @param imagingOrder The imaging order to schedule
     * @param requestedDate The requested date/time for the study
     * @return Scheduling result with appointment details
     */
    public SchedulingResult scheduleOrder(ImagingOrder imagingOrder, LocalDateTime requestedDate) {
        log.info("Scheduling imaging order {} for date: {}", imagingOrder.getOrderId(), requestedDate);
        
        // If scheduling is disabled or in standalone mode, just validate and return
        if (!schedulingEnabled || standaloneMode) {
            log.debug("Scheduling service disabled or in standalone mode. Validating schedule locally.");
            
            // Validate requested date
            if (requestedDate == null) {
                return SchedulingResult.builder()
                    .success(false)
                    .status("VALIDATION_ERROR")
                    .message("Requested date is required")
                    .scheduledAt(LocalDateTime.now())
                    .build();
            }
            
            if (requestedDate.isBefore(LocalDateTime.now())) {
                return SchedulingResult.builder()
                    .success(false)
                    .status("VALIDATION_ERROR")
                    .message("Requested date cannot be in the past")
                    .scheduledAt(LocalDateTime.now())
                    .build();
            }
            
            // In standalone mode, accept the requested date
            return SchedulingResult.builder()
                .success(true)
                .status("SCHEDULED")
                .message("Scheduled in standalone mode")
                .scheduledDate(requestedDate)
                .scheduledAt(LocalDateTime.now())
                .appointmentId(generateAppointmentId(imagingOrder))
                .build();
        }
        
        // If scheduling service is enabled, call external service
        if (schedulingEndpoint == null || schedulingEndpoint.isEmpty()) {
            log.error("Scheduling endpoint is not configured. Cannot schedule order {}", imagingOrder.getOrderId());
            return SchedulingResult.builder()
                .success(false)
                .status("CONFIGURATION_ERROR")
                .message("Scheduling endpoint is not configured")
                .scheduledAt(LocalDateTime.now())
                .build();
        }
        
        try {
            Map<String, Object> request = buildSchedulingRequest(imagingOrder, requestedDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Order-Id", imagingOrder.getOrderId().toString());
            headers.set("X-Order-Number", imagingOrder.getOrderNumber());
            
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);
            
            log.debug("Calling scheduling service for order {}: {}", imagingOrder.getOrderId(), schedulingEndpoint);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                schedulingEndpoint + "/appointments",
                HttpMethod.POST,
                httpRequest,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Extract appointment details from response
                String appointmentId = (String) responseBody.get("appointmentId");
                Object scheduledDateObj = responseBody.get("scheduledDate");
                LocalDateTime scheduledDate = null;
                
                if (scheduledDateObj != null) {
                    if (scheduledDateObj instanceof String) {
                        scheduledDate = LocalDateTime.parse((String) scheduledDateObj);
                    } else if (scheduledDateObj instanceof LocalDateTime) {
                        scheduledDate = (LocalDateTime) scheduledDateObj;
                    }
                }
                
                log.info("Successfully scheduled imaging order {} with appointment ID: {}", 
                    imagingOrder.getOrderId(), appointmentId);
                
                return SchedulingResult.builder()
                    .success(true)
                    .status("SCHEDULED")
                    .message("Successfully scheduled via scheduling service")
                    .scheduledDate(scheduledDate != null ? scheduledDate : requestedDate)
                    .scheduledAt(LocalDateTime.now())
                    .appointmentId(appointmentId)
                    .responseCode(response.getStatusCode().value())
                    .build();
            } else {
                log.error("Scheduling service returned error for order {}: {}", 
                    imagingOrder.getOrderId(), response.getStatusCode());
                return SchedulingResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("Scheduling service returned status: " + response.getStatusCode())
                    .scheduledAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during scheduling: {}", e.getMessage(), e);
            return SchedulingResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .scheduledAt(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * Reschedule an imaging order
     * 
     * @param imagingOrder The imaging order to reschedule
     * @param newScheduledDate The new requested date/time
     * @return Scheduling result
     */
    public SchedulingResult rescheduleOrder(ImagingOrder imagingOrder, LocalDateTime newScheduledDate) {
        log.info("Rescheduling imaging order {} to date: {}", imagingOrder.getOrderId(), newScheduledDate);
        
        // If scheduling is disabled or in standalone mode, just validate and return
        if (!schedulingEnabled || standaloneMode) {
            if (newScheduledDate == null || newScheduledDate.isBefore(LocalDateTime.now())) {
                return SchedulingResult.builder()
                    .success(false)
                    .status("VALIDATION_ERROR")
                    .message("New scheduled date is invalid")
                    .scheduledAt(LocalDateTime.now())
                    .build();
            }
            
            return SchedulingResult.builder()
                .success(true)
                .status("RESCHEDULED")
                .message("Rescheduled in standalone mode")
                .scheduledDate(newScheduledDate)
                .scheduledAt(LocalDateTime.now())
                .appointmentId(imagingOrder.getOrderNumber() + "-RESCHED")
                .build();
        }
        
        // Call external scheduling service for rescheduling
        if (schedulingEndpoint == null || schedulingEndpoint.isEmpty()) {
            return SchedulingResult.builder()
                .success(false)
                .status("CONFIGURATION_ERROR")
                .message("Scheduling endpoint is not configured")
                .scheduledAt(LocalDateTime.now())
                .build();
        }
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", imagingOrder.getOrderId().toString());
            request.put("orderNumber", imagingOrder.getOrderNumber());
            request.put("newScheduledDate", newScheduledDate.toString());
            request.put("appointmentId", imagingOrder.getOrderNumber()); // Use order number as appointment ID
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                schedulingEndpoint + "/appointments/reschedule",
                HttpMethod.PUT,
                httpRequest,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully rescheduled imaging order {}", imagingOrder.getOrderId());
                return SchedulingResult.builder()
                    .success(true)
                    .status("RESCHEDULED")
                    .message("Successfully rescheduled via scheduling service")
                    .scheduledDate(newScheduledDate)
                    .scheduledAt(LocalDateTime.now())
                    .appointmentId(imagingOrder.getOrderNumber() + "-RESCHED")
                    .responseCode(response.getStatusCode().value())
                    .build();
            } else {
                return SchedulingResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("Rescheduling service returned status: " + response.getStatusCode())
                    .scheduledAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during rescheduling: {}", e.getMessage(), e);
            return SchedulingResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .scheduledAt(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * Cancel a scheduled appointment
     * 
     * @param imagingOrder The imaging order to cancel
     * @return Cancellation result
     */
    public SchedulingResult cancelAppointment(ImagingOrder imagingOrder) {
        log.info("Cancelling appointment for imaging order {}", imagingOrder.getOrderId());
        
        if (!schedulingEnabled || standaloneMode) {
            return SchedulingResult.builder()
                .success(true)
                .status("CANCELLED")
                .message("Cancelled in standalone mode")
                .scheduledAt(LocalDateTime.now())
                .build();
        }
        
        if (schedulingEndpoint == null || schedulingEndpoint.isEmpty()) {
            return SchedulingResult.builder()
                .success(false)
                .status("CONFIGURATION_ERROR")
                .message("Scheduling endpoint is not configured")
                .scheduledAt(LocalDateTime.now())
                .build();
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Order-Id", imagingOrder.getOrderId().toString());
            headers.set("X-Order-Number", imagingOrder.getOrderNumber());
            
            HttpEntity<Void> httpRequest = new HttpEntity<>(headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                schedulingEndpoint + "/appointments/" + imagingOrder.getOrderNumber() + "/cancel",
                HttpMethod.DELETE,
                httpRequest,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully cancelled appointment for imaging order {}", imagingOrder.getOrderId());
                return SchedulingResult.builder()
                    .success(true)
                    .status("CANCELLED")
                    .message("Successfully cancelled via scheduling service")
                    .scheduledAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .build();
            } else {
                return SchedulingResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("Cancellation service returned status: " + response.getStatusCode())
                    .scheduledAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during appointment cancellation: {}", e.getMessage(), e);
            return SchedulingResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .scheduledAt(LocalDateTime.now())
                .build();
        }
    }
    
    // ========== Helper Methods ==========
    
    private Map<String, Object> buildSchedulingRequest(ImagingOrder imagingOrder, LocalDateTime requestedDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("orderId", imagingOrder.getOrderId().toString());
        request.put("orderNumber", imagingOrder.getOrderNumber());
        request.put("patientId", imagingOrder.getPatient().getPatientId().toString());
        request.put("patientName", PatientDisplayName.of(imagingOrder.getPatient()));
        request.put("studyType", imagingOrder.getStudyType());
        request.put("modality", imagingOrder.getStudyModality().name());
        request.put("studyDescription", imagingOrder.getStudyDescription());
        request.put("requestedDate", requestedDate.toString());
        request.put("duration", estimateStudyDuration(imagingOrder));
        request.put("facilityId", imagingOrder.getRadiologyFacilityId() != null ? 
            imagingOrder.getRadiologyFacilityId().toString() : null);
        request.put("facilityName", imagingOrder.getRadiologyFacilityName());
        request.put("priority", imagingOrder.getPriority().name());
        request.put("contrastRequired", imagingOrder.getContrastRequired());
        request.put("sedationRequired", imagingOrder.getSedationRequired());
        return request;
    }
    
    private int estimateStudyDuration(ImagingOrder imagingOrder) {
        // Estimate duration in minutes based on modality
        switch (imagingOrder.getStudyModality()) {
            case XRAY:
                return 15;
            case CT:
                return imagingOrder.getContrastRequired() ? 45 : 30;
            case MRI:
                return imagingOrder.getContrastRequired() ? 60 : 45;
            case ULTRASOUND:
                return 30;
            case MAMMOGRAPHY:
                return 20;
            default:
                return 30;
        }
    }
    
    private String generateAppointmentId(ImagingOrder imagingOrder) {
        return "APT-" + imagingOrder.getOrderNumber();
    }
    
    // ========== DTOs ==========
    
    @lombok.Data
    @lombok.Builder
    public static class SchedulingResult {
        private boolean success;
        private String status; // SCHEDULED, RESCHEDULED, CANCELLED, FAILED, ERROR, VALIDATION_ERROR, CONFIGURATION_ERROR, HTTP_ERROR, TRANSMISSION_ERROR
        private String message;
        private LocalDateTime scheduledDate;
        private LocalDateTime scheduledAt;
        private String appointmentId;
        private Integer responseCode;
    }
}
