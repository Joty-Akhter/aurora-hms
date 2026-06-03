package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PDMPQueryRequest;
import com.easyops.hospital.dto.response.PDMPQueryResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.PatientNameInterop;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PDMPService {

    private final PDMPQueryResultRepository pdmpQueryResultRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final RestTemplate restTemplate;

    public PDMPService(
            PDMPQueryResultRepository pdmpQueryResultRepository,
            PrescriptionRepository prescriptionRepository,
            PatientRepository patientRepository,
            @Qualifier("pdmpRestTemplate") RestTemplate restTemplate) {
        this.pdmpQueryResultRepository = pdmpQueryResultRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.restTemplate = restTemplate;
    }
    
    @Value("${pdmp.enabled:true}")
    private boolean pdmpEnabled;
    
    @Value("${pdmp.simulate:true}")
    private boolean simulatePDMP;
    
    @Value("${pdmp.integration.endpoint:}")
    private String pdmpEndpoint;
    
    @Value("${pdmp.integration.api-key:}")
    private String pdmpApiKey;
    
    @Value("${pdmp.integration.timeout:30000}")
    private int pdmpTimeout;
    
    @Value("${pdmp.integration.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${pdmp.integration.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${pdmp.integration.retry.delay:5000}")
    private long retryDelay;
    
    @PostConstruct
    void logPdmpIntegrationMode() {
        if (!pdmpEnabled) {
            log.info("PDMP integration disabled (pdmp.enabled=false).");
            return;
        }
        if (simulatePDMP) {
            log.warn("*** PDMP SIMULATION MODE (pdmp.simulate=true / PDMP_SIMULATE=true) *** "
                    + "Queries use synthetic data from local EHR only — no state PDMP is contacted. "
                    + "For production: set PDMP_SIMULATE=false, pdmp.integration.endpoint, and PDMP_API_KEY.");
            return;
        }
        if (pdmpEndpoint == null || pdmpEndpoint.isBlank()) {
            log.error("PDMP LIVE mode requested (simulate=false) but pdmp.integration.endpoint is blank — "
                    + "performRealPDMPQuery will return HTTP 503 until PDMP_ENDPOINT is configured.");
        } else {
            log.info("PDMP LIVE integration active: endpoint={}, client timeout={}ms (M3)",
                    pdmpEndpoint, pdmpTimeout);
        }
    }

    /**
     * Query PDMP for a patient's controlled substance history
     */
    @Transactional
    public PDMPQueryResponse queryPDMP(PDMPQueryRequest request, UUID providerId, String providerNpi, String providerName) {
        log.info("Querying PDMP for patient: {}, prescription: {}, state: {}", 
                request.getPatientId(), request.getPrescriptionId(), request.getQueryState());
        
        // Validate patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Patient not found: " + request.getPatientId()));
        
        // Validate prescription exists
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + request.getPrescriptionId()));
        
        // Check if PDMP is enabled
        if (!pdmpEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "PDMP integration is not enabled for this installation");
        }
        
        // Determine query state (use patient's state if not provided)
        String queryState = request.getQueryState();
        if (queryState == null || queryState.isEmpty()) {
            queryState = patient.getPrimaryState();
            if (queryState == null || queryState.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Query state is required for PDMP query (patient has no primary state on file)");
            }
        }
        
        // Create PDMP query result entity
        PDMPQueryResult queryResult = PDMPQueryResult.builder()
            .prescription(prescription)
            .patient(patient)
            .queryDate(LocalDateTime.now())
            .queryState(queryState)
            .queryType(request.getQueryType() != null ? request.getQueryType() : 
                      PDMPQueryResult.QueryType.PRESCRIPTION_CHECK)
            .queryingProviderId(providerId)
            .queryingProviderNpi(providerNpi)
            .queryingProviderName(providerName)
            .deaNumber(request.getDeaNumber())
            .queryStatus(PDMPQueryResult.QueryStatus.IN_PROGRESS)
            .querySuccess(false)
            .queryReason(request.getQueryReason() != null ? request.getQueryReason() : 
                        "Prescribing controlled substance")
            .createdBy(providerId)
            .build();
        
        queryResult = pdmpQueryResultRepository.save(queryResult);
        
        try {
            // Perform PDMP query
            PDMPQueryResult result;
            if (simulatePDMP) {
                result = performSimulatedPDMPQuery(queryResult, patient, prescription, queryState);
            } else {
                result = performRealPDMPQuery(queryResult, patient, prescription, queryState);
            }
            
            // Update prescription with PDMP query information
            prescription.setPdmpQueried(true);
            prescription.setPdmpQueryDate(LocalDateTime.now());
            prescriptionRepository.save(prescription);
            
            log.info("PDMP query completed successfully for patient: {}, query result: {}", 
                    patient.getPatientId(), result.getQueryResultId());
            
            return mapToResponse(result);
            
        } catch (Exception e) {
            log.error("PDMP query failed for patient: {}", patient.getPatientId(), e);
            queryResult.setQueryStatus(PDMPQueryResult.QueryStatus.FAILED);
            queryResult.setQuerySuccess(false);
            queryResult.setErrorMessage(e.getMessage());
            queryResult = pdmpQueryResultRepository.save(queryResult);
            
            // Re-throw ResponseStatusException as-is; wrap other failures as 502 Bad Gateway
            // (the downstream PDMP service failed, not the caller's request)
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "PDMP query failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform simulated PDMP query (for development/testing)
     * In production, this would be replaced with actual PDMP API integration
     */
    private PDMPQueryResult performSimulatedPDMPQuery(
            PDMPQueryResult queryResult, 
            Patient patient, 
            Prescription prescription,
            String state) {
        
        log.info("Performing simulated PDMP query for state: {}", state);
        
        // Simulate API delay
        try {
            Thread.sleep(500); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get existing prescriptions for the patient to simulate history
        List<Prescription> existingPrescriptions = prescriptionRepository
            .findByPatientPatientIdOrderByCreatedDateDesc(patient.getPatientId());
        
        // Filter for controlled substances
        List<Prescription> controlledSubstancePrescriptions = existingPrescriptions.stream()
            .filter(p -> p.getIsControlledSubstance() != null && p.getIsControlledSubstance())
            .filter(p -> !p.getPrescriptionId().equals(prescription.getPrescriptionId()))
            .collect(Collectors.toList());
        
        // Build prescription history
        List<Map<String, Object>> prescriptionHistory = new ArrayList<>();
        Set<String> pharmacyNames = new HashSet<>();
        Set<String> prescriberNames = new HashSet<>();
        
        for (Prescription p : controlledSubstancePrescriptions) {
            Map<String, Object> historyItem = new HashMap<>();
            historyItem.put("prescriptionId", p.getPrescriptionId().toString());
            historyItem.put("medicationName", p.getMedicationName());
            historyItem.put("medicationCode", p.getMedicationCode());
            historyItem.put("dosageStrength", p.getDosageStrength());
            historyItem.put("dosageUnit", p.getDosageUnit());
            historyItem.put("quantity", PrescriptionDerivedQuantity.deriveUnits(p.getFrequency(), p.getDurationDays()));
            historyItem.put("schedule", p.getSchedule() != null ? p.getSchedule().toString() : null);
            historyItem.put("prescribedDate", p.getCreatedDate() != null ? p.getCreatedDate().toLocalDate() : null);
            historyItem.put("filledDate", p.getFilledDate() != null ? p.getFilledDate().toLocalDate() : null);
            historyItem.put("prescriberName", p.getPrescribingProviderName());
            historyItem.put("prescriberNpi", p.getPrescribingProviderNpi());
            historyItem.put("pharmacyName", p.getPharmacyName());
            historyItem.put("pharmacyNpi", p.getPharmacyNpi());
            historyItem.put("status", p.getPrescriptionStatus() != null ? p.getPrescriptionStatus().toString() : null);
            
            prescriptionHistory.add(historyItem);
            
            if (p.getPharmacyName() != null) {
                pharmacyNames.add(p.getPharmacyName());
            }
            if (p.getPrescribingProviderName() != null) {
                prescriberNames.add(p.getPrescribingProviderName());
            }
        }
        
        // Calculate risk indicators
        boolean hasMultiplePharmacies = pharmacyNames.size() > 1;
        boolean hasMultiplePrescribers = prescriberNames.size() > 1;
        boolean hasOverlapping = checkForOverlappingPrescriptions(controlledSubstancePrescriptions);
        boolean hasEarlyRefills = checkForEarlyRefills(controlledSubstancePrescriptions);
        
        // Calculate risk score (0-100)
        int riskScore = 0;
        if (controlledSubstancePrescriptions.size() > 5) riskScore += 20;
        if (hasMultiplePharmacies) riskScore += 25;
        if (hasMultiplePrescribers) riskScore += 25;
        if (hasOverlapping) riskScore += 15;
        if (hasEarlyRefills) riskScore += 15;
        
        // Determine risk level
        PDMPQueryResult.RiskLevel riskLevel;
        if (riskScore >= 70) {
            riskLevel = PDMPQueryResult.RiskLevel.CRITICAL;
        } else if (riskScore >= 50) {
            riskLevel = PDMPQueryResult.RiskLevel.HIGH;
        } else if (riskScore >= 30) {
            riskLevel = PDMPQueryResult.RiskLevel.MODERATE;
        } else {
            riskLevel = PDMPQueryResult.RiskLevel.LOW;
        }
        
        // Build warnings
        List<String> warnings = new ArrayList<>();
        if (hasMultiplePharmacies) {
            warnings.add("Patient has prescriptions from multiple pharmacies");
        }
        if (hasMultiplePrescribers) {
            warnings.add("Patient has prescriptions from multiple prescribers");
        }
        if (hasOverlapping) {
            warnings.add("Patient has overlapping prescriptions for controlled substances");
        }
        if (hasEarlyRefills) {
            warnings.add("Patient has early refill requests");
        }
        if (controlledSubstancePrescriptions.size() > 3) {
            warnings.add("Patient has multiple controlled substance prescriptions");
        }
        
        // Update query result
        queryResult.setQueryStatus(PDMPQueryResult.QueryStatus.COMPLETED);
        queryResult.setQuerySuccess(true);
        queryResult.setTotalPrescriptions(controlledSubstancePrescriptions.size());
        queryResult.setTotalPharmacies(pharmacyNames.size());
        queryResult.setTotalPrescribers(prescriberNames.size());
        queryResult.setHasControlledSubstances(!controlledSubstancePrescriptions.isEmpty());
        queryResult.setRiskScore(riskScore);
        queryResult.setRiskLevel(riskLevel);
        queryResult.setPrescriptionHistory(prescriptionHistory);
        queryResult.setHasMultiplePharmacies(hasMultiplePharmacies);
        queryResult.setHasMultiplePrescribers(hasMultiplePrescribers);
        queryResult.setHasOverlappingPrescriptions(hasOverlapping);
        queryResult.setHasEarlyRefills(hasEarlyRefills);
        queryResult.setWarnings(String.join("; ", warnings));
        queryResult.setPdmpSystemName("Simulated PDMP System");
        queryResult.setPdmpSystemId("SIM-" + state);
        queryResult.setPdmpQueryId(UUID.randomUUID().toString());
        queryResult.setPdmpResponseId(UUID.randomUUID().toString());
        
        // Set date range
        if (!controlledSubstancePrescriptions.isEmpty()) {
            LocalDate minDate = controlledSubstancePrescriptions.stream()
                .map(p -> p.getCreatedDate() != null ? p.getCreatedDate().toLocalDate() : LocalDate.now())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now().minusYears(1));
            LocalDate maxDate = controlledSubstancePrescriptions.stream()
                .map(p -> p.getCreatedDate() != null ? p.getCreatedDate().toLocalDate() : LocalDate.now())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
            
            queryResult.setDateRangeStart(minDate);
            queryResult.setDateRangeEnd(maxDate);
        }
        
        return pdmpQueryResultRepository.save(queryResult);
    }
    
    /**
     * Perform real PDMP query with actual API integration
     */
    private PDMPQueryResult performRealPDMPQuery(
            PDMPQueryResult queryResult,
            Patient patient,
            Prescription prescription,
            String state) {
        
        log.info("Performing real PDMP query for state: {}", state);
        
        if (pdmpEndpoint == null || pdmpEndpoint.isEmpty()) {
            log.error("PDMP endpoint not configured (pdmp.integration.endpoint is blank) — " +
                    "cannot perform live PDMP query for prescription {}. " +
                    "Set PDMP_ENDPOINT in environment config.", prescription.getPrescriptionId());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "PDMP endpoint is not configured. Set PDMP_ENDPOINT and PDMP_API_KEY " +
                    "in environment config, or enable simulation mode for non-production use.");
        }
        
        try {
            // Build request payload
            Map<String, Object> requestPayload = new HashMap<>();
            String[] nameParts = PatientNameInterop.splitFullName(patient.getFullName());
            requestPayload.put("patient", Map.of(
                    "firstName", nameParts[0],
                    "lastName", nameParts[2],
                    "dateOfBirth", patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "",
                    "idNo", patient.getIdNo() != null ? patient.getIdNo() : ""
            ));
            requestPayload.put("state", state);
            requestPayload.put("prescriptionId", prescription.getPrescriptionId().toString());
            requestPayload.put("medicationName", prescription.getMedicationName() != null ? prescription.getMedicationName() : "");
            requestPayload.put("isControlledSubstance", prescription.getIsControlledSubstance() != null ? prescription.getIsControlledSubstance() : false);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (pdmpApiKey != null && !pdmpApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + pdmpApiKey);
            }
            headers.set("X-PDMP-State", state); // State-specific header
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
            
            // Make API call with retry logic
            ResponseEntity<Map> response = null;
            int attempts = 0;
            
            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            pdmpEndpoint + "/query",
                            HttpMethod.POST,
                            request,
                            Map.class
                    );
                    break; // Success
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("PDMP API call failed, retrying (attempt {}/{})", attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "PDMP API call interrupted");
                        }
                    } else {
                        log.error("PDMP API call failed after {} attempt(s) — aborting, not falling back to simulation. " +
                                "Prescription {} blocked.", attempts, prescription.getPrescriptionId(), e);
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "PDMP API unreachable after " + attempts + " attempt(s): " + e.getMessage());
                    }
                }
            }
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePDMPResponse(response.getBody(), queryResult, state);
            } else {
                HttpStatusCode status = response != null ? response.getStatusCode() : null;
                log.error("PDMP API returned non-2xx status {} for prescription {} — aborting, not falling back to simulation.",
                        status, prescription.getPrescriptionId());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "PDMP API returned an error response (HTTP " + status + "). " +
                        "Prescription blocked until a successful PDMP query is obtained.");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error performing real PDMP query for prescription {} — aborting.",
                    prescription.getPrescriptionId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PDMP query failed due to an unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Parse PDMP API response
     */
    private PDMPQueryResult parsePDMPResponse(Map<String, Object> responseBody, PDMPQueryResult queryResult, String state) {
        // Parse response from PDMP API
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> prescriptionHistory = (List<Map<String, Object>>) responseBody.get("prescriptionHistory");
        
        if (prescriptionHistory != null) {
            queryResult.setPrescriptionHistory(prescriptionHistory);
        }
        
        // Parse risk indicators
        @SuppressWarnings("unchecked")
        Map<String, Object> riskIndicators = (Map<String, Object>) responseBody.get("riskIndicators");
        if (riskIndicators != null) {
            queryResult.setRiskIndicators(riskIndicators);
            
            // Determine risk level
            String riskLevelStr = (String) riskIndicators.get("riskLevel");
            if (riskLevelStr != null) {
                try {
                    queryResult.setRiskLevel(PDMPQueryResult.RiskLevel.valueOf(riskLevelStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    queryResult.setRiskLevel(PDMPQueryResult.RiskLevel.UNKNOWN);
                }
            }
            
            // Parse risk score
            if (riskIndicators.get("riskScore") != null) {
                queryResult.setRiskScore(((Number) riskIndicators.get("riskScore")).intValue());
            }
        }
        
        // Parse alerts
        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) responseBody.get("alerts");
        if (alerts != null) {
            queryResult.setAlerts(alerts);
        }
        
        // Set system information
        queryResult.setPdmpSystemName((String) responseBody.getOrDefault("systemName", "PDMP System"));
        queryResult.setPdmpSystemId((String) responseBody.get("systemId"));
        queryResult.setQueryState(state);

        // Parse pharmacy list if present
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pharmacyList = (List<Map<String, Object>>) responseBody.get("pharmacyList");
        if (pharmacyList != null) {
            queryResult.setPharmacyList(pharmacyList);
        }

        // Parse prescriber list if present
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> prescriberList = (List<Map<String, Object>>) responseBody.get("prescriberList");
        if (prescriberList != null) {
            queryResult.setPrescriberList(prescriberList);
        }
        
        // Store raw response
        queryResult.setRawResponse(responseBody);
        
        queryResult.setQueryStatus(PDMPQueryResult.QueryStatus.COMPLETED);
        queryResult.setQuerySuccess(true);
        queryResult.setQueryDate(LocalDateTime.now());
        
        // Persist the completed result; unlike the simulated path this was not yet saved.
        return pdmpQueryResultRepository.save(queryResult);
    }
    
    /**
     * Get PDMP query results for a prescription
     */
    public List<PDMPQueryResponse> getPDMPQueryResults(UUID prescriptionId) {
        List<PDMPQueryResult> results = pdmpQueryResultRepository
            .findByPrescriptionPrescriptionIdOrderByQueryDateDesc(prescriptionId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the most recent PDMP query result for a prescription
     */
    public PDMPQueryResponse getLatestPDMPQueryResult(UUID prescriptionId) {
        PDMPQueryResult result = pdmpQueryResultRepository
            .findFirstByPrescriptionPrescriptionIdOrderByQueryDateDesc(prescriptionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No PDMP query results found for prescription: " + prescriptionId));
        return mapToResponse(result);
    }
    
    /**
     * Get PDMP query results for a patient
     */
    public List<PDMPQueryResponse> getPDMPQueryResultsByPatient(UUID patientId) {
        List<PDMPQueryResult> results = pdmpQueryResultRepository
            .findByPatientPatientIdOrderByQueryDateDesc(patientId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Check for overlapping prescriptions
     */
    private boolean checkForOverlappingPrescriptions(List<Prescription> prescriptions) {
        for (int i = 0; i < prescriptions.size(); i++) {
            for (int j = i + 1; j < prescriptions.size(); j++) {
                Prescription p1 = prescriptions.get(i);
                Prescription p2 = prescriptions.get(j);
                
                if (p1.getStartDate() != null && p2.getStartDate() != null &&
                    p1.getEndDate() != null && p2.getEndDate() != null) {
                    
                    // Check if date ranges overlap
                    if (!(p1.getEndDate().isBefore(p2.getStartDate()) || 
                          p2.getEndDate().isBefore(p1.getStartDate()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check for early refills
     */
    private boolean checkForEarlyRefills(List<Prescription> prescriptions) {
        // Check if any prescription was filled before the expected end date of a previous one
        // This is a simplified check
        for (Prescription p : prescriptions) {
            if (p.getFilledDate() != null && p.getStartDate() != null && p.getDurationDays() != null) {
                LocalDate expectedEndDate = p.getStartDate().plusDays(p.getDurationDays());
                if (p.getFilledDate().toLocalDate().isBefore(expectedEndDate.minusDays(3))) {
                    return true; // Filled more than 3 days early
                }
            }
        }
        return false;
    }
    
    /**
     * Map PDMPQueryResult entity to response DTO
     */
    private PDMPQueryResponse mapToResponse(PDMPQueryResult result) {
        PDMPQueryResponse.PDMPQueryResponseBuilder builder = PDMPQueryResponse.builder()
            .queryResultId(result.getQueryResultId())
            .prescriptionId(result.getPrescription().getPrescriptionId())
            .patientId(result.getPatient().getPatientId())
            .queryDate(result.getQueryDate())
            .queryState(result.getQueryState())
            .queryType(result.getQueryType())
            .queryingProviderId(result.getQueryingProviderId())
            .queryingProviderNpi(result.getQueryingProviderNpi())
            .queryingProviderName(result.getQueryingProviderName())
            .deaNumber(result.getDeaNumber())
            .queryStatus(result.getQueryStatus())
            .querySuccess(result.getQuerySuccess())
            .errorMessage(result.getErrorMessage())
            .totalPrescriptions(result.getTotalPrescriptions())
            .totalPharmacies(result.getTotalPharmacies())
            .totalPrescribers(result.getTotalPrescribers())
            .dateRangeStart(result.getDateRangeStart())
            .dateRangeEnd(result.getDateRangeEnd())
            .hasControlledSubstances(result.getHasControlledSubstances())
            .riskScore(result.getRiskScore())
            .riskLevel(result.getRiskLevel())
            .hasDuplicatePrescriptions(result.getHasDuplicatePrescriptions())
            .hasOverlappingPrescriptions(result.getHasOverlappingPrescriptions())
            .hasEarlyRefills(result.getHasEarlyRefills())
            .hasMultiplePrescribers(result.getHasMultiplePrescribers())
            .hasMultiplePharmacies(result.getHasMultiplePharmacies())
            .warnings(result.getWarnings())
            .queryReason(result.getQueryReason())
            .clinicalNotes(result.getClinicalNotes())
            .actionTaken(result.getActionTaken())
            .pdmpSystemName(result.getPdmpSystemName())
            .pdmpSystemId(result.getPdmpSystemId())
            .pdmpQueryId(result.getPdmpQueryId())
            .pdmpResponseId(result.getPdmpResponseId())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .createdBy(result.getCreatedBy())
            .updatedBy(result.getUpdatedBy());
        
        // Map prescription history
        if (result.getPrescriptionHistory() != null) {
            List<PDMPQueryResponse.PrescriptionHistoryItem> historyItems = result.getPrescriptionHistory().stream()
                .map(this::mapHistoryItem)
                .collect(Collectors.toList());
            builder.prescriptionHistory(historyItems);
        }

        // Map pharmacy list
        if (result.getPharmacyList() != null) {
            List<PDMPQueryResponse.PharmacyInfo> pharmacies = result.getPharmacyList().stream()
                .map(this::mapPharmacyInfo)
                .collect(Collectors.toList());
            builder.pharmacyList(pharmacies);
        }

        // Map prescriber list
        if (result.getPrescriberList() != null) {
            List<PDMPQueryResponse.PrescriberInfo> prescribers = result.getPrescriberList().stream()
                .map(this::mapPrescriberInfo)
                .collect(Collectors.toList());
            builder.prescriberList(prescribers);
        }
        
        return builder.build();
    }

    private PDMPQueryResponse.PharmacyInfo mapPharmacyInfo(Map<String, Object> item) {
        PDMPQueryResponse.PharmacyInfo.PharmacyInfoBuilder b = PDMPQueryResponse.PharmacyInfo.builder();
        if (item.get("pharmacyName") != null) b.pharmacyName(item.get("pharmacyName").toString());
        if (item.get("pharmacyNpi") != null) b.pharmacyNpi(item.get("pharmacyNpi").toString());
        if (item.get("addressLine1") != null) b.addressLine1(item.get("addressLine1").toString());
        if (item.get("addressLine2") != null) b.addressLine2(item.get("addressLine2").toString());
        if (item.get("city") != null) b.city(item.get("city").toString());
        if (item.get("state") != null) b.state(item.get("state").toString());
        if (item.get("zip") != null) b.zip(item.get("zip").toString());
        if (item.get("phone") != null) b.phone(item.get("phone").toString());
        if (item.get("prescriptionCount") != null) b.prescriptionCount(((Number) item.get("prescriptionCount")).intValue());
        if (item.get("firstPrescriptionDate") instanceof java.time.LocalDate) b.firstPrescriptionDate((java.time.LocalDate) item.get("firstPrescriptionDate"));
        if (item.get("lastPrescriptionDate") instanceof java.time.LocalDate) b.lastPrescriptionDate((java.time.LocalDate) item.get("lastPrescriptionDate"));
        return b.build();
    }

    private PDMPQueryResponse.PrescriberInfo mapPrescriberInfo(Map<String, Object> item) {
        PDMPQueryResponse.PrescriberInfo.PrescriberInfoBuilder b = PDMPQueryResponse.PrescriberInfo.builder();
        if (item.get("prescriberName") != null) b.prescriberName(item.get("prescriberName").toString());
        if (item.get("prescriberNpi") != null) b.prescriberNpi(item.get("prescriberNpi").toString());
        if (item.get("deaNumber") != null) b.deaNumber(item.get("deaNumber").toString());
        if (item.get("specialty") != null) b.specialty(item.get("specialty").toString());
        if (item.get("address") != null) b.address(item.get("address").toString());
        if (item.get("prescriptionCount") != null) b.prescriptionCount(((Number) item.get("prescriptionCount")).intValue());
        if (item.get("firstPrescriptionDate") instanceof java.time.LocalDate) b.firstPrescriptionDate((java.time.LocalDate) item.get("firstPrescriptionDate"));
        if (item.get("lastPrescriptionDate") instanceof java.time.LocalDate) b.lastPrescriptionDate((java.time.LocalDate) item.get("lastPrescriptionDate"));
        return b.build();
    }
    
    /**
     * Map prescription history item from Map to DTO
     */
    private PDMPQueryResponse.PrescriptionHistoryItem mapHistoryItem(Map<String, Object> item) {
        PDMPQueryResponse.PrescriptionHistoryItem.PrescriptionHistoryItemBuilder builder = 
            PDMPQueryResponse.PrescriptionHistoryItem.builder();
        
        if (item.get("prescriptionId") != null) {
            builder.prescriptionId(item.get("prescriptionId").toString());
        }
        if (item.get("medicationName") != null) {
            builder.medicationName(item.get("medicationName").toString());
        }
        if (item.get("medicationCode") != null) {
            builder.medicationCode(item.get("medicationCode").toString());
        }
        if (item.get("dosageStrength") != null) {
            builder.dosageStrength(item.get("dosageStrength").toString());
        }
        if (item.get("dosageUnit") != null) {
            builder.dosageUnit(item.get("dosageUnit").toString());
        }
        if (item.get("quantity") != null) {
            builder.quantity(item.get("quantity").toString());
        }
        if (item.get("schedule") != null) {
            builder.schedule(item.get("schedule").toString());
        }
        if (item.get("prescribedDate") != null) {
            if (item.get("prescribedDate") instanceof LocalDate) {
                builder.prescribedDate((LocalDate) item.get("prescribedDate"));
            }
        }
        if (item.get("filledDate") != null) {
            if (item.get("filledDate") instanceof LocalDate) {
                builder.filledDate((LocalDate) item.get("filledDate"));
            }
        }
        if (item.get("prescriberName") != null) {
            builder.prescriberName(item.get("prescriberName").toString());
        }
        if (item.get("prescriberNpi") != null) {
            builder.prescriberNpi(item.get("prescriberNpi").toString());
        }
        if (item.get("pharmacyName") != null) {
            builder.pharmacyName(item.get("pharmacyName").toString());
        }
        if (item.get("pharmacyNpi") != null) {
            builder.pharmacyNpi(item.get("pharmacyNpi").toString());
        }
        if (item.get("status") != null) {
            builder.status(item.get("status").toString());
        }
        
        return builder.build();
    }
}
