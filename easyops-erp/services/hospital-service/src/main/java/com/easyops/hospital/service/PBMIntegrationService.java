package com.easyops.hospital.service;

import com.easyops.hospital.entity.FormularyCheck;
import com.easyops.hospital.entity.PriorAuthorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for integrating with Pharmacy Benefit Managers (PBM)
 * This service handles communication with external PBM systems for:
 * - Formulary checking
 * - Medication coverage verification
 * - Prior authorization requests
 * - Cost estimation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PBMIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${pbm.integration.enabled:false}")
    private boolean pbmIntegrationEnabled;
    
    @Value("${pbm.integration.endpoint:}")
    private String pbmEndpoint;
    
    @Value("${pbm.integration.api-key:}")
    private String pbmApiKey;
    
    @Value("${pbm.integration.timeout:30000}")
    private int pbmTimeout;
    
    @Value("${pbm.integration.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${pbm.integration.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${pbm.integration.retry.delay:5000}")
    private long retryDelay;
    
    /**
     * Check medication coverage with PBM
     */
    public FormularyCheck checkFormularyCoverage(
            UUID patientId,
            UUID insuranceId,
            String medicationCode,
            String medicationName,
            String policyNumber,
            String insuranceCompanyName) {
        
        log.info("Checking formulary coverage for medication: {} via PBM", medicationCode);
        
        if (!pbmIntegrationEnabled || pbmEndpoint == null || pbmEndpoint.isEmpty()) {
            log.warn("PBM integration is disabled or endpoint not configured. Returning mock response.");
            return createMockFormularyCheck(patientId, insuranceId, medicationCode, medicationName, 
                    policyNumber, insuranceCompanyName);
        }
        
        try {
            // Build request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("patientId", patientId.toString());
            requestPayload.put("insuranceId", insuranceId != null ? insuranceId.toString() : null);
            requestPayload.put("medicationCode", medicationCode);
            requestPayload.put("medicationName", medicationName);
            requestPayload.put("policyNumber", policyNumber);
            requestPayload.put("insuranceCompanyName", insuranceCompanyName);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (pbmApiKey != null && !pbmApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + pbmApiKey);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            // Make API call with retry logic
            ResponseEntity<Map> response = null;
            int attempts = 0;
            Exception lastException = null;
            
            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            pbmEndpoint + "/formulary/check",
                            HttpMethod.POST,
                            request,
                            Map.class
                    );
                    break; // Success
                } catch (Exception e) {
                    lastException = e;
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("PBM API call failed, retrying (attempt {}/{})", attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("PBM API call interrupted", ie);
                        }
                    } else {
                        throw new RuntimeException("PBM API call failed after " + attempts + " attempts", e);
                    }
                }
            }
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePBMFormularyResponse(response.getBody(), patientId, insuranceId, 
                        medicationCode, medicationName, policyNumber, insuranceCompanyName);
            } else {
                throw new RuntimeException("PBM API returned error: " + 
                        (response != null ? response.getStatusCode() : "No response"));
            }
            
        } catch (Exception e) {
            log.error("Error checking formulary coverage via PBM", e);
            // Return error status check
            return FormularyCheck.builder()
                    .coverageStatus(FormularyCheck.CoverageStatus.ERROR)
                    .checkStatus(FormularyCheck.CheckStatus.FAILED)
                    .errorMessage("PBM integration error: " + e.getMessage())
                    .checkDate(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Submit prior authorization request to PBM
     */
    public PriorAuthorization submitPriorAuthorization(
            UUID prescriptionId,
            UUID insuranceId,
            String medicationCode,
            String medicationName,
            String policyNumber,
            String insuranceCompanyName,
            String clinicalJustification,
            String supportingDocumentation) {
        
        log.info("Submitting prior authorization request for medication: {} via PBM", medicationCode);
        
        if (!pbmIntegrationEnabled || pbmEndpoint == null || pbmEndpoint.isEmpty()) {
            log.warn("PBM integration is disabled or endpoint not configured. Returning mock response.");
            return createMockPriorAuthorization(prescriptionId, insuranceId, medicationCode, 
                    medicationName, policyNumber, insuranceCompanyName, clinicalJustification);
        }
        
        try {
            // Build request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("prescriptionId", prescriptionId.toString());
            requestPayload.put("insuranceId", insuranceId != null ? insuranceId.toString() : null);
            requestPayload.put("medicationCode", medicationCode);
            requestPayload.put("medicationName", medicationName);
            requestPayload.put("policyNumber", policyNumber);
            requestPayload.put("insuranceCompanyName", insuranceCompanyName);
            requestPayload.put("clinicalJustification", clinicalJustification);
            requestPayload.put("supportingDocumentation", supportingDocumentation);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (pbmApiKey != null && !pbmApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + pbmApiKey);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            // Make API call with retry logic
            ResponseEntity<Map> response = null;
            int attempts = 0;
            
            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            pbmEndpoint + "/prior-auth/submit",
                            HttpMethod.POST,
                            request,
                            Map.class
                    );
                    break; // Success
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("PBM prior auth API call failed, retrying (attempt {}/{})", attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("PBM API call interrupted", ie);
                        }
                    } else {
                        throw new RuntimeException("PBM prior auth API call failed after " + attempts + " attempts", e);
                    }
                }
            }
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePBMPriorAuthResponse(response.getBody(), prescriptionId, insuranceId, 
                        medicationCode, medicationName, policyNumber, insuranceCompanyName, clinicalJustification);
            } else {
                throw new RuntimeException("PBM API returned error: " + 
                        (response != null ? response.getStatusCode() : "No response"));
            }
            
        } catch (Exception e) {
            log.error("Error submitting prior authorization via PBM", e);
            // Return error status prior auth
            return PriorAuthorization.builder()
                    .status(PriorAuthorization.PriorAuthStatus.DENIED)
                    .denialReason("PBM integration error: " + e.getMessage())
                    .requestDate(LocalDate.now())
                    .build();
        }
    }
    
    /**
     * Get prior authorization status from PBM
     */
    public PriorAuthorization checkPriorAuthorizationStatus(String pbmRequestId) {
        log.info("Checking prior authorization status for request: {}", pbmRequestId);
        
        if (!pbmIntegrationEnabled || pbmEndpoint == null || pbmEndpoint.isEmpty()) {
            log.warn("PBM integration is disabled or endpoint not configured.");
            return null;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (pbmApiKey != null && !pbmApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + pbmApiKey);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    pbmEndpoint + "/prior-auth/status/" + pbmRequestId,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePBMPriorAuthStatusResponse(response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error checking prior authorization status via PBM", e);
        }
        
        return null;
    }
    
    // ========== Helper Methods ==========
    
    private FormularyCheck createMockFormularyCheck(
            UUID patientId, UUID insuranceId, String medicationCode, String medicationName,
            String policyNumber, String insuranceCompanyName) {
        
        // Mock response - in production, this would be replaced with actual PBM integration
        return FormularyCheck.builder()
                .insuranceId(insuranceId)
                .insuranceCompanyName(insuranceCompanyName != null ? insuranceCompanyName : "Mock Insurance")
                .policyNumber(policyNumber)
                .medicationCode(medicationCode)
                .medicationName(medicationName)
                .coverageStatus(FormularyCheck.CoverageStatus.COVERED)
                .formularyTier("Tier 2")
                .requiresPriorAuthorization(false)
                .priorAuthorizationRequired(false)
                .stepTherapyRequired(false)
                .copayAmount(new BigDecimal("25.00"))
                .coinsurancePercentage(new BigDecimal("20.00"))
                .deductibleApplies(false)
                .patientCostEstimate(new BigDecimal("25.00"))
                .insurancePays(new BigDecimal("100.00"))
                .pbmName("Mock PBM")
                .checkDate(LocalDateTime.now())
                .checkStatus(FormularyCheck.CheckStatus.COMPLETED)
                .build();
    }
    
    private FormularyCheck parsePBMFormularyResponse(
            Map<String, Object> responseBody, UUID patientId, UUID insuranceId,
            String medicationCode, String medicationName, String policyNumber, String insuranceCompanyName) {
        
        // Parse PBM response - adjust based on actual PBM API response format
        FormularyCheck.FormularyCheckBuilder builder = FormularyCheck.builder()
                .insuranceId(insuranceId)
                .insuranceCompanyName(insuranceCompanyName)
                .policyNumber(policyNumber)
                .medicationCode(medicationCode)
                .medicationName(medicationName)
                .checkDate(LocalDateTime.now())
                .checkStatus(FormularyCheck.CheckStatus.COMPLETED);
        
        // Parse coverage status
        String coverageStatusStr = (String) responseBody.get("coverageStatus");
        if (coverageStatusStr != null) {
            try {
                builder.coverageStatus(FormularyCheck.CoverageStatus.valueOf(coverageStatusStr));
            } catch (IllegalArgumentException e) {
                builder.coverageStatus(FormularyCheck.CoverageStatus.UNKNOWN);
            }
        }
        
        // Parse other fields
        builder.formularyTier((String) responseBody.get("formularyTier"));
        builder.requiresPriorAuthorization((Boolean) responseBody.getOrDefault("requiresPriorAuthorization", false));
        builder.priorAuthorizationRequired((Boolean) responseBody.getOrDefault("priorAuthorizationRequired", false));
        builder.stepTherapyRequired((Boolean) responseBody.getOrDefault("stepTherapyRequired", false));
        
        if (responseBody.get("copayAmount") != null) {
            builder.copayAmount(new BigDecimal(responseBody.get("copayAmount").toString()));
        }
        if (responseBody.get("patientCostEstimate") != null) {
            builder.patientCostEstimate(new BigDecimal(responseBody.get("patientCostEstimate").toString()));
        }
        if (responseBody.get("insurancePays") != null) {
            builder.insurancePays(new BigDecimal(responseBody.get("insurancePays").toString()));
        }
        
        builder.pbmName((String) responseBody.get("pbmName"));
        builder.pbmId((String) responseBody.get("pbmId"));
        builder.formularyId((String) responseBody.get("formularyId"));
        builder.formularyName((String) responseBody.get("formularyName"));
        
        return builder.build();
    }
    
    private PriorAuthorization createMockPriorAuthorization(
            UUID prescriptionId, UUID insuranceId, String medicationCode, String medicationName,
            String policyNumber, String insuranceCompanyName, String clinicalJustification) {
        
        // Mock response – we don't attach the full Prescription entity here
        return PriorAuthorization.builder()
                .insuranceId(insuranceId)
                .insuranceCompanyName(insuranceCompanyName != null ? insuranceCompanyName : "Mock Insurance")
                .policyNumber(policyNumber)
                .medicationCode(medicationCode)
                .medicationName(medicationName)
                .priorAuthNumber("MOCK-PA-" + UUID.randomUUID().toString().substring(0, 8))
                .requestDate(LocalDate.now())
                .status(PriorAuthorization.PriorAuthStatus.SUBMITTED)
                .submittedDate(LocalDate.now())
                .clinicalJustification(clinicalJustification)
                .pbmName("Mock PBM")
                .pbmRequestId("MOCK-REQ-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }
    
    private PriorAuthorization parsePBMPriorAuthResponse(
            Map<String, Object> responseBody, UUID prescriptionId, UUID insuranceId,
            String medicationCode, String medicationName, String policyNumber, 
            String insuranceCompanyName, String clinicalJustification) {
        
        PriorAuthorization.PriorAuthorizationBuilder builder = PriorAuthorization.builder()
                .insuranceId(insuranceId)
                .insuranceCompanyName(insuranceCompanyName)
                .policyNumber(policyNumber)
                .medicationCode(medicationCode)
                .medicationName(medicationName)
                .requestDate(LocalDate.now())
                .clinicalJustification(clinicalJustification);
        
        String statusStr = (String) responseBody.get("status");
        if (statusStr != null) {
            try {
                builder.status(PriorAuthorization.PriorAuthStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                builder.status(PriorAuthorization.PriorAuthStatus.PENDING);
            }
        }
        
        builder.priorAuthNumber((String) responseBody.get("priorAuthNumber"));
        builder.pbmRequestId((String) responseBody.get("pbmRequestId"));
        builder.pbmResponseId((String) responseBody.get("pbmResponseId"));
        builder.pbmName((String) responseBody.get("pbmName"));
        
        if (responseBody.get("submittedDate") != null) {
            builder.submittedDate(LocalDate.parse(responseBody.get("submittedDate").toString()));
        }
        if (responseBody.get("approvedDate") != null) {
            builder.approvedDate(LocalDate.parse(responseBody.get("approvedDate").toString()));
        }
        if (responseBody.get("expirationDate") != null) {
            builder.expirationDate(LocalDate.parse(responseBody.get("expirationDate").toString()));
        }
        
        return builder.build();
    }
    
    private PriorAuthorization parsePBMPriorAuthStatusResponse(Map<String, Object> responseBody) {
        // Parse status response
        PriorAuthorization.PriorAuthorizationBuilder builder = PriorAuthorization.builder();
        
        String statusStr = (String) responseBody.get("status");
        if (statusStr != null) {
            try {
                builder.status(PriorAuthorization.PriorAuthStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                builder.status(PriorAuthorization.PriorAuthStatus.PENDING);
            }
        }
        
        builder.priorAuthNumber((String) responseBody.get("priorAuthNumber"));
        builder.denialReason((String) responseBody.get("denialReason"));
        
        if (responseBody.get("approvedDate") != null) {
            builder.approvedDate(LocalDate.parse(responseBody.get("approvedDate").toString()));
        }
        if (responseBody.get("deniedDate") != null) {
            builder.deniedDate(LocalDate.parse(responseBody.get("deniedDate").toString()));
        }
        if (responseBody.get("expirationDate") != null) {
            builder.expirationDate(LocalDate.parse(responseBody.get("expirationDate").toString()));
        }
        
        return builder.build();
    }
}
