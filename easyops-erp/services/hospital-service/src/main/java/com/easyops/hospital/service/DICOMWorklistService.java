package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingOrder;
import com.easyops.hospital.util.PatientNameInterop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for DICOM Modality Worklist (MWL) integration.
 * DICOM MWL allows imaging modalities (CT, MRI, X-ray machines) to query for scheduled studies.
 * This service provides worklist entries in DICOM MWL format (C-FIND response).
 */
@Service
@Slf4j
public class DICOMWorklistService {
    
    private final RestTemplate restTemplate;
    
    public DICOMWorklistService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Value("${dicom.worklist.enabled:false}")
    private boolean worklistEnabled;
    
    @Value("${dicom.worklist.endpoint:}")
    private String worklistEndpoint;
    
    @Value("${dicom.worklist.ae-title:EHR-MWL}")
    private String applicationEntityTitle;
    
    /**
     * Generate DICOM MWL (Modality Worklist) entry for an imaging order
     * This creates a worklist entry that can be queried by DICOM modalities
     * 
     * @param imagingOrder The imaging order to create worklist entry for
     * @return Worklist entry in DICOM format (JSON representation)
     */
    public WorklistEntry generateWorklistEntry(ImagingOrder imagingOrder) {
        log.debug("Generating DICOM MWL entry for imaging order: {}", imagingOrder.getOrderId());
        
        // Generate accession number if not present
        String accessionNumber = generateAccessionNumber(imagingOrder);
        
        // Build DICOM MWL entry
        WorklistEntry entry = WorklistEntry.builder()
            .scheduledProcedureStepSequence(createScheduledProcedureStepSequence(imagingOrder, accessionNumber))
            .patientName(formatPatientName(imagingOrder.getPatient()))
            .patientId(imagingOrder.getPatient().getMrn())
            .patientBirthDate(formatDate(imagingOrder.getPatient().getDateOfBirth()))
            .patientSex(imagingOrder.getPatient().getGender() != null ? 
                imagingOrder.getPatient().getGender().name() : null)
            .referringPhysicianName(imagingOrder.getOrderingProviderName())
            .studyInstanceUID(generateStudyInstanceUID(imagingOrder))
            .studyDate(formatDate(imagingOrder.getScheduledDate()))
            .studyTime(formatTime(imagingOrder.getScheduledTime()))
            .accessionNumber(accessionNumber)
            .modality(imagingOrder.getStudyModality().name())
            .requestedProcedureDescription(imagingOrder.getStudyDescription())
            .requestedProcedureId(imagingOrder.getOrderNumber())
            .requestedProcedureCodeSequence(createRequestedProcedureCodeSequence(imagingOrder))
            .build();
        
        log.debug("Generated DICOM MWL entry for order: {}", imagingOrder.getOrderId());
        return entry;
    }
    
    /**
     * Submit worklist entry to DICOM worklist server
     * 
     * @param imagingOrder The imaging order to submit
     * @return Submission result
     */
    public WorklistSubmissionResult submitWorklistEntry(ImagingOrder imagingOrder) {
        if (!worklistEnabled) {
            log.warn("DICOM worklist is disabled. Worklist entry for order {} will not be submitted.", 
                imagingOrder.getOrderId());
            return WorklistSubmissionResult.builder()
                .success(false)
                .status("DISABLED")
                .message("DICOM worklist is disabled in configuration")
                .submittedAt(LocalDateTime.now())
                .build();
        }
        
        if (worklistEndpoint == null || worklistEndpoint.isEmpty()) {
            log.error("DICOM worklist endpoint is not configured. Cannot submit worklist entry for order {}", 
                imagingOrder.getOrderId());
            return WorklistSubmissionResult.builder()
                .success(false)
                .status("CONFIGURATION_ERROR")
                .message("DICOM worklist endpoint is not configured")
                .submittedAt(LocalDateTime.now())
                .build();
        }
        
        try {
            WorklistEntry entry = generateWorklistEntry(imagingOrder);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-AE-Title", applicationEntityTitle);
            headers.set("X-Order-Id", imagingOrder.getOrderId().toString());
            headers.set("X-Order-Number", imagingOrder.getOrderNumber());
            
            HttpEntity<WorklistEntry> request = new HttpEntity<>(entry, headers);
            
            log.info("Submitting DICOM MWL entry for order {} to endpoint: {}", 
                imagingOrder.getOrderId(), worklistEndpoint);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                worklistEndpoint,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully submitted DICOM MWL entry for order {}", imagingOrder.getOrderId());
                return WorklistSubmissionResult.builder()
                    .success(true)
                    .status("SUBMITTED")
                    .message("Successfully submitted to DICOM worklist")
                    .submittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .worklistEntry(entry)
                    .build();
            } else {
                log.error("Failed to submit DICOM MWL entry for order {}: HTTP {}", 
                    imagingOrder.getOrderId(), response.getStatusCode());
                return WorklistSubmissionResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("DICOM worklist returned status: " + response.getStatusCode())
                    .submittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during DICOM worklist submission: {}", e.getMessage(), e);
            return WorklistSubmissionResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .submittedAt(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * Query worklist entries for a modality
     * 
     * @param modality The modality to query for (CT, MRI, XRAY, etc.)
     * @param scheduledDate The scheduled date to query
     * @return List of worklist entries
     */
    public List<WorklistEntry> queryWorklist(String modality, LocalDateTime scheduledDate) {
        if (!worklistEnabled) {
            log.warn("DICOM worklist is disabled. Cannot query worklist.");
            return Collections.emptyList();
        }
        
        if (worklistEndpoint == null || worklistEndpoint.isEmpty()) {
            log.error("DICOM worklist endpoint is not configured. Cannot query worklist.");
            return Collections.emptyList();
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-AE-Title", applicationEntityTitle);
            headers.set("X-Modality", modality);
            if (scheduledDate != null) {
                headers.set("X-Scheduled-Date", formatDate(scheduledDate));
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String queryUrl = worklistEndpoint + "/query";
            
            log.debug("Querying DICOM worklist for modality: {}, date: {}", modality, scheduledDate);
            
            ResponseEntity<List<WorklistEntry>> response = restTemplate.exchange(
                queryUrl,
                HttpMethod.GET,
                request,
                new org.springframework.core.ParameterizedTypeReference<List<WorklistEntry>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Retrieved {} worklist entries for modality: {}", 
                    response.getBody().size(), modality);
                return response.getBody();
            } else {
                log.warn("No worklist entries found for modality: {}", modality);
                return Collections.emptyList();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during DICOM worklist query: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    // ========== Helper Methods ==========
    
    private ScheduledProcedureStepSequence createScheduledProcedureStepSequence(
            ImagingOrder imagingOrder, String accessionNumber) {
        return ScheduledProcedureStepSequence.builder()
            .modality(imagingOrder.getStudyModality().name())
            .scheduledStationAeTitle(applicationEntityTitle)
            .scheduledProcedureStepStartDate(formatDate(imagingOrder.getScheduledDate()))
            .scheduledProcedureStepStartTime(formatTime(imagingOrder.getScheduledTime()))
            .scheduledProcedureStepDescription(imagingOrder.getStudyDescription())
            .scheduledProcedureStepId(imagingOrder.getOrderNumber())
            .scheduledPerformingPhysicianName(imagingOrder.getOrderingProviderName())
            .build();
    }
    
    private RequestedProcedureCodeSequence createRequestedProcedureCodeSequence(ImagingOrder imagingOrder) {
        return RequestedProcedureCodeSequence.builder()
            .codeValue(imagingOrder.getCptCode())
            .codeMeaning(imagingOrder.getStudyDescription())
            .codingSchemeDesignator("CPT")
            .build();
    }
    
    private String formatPatientName(com.easyops.hospital.entity.Patient patient) {
        String[] n = PatientNameInterop.splitFullName(patient.getFullName());
        return String.format("%s^%s", n[2], n[0]);
    }
    
    private String formatDate(java.time.LocalDate date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    private String formatTime(java.time.LocalTime time) {
        if (time == null) return null;
        return time.format(DateTimeFormatter.ofPattern("HHmmss"));
    }
    
    private String generateAccessionNumber(ImagingOrder imagingOrder) {
        // Generate accession number: ACC + timestamp + order number suffix
        String prefix = "ACC";
        long timestamp = System.currentTimeMillis();
        String suffix = imagingOrder.getOrderNumber().substring(
            Math.max(0, imagingOrder.getOrderNumber().length() - 6));
        return prefix + String.valueOf(timestamp).substring(7) + suffix;
    }
    
    private String generateStudyInstanceUID(ImagingOrder imagingOrder) {
        // Generate DICOM Study Instance UID: 1.2.840.10008.5.1.4.1.1.2 (EHR) + timestamp + order ID
        String root = "1.2.840.10008.5.1.4.1.1.2";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String orderIdSuffix = imagingOrder.getOrderId().toString().replace("-", "");
        return root + "." + timestamp + "." + orderIdSuffix.substring(0, Math.min(16, orderIdSuffix.length()));
    }
    
    // ========== DTOs ==========
    
    @lombok.Data
    @lombok.Builder
    public static class WorklistEntry {
        private ScheduledProcedureStepSequence scheduledProcedureStepSequence;
        private String patientName;
        private String patientId;
        private String patientBirthDate;
        private String patientSex;
        private String referringPhysicianName;
        private String studyInstanceUID;
        private String studyDate;
        private String studyTime;
        private String accessionNumber;
        private String modality;
        private String requestedProcedureDescription;
        private String requestedProcedureId;
        private RequestedProcedureCodeSequence requestedProcedureCodeSequence;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ScheduledProcedureStepSequence {
        private String modality;
        private String scheduledStationAeTitle;
        private String scheduledProcedureStepStartDate;
        private String scheduledProcedureStepStartTime;
        private String scheduledProcedureStepDescription;
        private String scheduledProcedureStepId;
        private String scheduledPerformingPhysicianName;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RequestedProcedureCodeSequence {
        private String codeValue;
        private String codeMeaning;
        private String codingSchemeDesignator;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WorklistSubmissionResult {
        private boolean success;
        private String status; // SUBMITTED, FAILED, ERROR, DISABLED, CONFIGURATION_ERROR, HTTP_ERROR, TRANSMISSION_ERROR
        private String message;
        private LocalDateTime submittedAt;
        private Integer responseCode;
        private WorklistEntry worklistEntry;
    }
}
