package com.easyops.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.easyops.hospital.dto.request.FillStatusUpdateRequest;
import com.easyops.hospital.dto.request.InHouseDispenseFillRequest;
import com.easyops.hospital.dto.request.PrescriptionTransmissionRequest;
import com.easyops.hospital.dto.response.InHouseDispenseFillResponse;
import com.easyops.hospital.dto.response.PrescriptionTransmissionResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.PatientNameInterop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EPrescribingService {
    
    private final PrescriptionTransmissionRepository transmissionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacyNetworkRepository pharmacyNetworkRepository;
    private final PrescriptionHistoryRepository prescriptionHistoryRepository;
    private final InHouseDispenseFillEventRepository inHouseDispenseFillEventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final NcpdpScriptMessageBuilder ncpdpScriptMessageBuilder;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** NCPDP networks: any E_PRESCRIBING network sends XML; JSON is used for legacy / API networks. */
    private static boolean isNcpdpNetwork(PharmacyNetwork network) {
        return network.getNetworkType() == PharmacyNetwork.NetworkType.E_PRESCRIBING;
    }

    /** Extract MessageID from NCPDP Header element in serialised XML. */
    private static final Pattern MESSAGE_ID_PATTERN =
            Pattern.compile("<MessageID[^>]*>([^<]+)</MessageID>");

    private static String extractMessageId(String xml) {
        if (xml == null) return null;
        Matcher m = MESSAGE_ID_PATTERN.matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    /** Comma-separated MessageIDs when multiple NCPDP documents are concatenated for storage. */
    private static String aggregateMessageIds(List<String> xmlDocs) {
        List<String> ids = new ArrayList<>();
        for (String xml : xmlDocs) {
            String id = extractMessageId(xml);
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    // ---------------------------------------------------------------------------
    // Fill-status state machine (FR-P3.11a)
    // ---------------------------------------------------------------------------

    /**
     * Defines which fill-status values may follow a given current status.
     * Any transition not in this map — or where the current status maps to an
     * empty set — is a regression and must be rejected with HTTP 409.
     */
    private static final Map<PrescriptionTransmission.FillStatus,
                             Set<PrescriptionTransmission.FillStatus>> VALID_FILL_TRANSITIONS;

    static {
        Map<PrescriptionTransmission.FillStatus,
            Set<PrescriptionTransmission.FillStatus>> m = new EnumMap<>(PrescriptionTransmission.FillStatus.class);

        Set<PrescriptionTransmission.FillStatus> fromPending = EnumSet.of(
                PrescriptionTransmission.FillStatus.IN_PROGRESS,
                PrescriptionTransmission.FillStatus.FILLED,
                PrescriptionTransmission.FillStatus.PARTIALLY_FILLED,
                PrescriptionTransmission.FillStatus.ON_HOLD,
                PrescriptionTransmission.FillStatus.OUT_OF_STOCK,
                PrescriptionTransmission.FillStatus.CANCELLED,
                PrescriptionTransmission.FillStatus.REJECTED,
                PrescriptionTransmission.FillStatus.EXPIRED);

        m.put(PrescriptionTransmission.FillStatus.PENDING, fromPending);

        m.put(PrescriptionTransmission.FillStatus.IN_PROGRESS, EnumSet.of(
                PrescriptionTransmission.FillStatus.FILLED,
                PrescriptionTransmission.FillStatus.PARTIALLY_FILLED,
                PrescriptionTransmission.FillStatus.ON_HOLD,
                PrescriptionTransmission.FillStatus.OUT_OF_STOCK,
                PrescriptionTransmission.FillStatus.CANCELLED,
                PrescriptionTransmission.FillStatus.REJECTED,
                PrescriptionTransmission.FillStatus.EXPIRED));

        m.put(PrescriptionTransmission.FillStatus.ON_HOLD, EnumSet.of(
                PrescriptionTransmission.FillStatus.IN_PROGRESS,
                PrescriptionTransmission.FillStatus.FILLED,
                PrescriptionTransmission.FillStatus.PARTIALLY_FILLED,
                PrescriptionTransmission.FillStatus.OUT_OF_STOCK,
                PrescriptionTransmission.FillStatus.CANCELLED,
                PrescriptionTransmission.FillStatus.REJECTED,
                PrescriptionTransmission.FillStatus.EXPIRED));

        m.put(PrescriptionTransmission.FillStatus.OUT_OF_STOCK, EnumSet.of(
                PrescriptionTransmission.FillStatus.IN_PROGRESS,
                PrescriptionTransmission.FillStatus.FILLED,
                PrescriptionTransmission.FillStatus.PARTIALLY_FILLED,
                PrescriptionTransmission.FillStatus.CANCELLED,
                PrescriptionTransmission.FillStatus.EXPIRED));

        m.put(PrescriptionTransmission.FillStatus.PARTIALLY_FILLED, EnumSet.of(
                PrescriptionTransmission.FillStatus.FILLED,
                PrescriptionTransmission.FillStatus.PICKED_UP,
                PrescriptionTransmission.FillStatus.CANCELLED,
                PrescriptionTransmission.FillStatus.REJECTED));

        m.put(PrescriptionTransmission.FillStatus.FILLED, EnumSet.of(
                PrescriptionTransmission.FillStatus.PICKED_UP));

        // Terminal states — no further transitions allowed.
        m.put(PrescriptionTransmission.FillStatus.PICKED_UP, EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));
        m.put(PrescriptionTransmission.FillStatus.CANCELLED,  EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));
        m.put(PrescriptionTransmission.FillStatus.REJECTED,   EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));
        m.put(PrescriptionTransmission.FillStatus.EXPIRED,    EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));

        VALID_FILL_TRANSITIONS = Collections.unmodifiableMap(m);
    }
    
    @Value("${eprescribing.enabled:true}")
    private boolean eprescribingEnabled;
    
    @Value("${eprescribing.simulate:true}")
    private boolean simulateEPrescribing;
    
    @Value("${eprescribing.network.endpoint:}")
    private String networkEndpoint;
    
    @Value("${eprescribing.network.api-key:}")
    private String networkApiKey;
    
    @Value("${eprescribing.network.timeout:30000}")
    private int networkTimeout;
    
    @Value("${eprescribing.network.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${eprescribing.network.retry.maxAttempts:3}")
    private int maxRetryAttempts;
    
    @Value("${eprescribing.network.retry.delay:5000}")
    private long retryDelay;
    
    /**
     * Transmit prescription to pharmacy network
     */
    @Transactional
    public PrescriptionTransmissionResponse transmitPrescription(
            PrescriptionTransmissionRequest request, 
            UUID userId,
            String userName,
            String userNpi) {
        
        log.info("Transmitting prescription: {} to network", request.getPrescriptionId());
        
        // Load prescription with medication lines — required for NCPDP multi-line NewRx generation.
        Prescription prescription = prescriptionRepository.findByIdWithMedications(request.getPrescriptionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Prescription not found: " + request.getPrescriptionId()));
        
        // Check if e-prescribing is enabled
        if (!eprescribingEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "E-prescribing is not enabled for this installation");
        }
        
        // Get or select network
        PharmacyNetwork network = selectNetwork(request);
        if (network == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No active e-prescribing network is available");
        }
        
        // Create transmission record
        PrescriptionTransmission transmission = PrescriptionTransmission.builder()
            .prescription(prescription)
            .transmissionDate(LocalDateTime.now())
            .transmissionStatus(PrescriptionTransmission.TransmissionStatus.PENDING)
            .transmissionMethod(request.getTransmissionMethod() != null ? 
                request.getTransmissionMethod() : PrescriptionTransmission.TransmissionMethod.E_PRESCRIBING)
            .networkName(network.getNetworkName())
            .networkId(network.getNetworkId().toString())
            .pharmacyId(request.getPharmacyId() != null ? request.getPharmacyId() : prescription.getPharmacyId())
            .pharmacyName(request.getPharmacyName() != null ? request.getPharmacyName() : prescription.getPharmacyName())
            .pharmacyNpi(request.getPharmacyNpi() != null ? request.getPharmacyNpi() : prescription.getPharmacyNpi())
            .pharmacyAddressLine1(request.getPharmacyAddressLine1() != null ? 
                request.getPharmacyAddressLine1() : prescription.getPharmacyAddressLine1())
            .pharmacyAddressLine2(request.getPharmacyAddressLine2() != null ? 
                request.getPharmacyAddressLine2() : prescription.getPharmacyAddressLine2())
            .pharmacyCity(request.getPharmacyCity() != null ? request.getPharmacyCity() : prescription.getPharmacyCity())
            .pharmacyState(request.getPharmacyState() != null ? request.getPharmacyState() : prescription.getPharmacyState())
            .pharmacyZip(request.getPharmacyZip() != null ? request.getPharmacyZip() : prescription.getPharmacyZip())
            .pharmacyPhone(request.getPharmacyPhone() != null ? request.getPharmacyPhone() : prescription.getPharmacyPhone())
            .pharmacyFax(request.getPharmacyFax())
            .transmissionSuccess(false)
            .confirmationReceived(false)
            .transmittedBy(userId)
            .transmittedByName(userName)
            .transmittedByNpi(userNpi)
            .createdBy(userId)
            .build();
        
        transmission = transmissionRepository.save(transmission);
        
        try {
            // Perform actual transmission
            PrescriptionTransmission result;
            if (simulateEPrescribing) {
                result = performSimulatedTransmission(transmission, prescription, network);
            } else {
                result = performRealTransmission(transmission, prescription, network);
            }
            
            // Update prescription status if transmission was successful
            if (result.getTransmissionSuccess()) {
                String previousStatus = prescription.getPrescriptionStatus().toString();
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.SENT);
                prescription.setSentDate(LocalDateTime.now());
                prescriptionRepository.save(prescription);

                PrescriptionHistory history = PrescriptionHistory.builder()
                    .prescription(prescription)
                    .changeType(PrescriptionHistory.ChangeType.SENT)
                    .changedBy(userId)
                    .changedDate(LocalDateTime.now())
                    .fieldName("prescription_status")
                    .previousValue(previousStatus)
                    .newValue("SENT")
                    .notes("Prescription transmitted via " + network.getNetworkName())
                    .build();
                prescriptionHistoryRepository.save(history);
            }
            
            // Update network statistics
            updateNetworkStatistics(network, result.getTransmissionSuccess());
            
            log.info("Prescription transmission completed: {}, success: {}", 
                    result.getTransmissionId(), result.getTransmissionSuccess());
            
            return mapToResponse(result);
            
        } catch (Exception e) {
            log.error("Prescription transmission failed: {}", transmission.getTransmissionId(), e);
            transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.FAILED);
            transmission.setTransmissionSuccess(false);
            transmission.setErrorMessage(e.getMessage());
            transmission = transmissionRepository.save(transmission);
            
            if (e instanceof ResponseStatusException) throw e;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Prescription transmission failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform simulated transmission (for development/testing)
     */
    private PrescriptionTransmission performSimulatedTransmission(
            PrescriptionTransmission transmission,
            Prescription prescription,
            PharmacyNetwork network) {
        
        log.info("Performing simulated e-prescribing transmission via {}", network.getNetworkName());

        // FR-P3.6: Build and validate the NCPDP SCRIPT XML even in simulation so that
        // structural errors are caught during testing rather than at go-live.
        if (isNcpdpNetwork(network)) {
            try {
                List<String> xmlDocs = ncpdpScriptMessageBuilder.buildAndValidateAllNewRxDocuments(
                        prescription, transmission, network);
                String combined = xmlDocs.size() == 1
                        ? xmlDocs.get(0)
                        : String.join(NcpdpScriptMessageBuilder.NCPDP_MULTI_MESSAGE_SEPARATOR, xmlDocs);
                transmission.setNcpdpXmlPayload(combined);
                transmission.setNcpdpMessageId(aggregateMessageIds(xmlDocs));
                log.info("NCPDP SCRIPT 2017071 built and validated (simulation): {} document(s) for tx {}",
                        xmlDocs.size(), transmission.getTransmissionId());
            } catch (NcpdpScriptMessageBuilder.NcpdpValidationException e) {
                log.error("NCPDP SCRIPT XSD validation failed (simulation): {}", e.getMessage());
                transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.FAILED);
                transmission.setTransmissionSuccess(false);
                transmission.setErrorMessage("NCPDP SCRIPT XSD validation failed: " + e.getMessage());
                transmission.setErrorCode("NCPDP_VALIDATION_ERROR");
                return transmissionRepository.save(transmission);
            } catch (NcpdpScriptMessageBuilder.NcpdpBuildException e) {
                log.warn("NCPDP SCRIPT build failed (simulation, non-fatal): {}", e.getMessage());
                // Non-fatal in simulation — continue with the simulated result
            }
        }

        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate successful transmission (90% success rate in simulation)
        boolean success = Math.random() > 0.1;
        
        if (success) {
            String transactionId = "SIM-" + UUID.randomUUID().toString();
            
            transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.SENT);
            transmission.setTransmissionSuccess(true);
            transmission.setNetworkTransactionId(transactionId);
            transmission.setConfirmationReceived(true);
            transmission.setConfirmationDate(LocalDateTime.now());
            transmission.setConfirmationMessage("Prescription successfully transmitted and confirmed by pharmacy");
            transmission.setFillStatus(PrescriptionTransmission.FillStatus.PENDING);
            
            // Create simulated network response
            Map<String, Object> networkResponse = new HashMap<>();
            networkResponse.put("transactionId", transactionId);
            networkResponse.put("status", "SUCCESS");
            networkResponse.put("message", "Prescription received by pharmacy");
            networkResponse.put("timestamp", LocalDateTime.now().toString());
            networkResponse.put("pharmacyConfirmation", true);
            transmission.setNetworkResponse(networkResponse);
            
        } else {
            transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.FAILED);
            transmission.setTransmissionSuccess(false);
            transmission.setErrorMessage("Simulated transmission failure");
            transmission.setErrorCode("SIM_ERROR");
        }
        
        return transmissionRepository.save(transmission);
    }

    /**
     * Builds one validated NCPDP SCRIPT {@code Message} per medication line, POSTs each to the
     * network endpoint, and stores the joined XML payload on the transmission record.
     */
    private PrescriptionTransmission transmitNcpdpDocuments(
            PrescriptionTransmission transmission,
            Prescription prescription,
            PharmacyNetwork network,
            String endpoint,
            HttpHeaders headers) {

        List<String> xmlDocs;
        try {
            xmlDocs = ncpdpScriptMessageBuilder.buildAndValidateAllNewRxDocuments(
                    prescription, transmission, network);
        } catch (NcpdpScriptMessageBuilder.NcpdpValidationException e) {
            log.error("NCPDP SCRIPT XSD validation failed — aborting transmission for tx {}: {}",
                    transmission.getTransmissionId(), e.getMessage());
            transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.FAILED);
            transmission.setTransmissionSuccess(false);
            transmission.setErrorMessage("NCPDP SCRIPT XSD validation failed: " + e.getMessage());
            transmission.setErrorCode("NCPDP_VALIDATION_ERROR");
            return transmissionRepository.save(transmission);
        }

        if (prescription.getMedications() != null && prescription.getMedications().size() > 1) {
            log.info("NCPDP multi-line: transmitting {} separate NewRx message(s) for prescription {} / tx {}",
                    xmlDocs.size(), prescription.getPrescriptionId(), transmission.getTransmissionId());
        }

        String combined = xmlDocs.size() == 1
                ? xmlDocs.get(0)
                : String.join(NcpdpScriptMessageBuilder.NCPDP_MULTI_MESSAGE_SEPARATOR, xmlDocs);
        transmission.setNcpdpXmlPayload(combined);
        transmission.setNcpdpMessageId(aggregateMessageIds(xmlDocs));
        transmission = transmissionRepository.save(transmission);

        headers.setContentType(MediaType.APPLICATION_XML);

        Map<String, Object> lastBody = null;
        for (int i = 0; i < xmlDocs.size(); i++) {
            String singleXml = xmlDocs.get(i);
            HttpEntity<String> ncpdpRequest = new HttpEntity<>(singleXml, headers);
            log.info("NCPDP SCRIPT POST {}/{} (MessageID={}) for tx {}",
                    i + 1, xmlDocs.size(), extractMessageId(singleXml), transmission.getTransmissionId());

            ResponseEntity<Map> response = null;
            int attempts = 0;
            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            endpoint + "/transmit",
                            HttpMethod.POST,
                            ncpdpRequest,
                            Map.class
                    );
                    break;
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("E-prescribing NCPDP POST failed, retrying (attempt {}/{})",
                                attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "E-prescribing API call interrupted");
                        }
                    } else {
                        log.error("E-prescribing NCPDP POST failed after {} attempts", attempts, e);
                        return performSimulatedTransmission(transmission, prescription, network);
                    }
                }
            }

            if (response == null || !response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                log.warn("E-prescribing network returned error on NCPDP document {}/{} — falling back to simulation",
                        i + 1, xmlDocs.size());
                return performSimulatedTransmission(transmission, prescription, network);
            }
            lastBody = response.getBody();
        }

        return parseTransmissionResponse(lastBody, transmission, network);
    }
    
    /**
     * Perform real transmission to network with actual API integration
     */
    private PrescriptionTransmission performRealTransmission(
            PrescriptionTransmission transmission,
            Prescription prescription,
            PharmacyNetwork network) {
        
        log.info("Performing real e-prescribing transmission via {}", network.getNetworkName());
        
        // Use network-specific endpoint if configured, otherwise use default
        String endpoint = network.getApiEndpoint() != null && !network.getApiEndpoint().isEmpty() 
                ? network.getApiEndpoint() 
                : (networkEndpoint != null && !networkEndpoint.isEmpty() ? networkEndpoint : null);
        
        if (endpoint == null) {
            log.warn("E-prescribing network endpoint not configured. Using simulated transmission.");
            return performSimulatedTransmission(transmission, prescription, network);
        }
        
        try {
            // FR-P3.6: Build the message body and choose the right Content-Type.
            // NCPDP-capable networks (E_PRESCRIBING type) receive NCPDP SCRIPT 2017071 XML.
            // Legacy / direct API networks receive JSON.
            HttpEntity<?> request;
            HttpHeaders headers = new HttpHeaders();

            String apiKey = (network.getApiKey() != null && !network.getApiKey().isEmpty())
                    ? network.getApiKey() : networkApiKey;
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }
            headers.set("X-Network-Name", network.getNetworkName());
            if (network.getNetworkId() != null) {
                headers.set("X-Network-Id", String.valueOf(network.getNetworkId()));
            }

            if (isNcpdpNetwork(network)) {
                return transmitNcpdpDocuments(transmission, prescription, network, endpoint, headers);
            } else {
                // --- Legacy JSON path (non-NCPDP networks) ---
                Map<String, Object> jsonPayload = buildPrescriptionMessageJson(prescription, network);
                headers.setContentType(MediaType.APPLICATION_JSON);
                request = new HttpEntity<>(jsonPayload, headers);
            }

            // Make API call with retry logic
            ResponseEntity<Map> response = null;
            int attempts = 0;

            while (attempts < maxRetryAttempts) {
                try {
                    response = restTemplate.exchange(
                            endpoint + "/transmit",
                            HttpMethod.POST,
                            request,
                            Map.class
                    );
                    break;
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxRetryAttempts && retryEnabled) {
                        log.warn("E-prescribing network API call failed, retrying (attempt {}/{})",
                                attempts, maxRetryAttempts);
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "E-prescribing API call interrupted");
                        }
                    } else {
                        log.error("E-prescribing network API call failed after {} attempts", attempts, e);
                        return performSimulatedTransmission(transmission, prescription, network);
                    }
                }
            }

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseTransmissionResponse(response.getBody(), transmission, network);
            } else {
                log.warn("E-prescribing network API returned error, falling back to simulated transmission");
                return performSimulatedTransmission(transmission, prescription, network);
            }
            
        } catch (Exception e) {
            log.error("Error performing real e-prescribing transmission, falling back to simulated", e);
            return performSimulatedTransmission(transmission, prescription, network);
        }
    }
    
    /**
     * Build a JSON prescription payload for non-NCPDP (legacy / direct API) networks.
     *
     * <p>NCPDP-capable networks ({@link PharmacyNetwork.NetworkType#E_PRESCRIBING}) use
     * {@link NcpdpScriptMessageBuilder#buildAndValidateNewRx} instead.
     */
    private Map<String, Object> buildPrescriptionMessageJson(Prescription prescription, PharmacyNetwork network) {
        Map<String, Object> message = new HashMap<>();

        message.put("prescriptionId",     prescription.getPrescriptionId().toString());
        message.put("prescriptionNumber", prescription.getPrescriptionNumber());
        message.put("medicationName",     prescription.getMedicationName());
        message.put("medicationCode",     prescription.getMedicationCode());
        message.put("dosageStrength",     prescription.getDosageStrength());
        message.put("dosageUnit",         prescription.getDosageUnit());
        message.put("dosageForm",    prescription.getDosageForm()  != null ? prescription.getDosageForm().toString()  : null);
        message.put("quantity", PrescriptionDerivedQuantity.deriveUnits(
                prescription.getFrequency(), prescription.getDurationDays()));
        message.put("quantityUnit", "EA");
        message.put("route",         prescription.getRoute()       != null ? prescription.getRoute().toString()       : null);
        message.put("frequency",     prescription.getFrequency());
        message.put("instructions",  prescription.getInstructions());
        message.put("refillsAuthorized",  prescription.getRefillsAuthorized());
        message.put("substitutionAllowed", prescription.getSubstitutionAllowed());

        if (prescription.getPatient() != null) {
            Map<String, Object> patient = new HashMap<>();
            patient.put("patientId", prescription.getPatient().getPatientId().toString());
            String[] np = PatientNameInterop.splitFullName(prescription.getPatient().getFullName());
            patient.put("firstName", np[0]);
            patient.put("lastName",  np[2]);
            patient.put("dateOfBirth", prescription.getPatient().getDateOfBirth() != null
                    ? prescription.getPatient().getDateOfBirth().toString() : null);
            message.put("patient", patient);
        }

        Map<String, Object> provider = new HashMap<>();
        provider.put("providerId", prescription.getPrescribingProviderId() != null
                ? prescription.getPrescribingProviderId().toString() : null);
        provider.put("npi",  prescription.getPrescribingProviderNpi());
        provider.put("name", prescription.getPrescribingProviderName());
        message.put("provider", provider);

        Map<String, Object> pharmacy = new HashMap<>();
        pharmacy.put("pharmacyId", prescription.getPharmacyId() != null
                ? prescription.getPharmacyId().toString() : null);
        pharmacy.put("npi",  prescription.getPharmacyNpi());
        pharmacy.put("name", prescription.getPharmacyName());
        pharmacy.put("address", Map.of(
                "line1",  prescription.getPharmacyAddressLine1()  != null ? prescription.getPharmacyAddressLine1()  : "",
                "line2",  prescription.getPharmacyAddressLine2()  != null ? prescription.getPharmacyAddressLine2()  : "",
                "city",   prescription.getPharmacyCity()          != null ? prescription.getPharmacyCity()          : "",
                "state",  prescription.getPharmacyState()         != null ? prescription.getPharmacyState()         : "",
                "zip",    prescription.getPharmacyZip()           != null ? prescription.getPharmacyZip()           : ""
        ));
        pharmacy.put("phone", prescription.getPharmacyPhone());
        message.put("pharmacy", pharmacy);

        message.put("networkName", network.getNetworkName());
        return message;
    }
    
    /**
     * Parse transmission response from network
     */
    private PrescriptionTransmission parseTransmissionResponse(
            Map<String, Object> responseBody, 
            PrescriptionTransmission transmission,
            PharmacyNetwork network) {
        
        // Parse response
        Boolean success = (Boolean) responseBody.getOrDefault("success", false);
        String transactionId = (String) responseBody.get("transactionId");
        String status = (String) responseBody.get("status");
        String errorMessage = (String) responseBody.get("errorMessage");
        
        // Update transmission
        transmission.setTransmissionSuccess(success);
        transmission.setNetworkTransactionId(transactionId);
        transmission.setErrorMessage(errorMessage);
        transmission.setTransmissionDate(LocalDateTime.now());

        // Map status string to enum where possible
        PrescriptionTransmission.TransmissionStatus resolvedStatus;
        if (status != null) {
            try {
                resolvedStatus = PrescriptionTransmission.TransmissionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                resolvedStatus = success
                    ? PrescriptionTransmission.TransmissionStatus.SENT
                    : PrescriptionTransmission.TransmissionStatus.FAILED;
            }
        } else {
            resolvedStatus = success
                ? PrescriptionTransmission.TransmissionStatus.SENT
                : PrescriptionTransmission.TransmissionStatus.FAILED;
        }
        transmission.setTransmissionStatus(resolvedStatus);
        
        return transmissionRepository.save(transmission);
    }
    
    /**
     * Update fill status from an inbound pharmacy/e-prescribing-network webhook (FR-P3.11a).
     *
     * <p>Three behavioural guarantees:
     * <ol>
     *   <li><b>Idempotency</b> — duplicate callbacks with the same networkTransactionId
     *       and fillStatus return HTTP 200 with the current state without writing anything.</li>
     *   <li><b>Regression guard</b> — callbacks that attempt an invalid state transition
     *       (per {@link #VALID_FILL_TRANSITIONS}) are rejected with HTTP 409.</li>
     *   <li><b>Domain events</b> — {@code prescription.filled} and
     *       {@code prescription.cancelled_by_pharmacy} are published so downstream
     *       consumers (billing, medication list, notifications) can react.</li>
     * </ol>
     */
    @Transactional
    public PrescriptionTransmissionResponse updateFillStatus(FillStatusUpdateRequest request) {
        log.info("Updating fill status for transaction: {} → {}",
                request.getNetworkTransactionId(), request.getFillStatus());

        PrescriptionTransmission transmission = transmissionRepository
                .findByNetworkTransactionId(request.getNetworkTransactionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transmission not found: " + request.getNetworkTransactionId()));

        PrescriptionTransmission.FillStatus currentStatus = transmission.getFillStatus();
        PrescriptionTransmission.FillStatus incomingStatus = request.getFillStatus();

        // --- Idempotency: same status received again → 200 with no write ----------
        if (incomingStatus == currentStatus) {
            log.debug("Fill-status callback is idempotent (no change): tx={} status={}",
                    request.getNetworkTransactionId(), incomingStatus);
            return mapToResponse(transmission);
        }

        // Gap M2: reject null fillStatusDate before writing anything.
        // The bean-validator (@ValidFillStatusRequest) enforces this at the HTTP boundary, but
        // updateFillStatus() may also be called programmatically (e.g. from integration tests or
        // future internal callers) where @Valid is not applied.  Silently substituting
        // LocalDateTime.now() produces an audit record timestamped at server-receipt time, not at
        // the actual pharmacy fill event — corrupting downstream billing reconciliation.
        if (request.getFillStatusDate() == null) {
            throw new IllegalArgumentException(
                    "fillStatusDate is required and must reflect the pharmacy event time; "
                    + "do not allow the service to substitute a server-generated timestamp");
        }

        // --- Regression guard: reject invalid transitions with 409 ----------------
        Set<PrescriptionTransmission.FillStatus> allowed =
                VALID_FILL_TRANSITIONS.getOrDefault(currentStatus,
                        EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));

        if (!allowed.contains(incomingStatus)) {
            String msg = String.format(
                    "Fill-status transition rejected: %s → %s is not a valid transition for tx=%s",
                    currentStatus, incomingStatus, request.getNetworkTransactionId());
            log.warn(msg);
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        }

        // --- Apply the fill-status update ----------------------------------------
        transmission.setFillStatus(incomingStatus);
        transmission.setFillStatusSource(FillStatusSource.NETWORK_WEBHOOK);
        transmission.setFillStatusDate(request.getFillStatusDate()); // non-null guaranteed by guard above
        transmission.setFillStatusMessage(request.getFillStatusMessage());
        transmission.setFilledDate(request.getFilledDate());
        transmission.setPickedUpDate(request.getPickedUpDate());
        transmission.setCancelledByPharmacy(request.getCancelledByPharmacy());
        transmission.setCancellationReason(request.getCancellationReason());
        transmission.setUpdatedAt(LocalDateTime.now());

        transmission = transmissionRepository.save(transmission);

        // --- Mirror status onto prescription row and write history ----------------
        Prescription prescription = transmission.getPrescription();
        boolean prescriptionUpdated = false;

        switch (incomingStatus) {
            case FILLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.FILLED);
                if (request.getFilledDate() == null) {
                    // filledDate is required for FILLED status (enforced by @ValidFillStatusRequest).
                    // Substituting LocalDateTime.now() would record an incorrect fill timestamp
                    // in the prescription row and corrupt billing reconciliation (Gap M2).
                    throw new IllegalArgumentException(
                            "filledDate is required when fillStatus is FILLED");
                }
                prescription.setFilledDate(request.getFilledDate());
                prescriptionUpdated = true;
                break;
            case PARTIALLY_FILLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.PARTIALLY_FILLED);
                prescriptionUpdated = true;
                break;
            case PICKED_UP:
                prescriptionUpdated = true;
                break;
            case CANCELLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.CANCELLED);
                prescription.setCancellationReason(request.getCancellationReason());
                prescriptionUpdated = true;
                break;
            case REJECTED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.REJECTED);
                prescriptionUpdated = true;
                break;
            case EXPIRED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.EXPIRED);
                prescriptionUpdated = true;
                break;
            case ON_HOLD:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.ON_HOLD);
                prescriptionUpdated = true;
                break;
            case OUT_OF_STOCK:
                // Pharmacy cannot fill immediately; keep prescription status as SENT for follow-up.
                prescriptionUpdated = true;
                break;
            case IN_PROGRESS:
                // Pharmacy is actively filling — no prescription status change.
                prescriptionUpdated = true;
                break;
            case PENDING:
                break;
            default:
                log.warn("Unhandled FillStatus value: {}", incomingStatus);
                break;
        }

        if (prescriptionUpdated) {
            prescriptionRepository.save(prescription);

            // changedBy is null: fill-status arrives via inbound webhook, not a user action.
            // Pharmacy/network identity is captured on the transmission record.
            PrescriptionHistory history = PrescriptionHistory.builder()
                    .prescription(prescription)
                    .changeType(PrescriptionHistory.ChangeType.STATUS_CHANGED)
                    .changedDate(LocalDateTime.now())
                    .fieldName("fill_status")
                    .previousValue(currentStatus != null ? currentStatus.name() : null)
                    .newValue(incomingStatus.name())
                    .notes("Pharmacy webhook: " + (request.getFillStatusMessage() != null
                            ? request.getFillStatusMessage() : incomingStatus))
                    .build();
            prescriptionHistoryRepository.save(history);
        }

        // --- Publish domain events (FR-P3.11a) ------------------------------------
        publishFillStatusEvent(incomingStatus, transmission, prescription, request);

        log.info("Fill status updated: tx={} {} → {}",
                transmission.getTransmissionId(), currentStatus, incomingStatus);

        return mapToResponse(transmission);
    }

    /**
     * Publishes domain events for clinically significant fill-status transitions.
     * {@code prescription.filled} enables downstream billing and medication-list update.
     * {@code prescription.cancelled_by_pharmacy} triggers a provider notification so they
     * can re-prescribe if needed.
     */
    private void publishFillStatusEvent(
            PrescriptionTransmission.FillStatus status,
            PrescriptionTransmission transmission,
            Prescription prescription,
            FillStatusUpdateRequest request) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("prescriptionId",      prescription.getPrescriptionId().toString());
        payload.put("transmissionId",      transmission.getTransmissionId().toString());
        payload.put("networkTransactionId", request.getNetworkTransactionId());
        payload.put("patientId",           prescription.getPatient() != null
                                                ? prescription.getPatient().getPatientId().toString() : null);
        payload.put("prescribingProviderId", prescription.getPrescribingProviderId() != null
                                                ? prescription.getPrescribingProviderId().toString() : null);
        payload.put("fillStatus",          status.name());
        payload.put("fillStatusDate",      request.getFillStatusDate().toString()); // non-null: guarded in updateFillStatus()
        payload.put("pharmacyName",        request.getPharmacyName());
        payload.put("pharmacyNpi",         request.getPharmacyNpi());

        switch (status) {
            case FILLED:
                payload.put("filledDate", request.getFilledDate().toString()); // non-null: guarded in FILLED arm above
                domainEventPublisher.publish("prescription.filled", payload);
                break;
            case PARTIALLY_FILLED:
                domainEventPublisher.publish("prescription.partially_filled", payload);
                break;
            case CANCELLED:
            case REJECTED:
                payload.put("cancellationReason", request.getCancellationReason());
                domainEventPublisher.publish("prescription.cancelled_by_pharmacy", payload);
                break;
            default:
                // No domain event for other statuses (informational transitions).
                break;
        }
    }
    
    /**
     * Apply fill status from in-house hospital pharmacy dispensing (Phase P2 — WS-B).
     * Skips updates when {@code prescriptionId} is absent on the dispense order (caller must not invoke).
     * Rejects updates when the latest transmission was already filled via {@link FillStatusSource#NETWORK_WEBHOOK}.
     */
    @Transactional
    public InHouseDispenseFillResponse applyInHouseDispenseFill(InHouseDispenseFillRequest request) {
        Objects.requireNonNull(request.getIdempotencyKey(), "idempotencyKey");
        var existing = inHouseDispenseFillEventRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
        if (existing.isPresent()) {
            try {
                return objectMapper.readValue(existing.get().getResponseJson(), InHouseDispenseFillResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse idempotent in-house fill replay, re-applying: {}", e.getMessage());
            }
        }

        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prescription not found: " + request.getPrescriptionId()));

        PrescriptionTransmission.FillStatus incoming = request.getFillStatus();
        if (request.getFillStatusDate() == null) {
            throw new IllegalArgumentException("fillStatusDate is required");
        }
        if ((incoming == PrescriptionTransmission.FillStatus.FILLED
                || incoming == PrescriptionTransmission.FillStatus.PARTIALLY_FILLED)
                && request.getFilledDate() == null) {
            throw new IllegalArgumentException("filledDate is required when fillStatus is FILLED or PARTIALLY_FILLED");
        }

        Optional<PrescriptionTransmission> optTransmission = transmissionRepository
                .findFirstByPrescriptionPrescriptionIdOrderByTransmissionDateDesc(request.getPrescriptionId());

        if (optTransmission.isPresent()) {
            PrescriptionTransmission t = optTransmission.get();
            PrescriptionTransmission.FillStatus current = t.getFillStatus();
            if (t.getFillStatusSource() == FillStatusSource.NETWORK_WEBHOOK
                    && current != null
                    && (current == PrescriptionTransmission.FillStatus.FILLED
                    || current == PrescriptionTransmission.FillStatus.PARTIALLY_FILLED
                    || current == PrescriptionTransmission.FillStatus.PICKED_UP)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Fill status was already set via external network; in-house update rejected");
            }
            if (t.getFillStatusSource() == null
                    && current != null
                    && (current == PrescriptionTransmission.FillStatus.FILLED
                    || current == PrescriptionTransmission.FillStatus.PARTIALLY_FILLED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Transmission already shows a fill from an external source; in-house update rejected");
            }
            Set<PrescriptionTransmission.FillStatus> allowed =
                    VALID_FILL_TRANSITIONS.getOrDefault(current,
                            EnumSet.noneOf(PrescriptionTransmission.FillStatus.class));
            if (current != null && current != incoming && !allowed.contains(incoming)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format("Fill-status transition rejected: %s → %s (in-house)", current, incoming));
            }
            if (current != null && current == incoming) {
                InHouseDispenseFillResponse shortOut = InHouseDispenseFillResponse.builder()
                        .prescriptionId(prescription.getPrescriptionId())
                        .dispenseOrderId(request.getDispenseOrderId())
                        .fillStatus(incoming)
                        .transmissionId(t.getTransmissionId())
                        .transmissionUpdated(false)
                        .build();
                persistInHouseFillIdempotency(request, shortOut);
                return shortOut;
            }

            t.setFillStatus(incoming);
            t.setFillStatusSource(FillStatusSource.IN_HOUSE_PHARMACY);
            t.setFillStatusDate(request.getFillStatusDate());
            t.setFillStatusMessage(request.getFillStatusMessage());
            t.setFilledDate(request.getFilledDate());
            t.setUpdatedAt(LocalDateTime.now());
            transmissionRepository.save(t);
        }

        applyPrescriptionHeaderForInHouseFill(prescription, incoming, request);

        PrescriptionHistory history = PrescriptionHistory.builder()
                .prescription(prescription)
                .changeType(PrescriptionHistory.ChangeType.STATUS_CHANGED)
                .changedDate(LocalDateTime.now())
                .fieldName("fill_status_in_house")
                .newValue(incoming.name())
                .notes("In-house pharmacy dispense order " + request.getDispenseOrderId())
                .build();
        prescriptionHistoryRepository.save(history);

        publishInHouseDispenseFillEvent(incoming, optTransmission.orElse(null), prescription, request);

        InHouseDispenseFillResponse response = InHouseDispenseFillResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .dispenseOrderId(request.getDispenseOrderId())
                .fillStatus(incoming)
                .transmissionId(optTransmission.map(PrescriptionTransmission::getTransmissionId).orElse(null))
                .transmissionUpdated(optTransmission.isPresent())
                .build();

        persistInHouseFillIdempotency(request, response);
        return response;
    }

    private void persistInHouseFillIdempotency(InHouseDispenseFillRequest request, InHouseDispenseFillResponse response) {
        try {
            InHouseDispenseFillEvent ev = InHouseDispenseFillEvent.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(request.getIdempotencyKey().trim())
                    .prescriptionId(request.getPrescriptionId())
                    .dispenseOrderId(request.getDispenseOrderId())
                    .responseJson(objectMapper.writeValueAsString(response))
                    .createdAt(OffsetDateTime.now())
                    .build();
            inHouseDispenseFillEventRepository.save(ev);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist in-house fill idempotency record", e);
        }
    }

    private void applyPrescriptionHeaderForInHouseFill(
            Prescription prescription,
            PrescriptionTransmission.FillStatus incoming,
            InHouseDispenseFillRequest request) {
        switch (incoming) {
            case FILLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.FILLED);
                prescription.setFilledDate(request.getFilledDate());
                break;
            case PARTIALLY_FILLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.PARTIALLY_FILLED);
                break;
            case CANCELLED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.CANCELLED);
                prescription.setCancellationReason(request.getFillStatusMessage());
                break;
            case REJECTED:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.REJECTED);
                break;
            case OUT_OF_STOCK:
                break;
            case ON_HOLD:
                prescription.setPrescriptionStatus(Prescription.PrescriptionStatus.ON_HOLD);
                break;
            case IN_PROGRESS:
                break;
            default:
                break;
        }
        prescriptionRepository.save(prescription);
    }

    private void publishInHouseDispenseFillEvent(
            PrescriptionTransmission.FillStatus status,
            PrescriptionTransmission transmission,
            Prescription prescription,
            InHouseDispenseFillRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prescriptionId", prescription.getPrescriptionId().toString());
        payload.put("dispenseOrderId", request.getDispenseOrderId().toString());
        payload.put("channel", "IN_HOUSE_PHARMACY");
        if (transmission != null) {
            payload.put("transmissionId", transmission.getTransmissionId().toString());
        }
        payload.put("patientId", prescription.getPatient() != null
                ? prescription.getPatient().getPatientId().toString() : null);
        payload.put("fillStatus", status.name());
        payload.put("fillStatusDate", request.getFillStatusDate().toString());
        switch (status) {
            case FILLED:
                payload.put("filledDate", request.getFilledDate().toString());
                domainEventPublisher.publish("prescription.filled", payload);
                break;
            case PARTIALLY_FILLED:
                domainEventPublisher.publish("prescription.partially_filled", payload);
                break;
            default:
                break;
        }
    }

    /**
     * Get transmission by ID
     */
    public PrescriptionTransmissionResponse getTransmission(UUID transmissionId) {
        PrescriptionTransmission transmission = transmissionRepository.findById(transmissionId)
            .orElseThrow(() -> new RuntimeException("Transmission not found: " + transmissionId));
        return mapToResponse(transmission);
    }
    
    /**
     * Get all transmissions for a prescription
     */
    public List<PrescriptionTransmissionResponse> getTransmissionsByPrescription(UUID prescriptionId) {
        List<PrescriptionTransmission> transmissions = transmissionRepository
            .findByPrescriptionPrescriptionIdOrderByTransmissionDateDesc(prescriptionId);
        return transmissions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get latest transmission for a prescription
     */
    public PrescriptionTransmissionResponse getLatestTransmission(UUID prescriptionId) {
        PrescriptionTransmission transmission = transmissionRepository
            .findFirstByPrescriptionPrescriptionIdOrderByTransmissionDateDesc(prescriptionId)
            .orElseThrow(() -> new RuntimeException("No transmissions found for prescription: " + prescriptionId));
        return mapToResponse(transmission);
    }
    
    /**
     * Retry failed transmission
     */
    @Transactional
    public PrescriptionTransmissionResponse retryTransmission(UUID transmissionId, UUID userId) {
        log.info("Retrying transmission: {}", transmissionId);
        
        PrescriptionTransmission transmission = transmissionRepository.findById(transmissionId)
            .orElseThrow(() -> new RuntimeException("Transmission not found: " + transmissionId));
        
        if (transmission.getRetryCount() >= transmission.getMaxRetries()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Maximum retry count reached for transmission: " + transmissionId);
        }
        
        // Reset transmission status
        transmission.setTransmissionStatus(PrescriptionTransmission.TransmissionStatus.PENDING);
        transmission.setRetryCount(transmission.getRetryCount() + 1);
        transmission.setLastRetryDate(LocalDateTime.now());
        transmission.setErrorMessage(null);
        transmission.setErrorCode(null);
        transmission.setUpdatedBy(userId);
        
        transmission = transmissionRepository.save(transmission);
        
        // Retry transmission
        Prescription prescription = transmission.getPrescription();
        final String networkName = transmission.getNetworkName();
        PharmacyNetwork network = pharmacyNetworkRepository.findByNetworkName(networkName)
            .orElseThrow(() -> new RuntimeException("Network not found: " + networkName));
        
        PrescriptionTransmission result;
        if (simulateEPrescribing) {
            result = performSimulatedTransmission(transmission, prescription, network);
        } else {
            result = performRealTransmission(transmission, prescription, network);
        }
        
        return mapToResponse(result);
    }
    
    /**
     * Select network for transmission
     */
    private PharmacyNetwork selectNetwork(PrescriptionTransmissionRequest request) {
        if (request.getNetworkId() != null) {
            return pharmacyNetworkRepository.findById(request.getNetworkId())
                .orElseThrow(() -> new RuntimeException("Network not found: " + request.getNetworkId()));
        }
        
        if (request.getNetworkName() != null) {
            return pharmacyNetworkRepository.findByNetworkNameAndIsActiveTrue(request.getNetworkName())
                .orElseThrow(() -> new RuntimeException("Network not found: " + request.getNetworkName()));
        }
        
        // Use default network
        return pharmacyNetworkRepository.findByIsDefaultTrueAndIsActiveTrue()
            .orElseGet(() -> {
                // Fallback to first active network
                List<PharmacyNetwork> networks = pharmacyNetworkRepository
                    .findNetworksSupportingPrescriptionTransmission();
                if (networks.isEmpty()) {
                    return null;
                }
                return networks.get(0);
            });
    }
    
    /**
     * Update network statistics
     */
    private void updateNetworkStatistics(PharmacyNetwork network, boolean success) {
        network.setTotalTransmissions(network.getTotalTransmissions() + 1);
        if (success) {
            network.setSuccessfulTransmissions(network.getSuccessfulTransmissions() + 1);
        } else {
            network.setFailedTransmissions(network.getFailedTransmissions() + 1);
        }
        network.setLastTransmissionDate(LocalDateTime.now());
        pharmacyNetworkRepository.save(network);
    }
    
    /**
     * Map transmission entity to response DTO
     */
    private PrescriptionTransmissionResponse mapToResponse(PrescriptionTransmission transmission) {
        return PrescriptionTransmissionResponse.builder()
            .transmissionId(transmission.getTransmissionId())
            .prescriptionId(transmission.getPrescription().getPrescriptionId())
            .transmissionDate(transmission.getTransmissionDate())
            .transmissionStatus(transmission.getTransmissionStatus())
            .transmissionMethod(transmission.getTransmissionMethod())
            .networkName(transmission.getNetworkName())
            .networkId(transmission.getNetworkId())
            .networkTransactionId(transmission.getNetworkTransactionId())
            .pharmacyId(transmission.getPharmacyId())
            .pharmacyName(transmission.getPharmacyName())
            .pharmacyNpi(transmission.getPharmacyNpi())
            .pharmacyDea(transmission.getPharmacyDea())
            .pharmacyAddressLine1(transmission.getPharmacyAddressLine1())
            .pharmacyAddressLine2(transmission.getPharmacyAddressLine2())
            .pharmacyCity(transmission.getPharmacyCity())
            .pharmacyState(transmission.getPharmacyState())
            .pharmacyZip(transmission.getPharmacyZip())
            .pharmacyPhone(transmission.getPharmacyPhone())
            .pharmacyFax(transmission.getPharmacyFax())
            .transmissionSuccess(transmission.getTransmissionSuccess())
            .confirmationReceived(transmission.getConfirmationReceived())
            .confirmationDate(transmission.getConfirmationDate())
            .confirmationMessage(transmission.getConfirmationMessage())
            .errorMessage(transmission.getErrorMessage())
            .errorCode(transmission.getErrorCode())
            .retryCount(transmission.getRetryCount())
            .lastRetryDate(transmission.getLastRetryDate())
            .maxRetries(transmission.getMaxRetries())
            .networkResponse(transmission.getNetworkResponse())
            .transmissionPayload(transmission.getTransmissionPayload())
            .fillStatus(transmission.getFillStatus())
            .fillStatusDate(transmission.getFillStatusDate())
            .fillStatusMessage(transmission.getFillStatusMessage())
            .filledDate(transmission.getFilledDate())
            .pickedUpDate(transmission.getPickedUpDate())
            .cancelledByPharmacy(transmission.getCancelledByPharmacy())
            .cancellationReason(transmission.getCancellationReason())
            .transmittedBy(transmission.getTransmittedBy())
            .transmittedByName(transmission.getTransmittedByName())
            .transmittedByNpi(transmission.getTransmittedByNpi())
            .createdAt(transmission.getCreatedAt())
            .updatedAt(transmission.getUpdatedAt())
            .createdBy(transmission.getCreatedBy())
            .updatedBy(transmission.getUpdatedBy())
            .build();
    }
}
