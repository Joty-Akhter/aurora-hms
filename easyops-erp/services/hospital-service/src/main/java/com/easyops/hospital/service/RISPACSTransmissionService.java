package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for transmitting imaging orders to RIS/PACS (Radiology Information System / Picture Archiving and Communication System)
 * via HL7 V2 ORM messages or HL7 FHIR ServiceRequest resources.
 */
@Service
@Slf4j
public class RISPACSTransmissionService {
    
    private final HL7V2OrmMessageGenerator hl7V2OrmMessageGenerator;
    private final HL7FhirServiceRequestGenerator hl7FhirServiceRequestGenerator;
    private final RestTemplate restTemplate;
    
    public RISPACSTransmissionService(
            HL7V2OrmMessageGenerator hl7V2OrmMessageGenerator,
            HL7FhirServiceRequestGenerator hl7FhirServiceRequestGenerator,
            RestTemplate restTemplate) {
        this.hl7V2OrmMessageGenerator = hl7V2OrmMessageGenerator;
        this.hl7FhirServiceRequestGenerator = hl7FhirServiceRequestGenerator;
        this.restTemplate = restTemplate;
    }
    
    @Value("${ris.pacs.transmission.enabled:false}")
    private boolean transmissionEnabled;
    
    @Value("${ris.pacs.transmission.endpoint:}")
    private String risPacsEndpoint;
    
    @Value("${ris.pacs.transmission.method:HL7_V2}")
    private String transmissionMethod; // HL7_V2 or HL7_FHIR
    
    @Value("${ris.pacs.transmission.timeout:30000}")
    private int transmissionTimeout;
    
    @Value("${ris.pacs.transmission.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${ris.pacs.transmission.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${ris.pacs.transmission.retry.delay:5000}")
    private long retryDelayMs;
    
    /**
     * Transmit imaging order to RIS/PACS
     * 
     * @param imagingOrder The imaging order to transmit
     * @return Transmission result with status and details
     */
    public TransmissionResult transmitOrder(ImagingOrder imagingOrder) {
        if (!transmissionEnabled) {
            log.warn("RIS/PACS transmission is disabled. Order {} will not be transmitted.", imagingOrder.getOrderId());
            return TransmissionResult.builder()
                .success(false)
                .status("DISABLED")
                .message("RIS/PACS transmission is disabled in configuration")
                .transmittedAt(LocalDateTime.now())
                .build();
        }
        
        if (risPacsEndpoint == null || risPacsEndpoint.isEmpty()) {
            log.error("RIS/PACS endpoint is not configured. Cannot transmit order {}", imagingOrder.getOrderId());
            return TransmissionResult.builder()
                .success(false)
                .status("CONFIGURATION_ERROR")
                .message("RIS/PACS endpoint is not configured")
                .transmittedAt(LocalDateTime.now())
                .build();
        }
        
        log.info("Transmitting imaging order {} to RIS/PACS endpoint: {}", imagingOrder.getOrderId(), risPacsEndpoint);
        
        int attempt = 0;
        TransmissionResult result = null;
        
        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                if ("HL7_FHIR".equalsIgnoreCase(transmissionMethod)) {
                    result = transmitFhirMessage(imagingOrder);
                } else {
                    result = transmitHL7V2Message(imagingOrder);
                }
                
                if (result.isSuccess()) {
                    log.info("Successfully transmitted imaging order {} to RIS/PACS on attempt {}", 
                        imagingOrder.getOrderId(), attempt);
                    return result;
                } else {
                    log.warn("Failed to transmit imaging order {} on attempt {}: {}", 
                        imagingOrder.getOrderId(), attempt, result.getMessage());
                }
            } catch (Exception e) {
                log.error("Exception during RIS/PACS transmission attempt {} for order {}: {}", 
                    attempt, imagingOrder.getOrderId(), e.getMessage(), e);
                result = TransmissionResult.builder()
                    .success(false)
                    .status("ERROR")
                    .message("Exception: " + e.getMessage())
                    .transmittedAt(LocalDateTime.now())
                    .attempt(attempt)
                    .build();
            }
            
            // Retry if enabled and not last attempt
            if (retryEnabled && attempt < maxRetryAttempts && !result.isSuccess()) {
                try {
                    log.info("Retrying transmission for order {} in {} ms (attempt {}/{})", 
                        imagingOrder.getOrderId(), retryDelayMs, attempt + 1, maxRetryAttempts);
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted during retry delay for order {}", imagingOrder.getOrderId());
                    break;
                }
            }
        }
        
        log.error("Failed to transmit imaging order {} after {} attempts", 
            imagingOrder.getOrderId(), maxRetryAttempts);
        return result != null ? result : TransmissionResult.builder()
            .success(false)
            .status("FAILED")
            .message("Failed after " + maxRetryAttempts + " attempts")
            .transmittedAt(LocalDateTime.now())
            .attempt(maxRetryAttempts)
            .build();
    }
    
    /**
     * Transmit HL7 V2 ORM message to RIS/PACS
     */
    private TransmissionResult transmitHL7V2Message(ImagingOrder imagingOrder) {
        try {
            String hl7Message = hl7V2OrmMessageGenerator.generateOrmMessageForImaging(imagingOrder);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("X-Message-Type", "HL7_V2_ORM");
            headers.set("X-Order-Id", imagingOrder.getOrderId().toString());
            headers.set("X-Order-Number", imagingOrder.getOrderNumber());
            
            HttpEntity<String> request = new HttpEntity<>(hl7Message, headers);
            
            log.debug("Sending HL7 V2 ORM message to RIS/PACS: {}", risPacsEndpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                risPacsEndpoint,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return TransmissionResult.builder()
                    .success(true)
                    .status("SENT")
                    .message("Successfully transmitted to RIS/PACS")
                    .transmittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .responseMessage(response.getBody())
                    .transmissionMethod("HL7_V2")
                    .build();
            } else {
                return TransmissionResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("RIS/PACS returned status: " + response.getStatusCode())
                    .transmittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .transmissionMethod("HL7_V2")
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during HL7 V2 transmission: {}", e.getMessage());
            return TransmissionResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .transmittedAt(LocalDateTime.now())
                .transmissionMethod("HL7_V2")
                .build();
        }
    }
    
    /**
     * Transmit HL7 FHIR ServiceRequest to RIS/PACS
     */
    private TransmissionResult transmitFhirMessage(ImagingOrder imagingOrder) {
        try {
            Map<String, Object> serviceRequest = hl7FhirServiceRequestGenerator.generateServiceRequestForImaging(imagingOrder);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Message-Type", "HL7_FHIR_SERVICEREQUEST");
            headers.set("X-Order-Id", imagingOrder.getOrderId().toString());
            headers.set("X-Order-Number", imagingOrder.getOrderNumber());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(serviceRequest, headers);
            
            log.debug("Sending HL7 FHIR ServiceRequest to RIS/PACS: {}", risPacsEndpoint);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                risPacsEndpoint,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return TransmissionResult.builder()
                    .success(true)
                    .status("SENT")
                    .message("Successfully transmitted to RIS/PACS")
                    .transmittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .responseMessage(response.getBody() != null ? response.getBody().toString() : null)
                    .transmissionMethod("HL7_FHIR")
                    .build();
            } else {
                return TransmissionResult.builder()
                    .success(false)
                    .status("HTTP_ERROR")
                    .message("RIS/PACS returned status: " + response.getStatusCode())
                    .transmittedAt(LocalDateTime.now())
                    .responseCode(response.getStatusCode().value())
                    .transmissionMethod("HL7_FHIR")
                    .build();
            }
        } catch (RestClientException e) {
            log.error("RestClientException during HL7 FHIR transmission: {}", e.getMessage());
            return TransmissionResult.builder()
                .success(false)
                .status("TRANSMISSION_ERROR")
                .message("Network error: " + e.getMessage())
                .transmittedAt(LocalDateTime.now())
                .transmissionMethod("HL7_FHIR")
                .build();
        }
    }
    
    /**
     * Transmission result
     */
    @lombok.Data
    @lombok.Builder
    public static class TransmissionResult {
        private boolean success;
        private String status; // SENT, FAILED, ERROR, DISABLED, CONFIGURATION_ERROR, HTTP_ERROR, TRANSMISSION_ERROR
        private String message;
        private LocalDateTime transmittedAt;
        private Integer responseCode;
        private String responseMessage;
        private String transmissionMethod; // HL7_V2 or HL7_FHIR
        private Integer attempt;
    }
}
