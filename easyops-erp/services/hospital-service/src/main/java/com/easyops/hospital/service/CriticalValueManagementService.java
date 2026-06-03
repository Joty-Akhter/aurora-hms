package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.CriticalValueAcknowledgmentRequest;
import com.easyops.hospital.dto.request.CriticalValueEscalationRequest;
import com.easyops.hospital.dto.response.CriticalValueAlertResponse;
import com.easyops.hospital.dto.response.CriticalValueDocumentationResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CriticalValueManagementService {
    
    private final LabResultRepository labResultRepository;
    private final LabCriticalValueAlertRepository labCriticalValueAlertRepository;
    private final LabOrderRepository labOrderRepository;
    private final PatientRepository patientRepository;
    private final LabResultHistoryRepository labResultHistoryRepository;
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Value("${notification.service.url:http://notification-service}")
    private String notificationServiceUrl;
    
    @Value("${critical-value.escalation.timeout-minutes:15}")
    private int escalationTimeoutMinutes;
    
    @Value("${critical-value.max-escalation-levels:3}")
    private int maxEscalationLevels;
    
    // ========== Critical Value Detection ==========
    
    /**
     * Enhanced critical value detection with configurable thresholds
     */
    public boolean detectCriticalValue(LabResult result) {
        if (result.getResultType() != LabResult.ResultType.NUMERIC) {
            return false;
        }
        
        if (result.getIsPanicValue() != null && result.getIsPanicValue()) {
            return true;
        }
        
        if (result.getAbnormalFlag() == LabResult.AbnormalFlag.C) {
            return true;
        }
        
        if (result.getResultValueNumeric() == null || 
            result.getReferenceRangeLow() == null || 
            result.getReferenceRangeHigh() == null) {
            return false;
        }
        
        BigDecimal value = result.getResultValueNumeric();
        BigDecimal low = result.getReferenceRangeLow();
        BigDecimal high = result.getReferenceRangeHigh();
        BigDecimal range = high.subtract(low);
        
        // Critical thresholds based on test type
        String loincCode = result.getLoincCode();
        String testCategory = result.getTestCategory();
        
        // Critical if value is more than 50% outside reference range (default)
        BigDecimal criticalLowThreshold = low.subtract(range.multiply(BigDecimal.valueOf(0.5)));
        BigDecimal criticalHighThreshold = high.add(range.multiply(BigDecimal.valueOf(0.5)));
        
        // Special handling for specific test types
        if (isPotassiumTest(loincCode)) {
            // Potassium: critical if < 2.5 or > 6.5 mmol/L
            criticalLowThreshold = BigDecimal.valueOf(2.5);
            criticalHighThreshold = BigDecimal.valueOf(6.5);
        } else if (isSodiumTest(loincCode)) {
            // Sodium: critical if < 120 or > 160 mmol/L
            criticalLowThreshold = BigDecimal.valueOf(120);
            criticalHighThreshold = BigDecimal.valueOf(160);
        } else if (isGlucoseTest(loincCode)) {
            // Glucose: critical if < 40 or > 400 mg/dL
            criticalLowThreshold = BigDecimal.valueOf(40);
            criticalHighThreshold = BigDecimal.valueOf(400);
        } else if (isHemoglobinTest(loincCode)) {
            // Hemoglobin: critical if < 7 or > 20 g/dL
            criticalLowThreshold = BigDecimal.valueOf(7);
            criticalHighThreshold = BigDecimal.valueOf(20);
        }
        
        return value.compareTo(criticalLowThreshold) < 0 || value.compareTo(criticalHighThreshold) > 0;
    }
    
    private boolean isPotassiumTest(String loincCode) {
        return loincCode != null && (loincCode.contains("2823-3") || loincCode.contains("K"));
    }
    
    private boolean isSodiumTest(String loincCode) {
        return loincCode != null && (loincCode.contains("2951-2") || loincCode.contains("NA"));
    }
    
    private boolean isGlucoseTest(String loincCode) {
        return loincCode != null && (loincCode.contains("2339-0") || loincCode.contains("GLU"));
    }
    
    private boolean isHemoglobinTest(String loincCode) {
        return loincCode != null && (loincCode.contains("718-7") || loincCode.contains("HGB"));
    }
    
    // ========== Critical Value Alerts and Notifications ==========
    
    /**
     * Create critical value alert and send notifications
     */
    @Transactional
    public LabCriticalValueAlert createCriticalValueAlert(LabResult result, UUID userId) {
        log.info("Creating critical value alert for result: {}", result.getResultId());
        
        LabOrder order = result.getOrder();
        Patient patient = result.getPatient();
        
        // Check if alert already exists
        List<LabCriticalValueAlert> existingAlerts = labCriticalValueAlertRepository
            .findByResultResultId(result.getResultId());
        if (!existingAlerts.isEmpty()) {
            log.warn("Critical value alert already exists for result: {}", result.getResultId());
            return existingAlerts.get(0);
        }
        
        // Create alert
        LabCriticalValueAlert alert = LabCriticalValueAlert.builder()
            .result(result)
            .patient(patient)
            .orderId(order.getOrderId())
            .alertStatus(LabCriticalValueAlert.AlertStatus.PENDING)
            .alertPriority(LabCriticalValueAlert.AlertPriority.CRITICAL)
            .alertMessage(buildAlertMessage(result, patient))
            .notifiedProviderId(order.getOrderingProviderId())
            .notifiedProviderName(order.getOrderingProviderName())
            .isAcknowledged(false)
            .escalationLevel(0)
            .build();
        
        alert = labCriticalValueAlertRepository.save(alert);
        
        // Send notifications
        sendCriticalValueNotifications(alert, result, order, patient);
        
        // Update result
        result.setIsCriticalValue(true);
        labResultRepository.save(result);
        
        log.info("Created critical value alert: {} for patient: {}", alert.getAlertId(), patient.getPatientId());
        return alert;
    }
    
    /**
     * Send notifications for critical value alert
     */
    private void sendCriticalValueNotifications(LabCriticalValueAlert alert, LabResult result, 
                                              LabOrder order, Patient patient) {
        try {
            UUID providerId = order.getOrderingProviderId();
            if (providerId == null) {
                log.warn("No provider ID found for order: {}", order.getOrderId());
                return;
            }
            
            // Send in-app notification
            sendInAppNotification(alert, providerId, result, patient);
            
            // Send email notification (if configured)
            sendEmailNotification(alert, providerId, result, patient);
            
            // Update alert status
            alert.setAlertStatus(LabCriticalValueAlert.AlertStatus.NOTIFIED);
            alert.setNotificationSentDate(LocalDateTime.now());
            alert.setNotificationMethod("IN_APP,EMAIL");
            labCriticalValueAlertRepository.save(alert);
            
        } catch (Exception e) {
            log.error("Failed to send critical value notifications", e);
        }
    }
    
    private void sendInAppNotification(LabCriticalValueAlert alert, UUID providerId, 
                                      LabResult result, Patient patient) {
        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", providerId);
            notificationRequest.put("organizationId", result.getOrganizationId());
            notificationRequest.put("type", "CRITICAL_VALUE_ALERT");
            notificationRequest.put("title", "Critical Lab Value Alert");
            notificationRequest.put("message", alert.getAlertMessage());
            notificationRequest.put("priority", "CRITICAL");
            notificationRequest.put("actionUrl", "/lab-results/" + result.getResultId());
            notificationRequest.put("actionLabel", "View Result");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("resultId", result.getResultId());
            metadata.put("patientId", patient.getPatientId());
            metadata.put("patientName", PatientDisplayName.of(patient));
            metadata.put("testName", result.getTestName());
            metadata.put("resultValue", result.getResultValue());
            metadata.put("alertId", alert.getAlertId());
            notificationRequest.put("metadata", metadata);
            
            try {
                if (restTemplate != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);
                    restTemplate.postForObject(
                        notificationServiceUrl + "/api/notifications",
                        entity,
                        Void.class
                    );
                    log.info("Sent in-app notification for critical value alert");
                } else {
                    log.warn("RestTemplate not available, skipping notification");
                }
            } catch (Exception e) {
                log.error("Failed to send in-app notification", e);
            }
                
        } catch (Exception e) {
            log.error("Error sending in-app notification", e);
        }
    }
    
    private void sendEmailNotification(LabCriticalValueAlert alert, UUID providerId, 
                                      LabResult result, Patient patient) {
        try {
            // This would typically fetch provider email from user service
            // For now, we'll just log it
            log.info("Email notification would be sent to provider: {} for critical value: {}", 
                providerId, result.getResultId());
            
            // In a real implementation, you would:
            // 1. Fetch provider email from user service
            // 2. Send email via notification service
            // 3. Update alert with email delivery status
            
        } catch (Exception e) {
            log.error("Error sending email notification", e);
        }
    }
    
    private String buildAlertMessage(LabResult result, Patient patient) {
        return String.format(
            "CRITICAL VALUE ALERT: Patient %s (MRN: %s) - Test: %s = %s %s (Reference: %s-%s %s)",
            PatientDisplayName.of(patient),
            patient.getMrn(),
            result.getTestName(),
            result.getResultValue(),
            result.getResultUnits(),
            result.getReferenceRangeLow(),
            result.getReferenceRangeHigh(),
            result.getReferenceRangeUnits()
        );
    }
    
    // ========== Critical Value Acknowledgment Workflow ==========
    
    /**
     * Acknowledge critical value with response
     */
    @Transactional
    public LabCriticalValueAlert acknowledgeCriticalValue(UUID alertId, 
                                                          CriticalValueAcknowledgmentRequest request,
                                                          UUID userId) {
        log.info("Acknowledging critical value alert: {}", alertId);
        
        LabCriticalValueAlert alert = labCriticalValueAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Critical value alert not found: " + alertId));
        
        if (alert.getIsAcknowledged()) {
            throw new RuntimeException("Alert already acknowledged");
        }
        
        // Update alert
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedDate(LocalDateTime.now());
        alert.setAcknowledgmentNotes(request.getAcknowledgmentNotes());
        alert.setProviderResponse(request.getProviderResponse());
        alert.setAlertStatus(LabCriticalValueAlert.AlertStatus.ACKNOWLEDGED);
        
        alert = labCriticalValueAlertRepository.save(alert);
        
        // Update result
        LabResult result = alert.getResult();
        result.setIsCriticalValueAcknowledged(true);
        result.setCriticalValueAcknowledgedBy(userId);
        result.setCriticalValueAcknowledgedDate(LocalDateTime.now());
        result.setCriticalValueResponse(request.getProviderResponse());
        labResultRepository.save(result);
        
        // Create documentation
        createAcknowledgmentDocumentation(alert, result, request, userId);
        
        log.info("Acknowledged critical value alert: {}", alertId);
        return alert;
    }
    
    // ========== Critical Value Documentation ==========
    
    /**
     * Create documentation for critical value acknowledgment
     */
    private void createAcknowledgmentDocumentation(LabCriticalValueAlert alert, LabResult result,
                                                  CriticalValueAcknowledgmentRequest request, UUID userId) {
        LabResultHistory history = LabResultHistory.builder()
            .result(result)
            .changeType(LabResultHistory.ChangeType.ACKNOWLEDGED)
            .changedBy(userId)
            .fieldName("critical_value_acknowledgment")
            .previousValue("false")
            .newValue("true")
            .changeReason("Critical value acknowledged")
            .notes(String.format(
                "Response: %s. Notes: %s",
                request.getProviderResponse(),
                request.getAcknowledgmentNotes() != null ? request.getAcknowledgmentNotes() : "None"
            ))
            .build();
        
        labResultHistoryRepository.save(history);
    }
    
    /**
     * Get critical value documentation
     */
    public CriticalValueDocumentationResponse getCriticalValueDocumentation(UUID resultId) {
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        if (!result.getIsCriticalValue()) {
            throw new RuntimeException("Result is not a critical value");
        }
        
        List<LabCriticalValueAlert> alerts = labCriticalValueAlertRepository
            .findByResultResultId(resultId);
        
        List<LabResultHistory> history = labResultHistoryRepository
            .findByResultResultIdOrderByChangedDateDesc(resultId);
        
        return CriticalValueDocumentationResponse.builder()
            .resultId(resultId)
            .testName(result.getTestName())
            .resultValue(result.getResultValue())
            .resultUnits(result.getResultUnits())
            .criticalValueDetectedAt(result.getCreatedAt())
            .isAcknowledged(result.getIsCriticalValueAcknowledged())
            .acknowledgedBy(result.getCriticalValueAcknowledgedBy())
            .acknowledgedDate(result.getCriticalValueAcknowledgedDate())
            .providerResponse(result.getCriticalValueResponse())
            .alertHistory(alerts.stream()
                .map(this::mapAlertToResponse)
                .collect(Collectors.toList()))
            .documentationHistory(history.stream()
                .filter(h -> h.getChangeType() == LabResultHistory.ChangeType.ACKNOWLEDGED ||
                           h.getChangeType() == LabResultHistory.ChangeType.CREATED)
                .map(this::mapHistoryToDocumentation)
                .collect(Collectors.toList()))
            .build();
    }
    
    // ========== Escalation Procedures ==========
    
    /**
     * Escalate critical value alert
     */
    @Transactional
    public LabCriticalValueAlert escalateCriticalValue(UUID alertId, 
                                                      CriticalValueEscalationRequest request,
                                                      UUID userId) {
        log.info("Escalating critical value alert: {}", alertId);
        
        LabCriticalValueAlert alert = labCriticalValueAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Critical value alert not found: " + alertId));
        
        if (alert.getEscalationLevel() >= maxEscalationLevels) {
            throw new RuntimeException("Maximum escalation level reached");
        }
        
        // Increment escalation level
        int newEscalationLevel = alert.getEscalationLevel() + 1;
        alert.setEscalationLevel(newEscalationLevel);
        alert.setEscalatedTo(request.getEscalatedToUserId());
        alert.setEscalationDate(LocalDateTime.now());
        alert.setEscalationReason(request.getEscalationReason());
        alert.setAlertStatus(LabCriticalValueAlert.AlertStatus.ESCALATED);
        
        alert = labCriticalValueAlertRepository.save(alert);
        
        // Send notification to escalated user
        sendEscalationNotification(alert, request.getEscalatedToUserId());
        
        // Create documentation
        createEscalationDocumentation(alert, request, userId);
        
        log.info("Escalated critical value alert: {} to level: {}", alertId, newEscalationLevel);
        return alert;
    }
    
    /**
     * Check and auto-escalate unacknowledged alerts
     */
    @Transactional
    public void checkAndEscalateUnacknowledgedAlerts() {
        log.info("Checking for unacknowledged critical value alerts to escalate");
        
        List<LabCriticalValueAlert> unacknowledgedAlerts = labCriticalValueAlertRepository
            .findUnacknowledgedAlerts();
        
        LocalDateTime now = LocalDateTime.now();
        
        for (LabCriticalValueAlert alert : unacknowledgedAlerts) {
            Duration timeSinceCreation = Duration.between(alert.getCreatedAt(), now);
            long minutesSinceCreation = timeSinceCreation.toMinutes();
            
            // Check if escalation timeout has passed
            if (minutesSinceCreation >= escalationTimeoutMinutes && 
                alert.getEscalationLevel() < maxEscalationLevels) {
                
                // Auto-escalate to supervisor or on-call provider
                UUID escalatedTo = determineEscalationTarget(alert);
                
                if (escalatedTo != null) {
                    CriticalValueEscalationRequest escalationRequest = 
                        CriticalValueEscalationRequest.builder()
                            .escalatedToUserId(escalatedTo)
                            .escalationReason("Auto-escalated: Alert not acknowledged within " + 
                                escalationTimeoutMinutes + " minutes")
                            .build();
                    
                    escalateCriticalValue(alert.getAlertId(), escalationRequest, null);
                    log.info("Auto-escalated alert: {} after {} minutes", 
                        alert.getAlertId(), minutesSinceCreation);
                }
            }
        }
    }
    
    private UUID determineEscalationTarget(LabCriticalValueAlert alert) {
        // In a real implementation, this would:
        // 1. Get the ordering provider's supervisor
        // 2. Get the on-call provider
        // 3. Get the department head
        // For now, return null (would need user service integration)
        return null;
    }
    
    private void sendEscalationNotification(LabCriticalValueAlert alert, UUID escalatedToUserId) {
        try {
            LabResult result = alert.getResult();
            Patient patient = alert.getPatient();
            
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", escalatedToUserId);
            notificationRequest.put("organizationId", result.getOrganizationId());
            notificationRequest.put("type", "CRITICAL_VALUE_ESCALATION");
            notificationRequest.put("title", "Critical Value Escalation");
            notificationRequest.put("message", 
                "Critical value alert escalated: " + alert.getAlertMessage());
            notificationRequest.put("priority", "CRITICAL");
            notificationRequest.put("actionUrl", "/lab-results/" + result.getResultId());
            notificationRequest.put("actionLabel", "View Result");
            
            try {
                if (restTemplate != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);
                    restTemplate.postForObject(
                        notificationServiceUrl + "/api/notifications",
                        entity,
                        Void.class
                    );
                    log.info("Sent escalation notification");
                } else {
                    log.warn("RestTemplate not available, skipping notification");
                }
            } catch (Exception e) {
                log.error("Failed to send escalation notification", e);
            }
                
        } catch (Exception e) {
            log.error("Error sending escalation notification", e);
        }
    }
    
    private void createEscalationDocumentation(LabCriticalValueAlert alert,
                                              CriticalValueEscalationRequest request, UUID userId) {
        LabResult result = alert.getResult();
        LabResultHistory history = LabResultHistory.builder()
            .result(result)
            .changeType(LabResultHistory.ChangeType.UPDATED)
            .changedBy(userId)
            .fieldName("critical_value_escalation")
            .previousValue("Level " + (alert.getEscalationLevel() - 1))
            .newValue("Level " + alert.getEscalationLevel())
            .changeReason("Critical value escalated")
            .notes(String.format(
                "Escalated to level %d. Escalated to: %s. Reason: %s",
                alert.getEscalationLevel(),
                request.getEscalatedToUserId(),
                request.getEscalationReason()
            ))
            .build();
        
        labResultHistoryRepository.save(history);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Get all critical value alerts
     */
    public List<CriticalValueAlertResponse> getAllCriticalValueAlerts() {
        return labCriticalValueAlertRepository.findAll().stream()
            .map(this::mapAlertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unacknowledged critical value alerts
     */
    public List<CriticalValueAlertResponse> getUnacknowledgedAlerts() {
        return labCriticalValueAlertRepository.findUnacknowledgedAlerts().stream()
            .map(this::mapAlertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get critical value alerts for a patient
     */
    public List<CriticalValueAlertResponse> getAlertsByPatient(UUID patientId) {
        return labCriticalValueAlertRepository.findByPatientPatientId(patientId).stream()
            .map(this::mapAlertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get critical value alerts for a provider
     */
    public List<CriticalValueAlertResponse> getAlertsByProvider(UUID providerId) {
        return labCriticalValueAlertRepository.findUnacknowledgedAlertsByProvider(providerId).stream()
            .map(this::mapAlertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get critical value alert by ID
     */
    public CriticalValueAlertResponse getAlertById(UUID alertId) {
        LabCriticalValueAlert alert = labCriticalValueAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Critical value alert not found: " + alertId));
        return mapAlertToResponse(alert);
    }
    
    // ========== Helper Methods ==========
    
    private CriticalValueAlertResponse mapAlertToResponse(LabCriticalValueAlert alert) {
        LabResult result = alert.getResult();
        Patient patient = alert.getPatient();
        
        return CriticalValueAlertResponse.builder()
            .alertId(alert.getAlertId())
            .resultId(result.getResultId())
            .resultNumber(result.getResultNumber())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .testName(result.getTestName())
            .loincCode(result.getLoincCode())
            .resultValue(result.getResultValue())
            .resultUnits(result.getResultUnits())
            .referenceRangeLow(result.getReferenceRangeLow())
            .referenceRangeHigh(result.getReferenceRangeHigh())
            .alertMessage(alert.getAlertMessage())
            .alertStatus(alert.getAlertStatus())
            .alertPriority(alert.getAlertPriority())
            .notifiedProviderId(alert.getNotifiedProviderId())
            .notifiedProviderName(alert.getNotifiedProviderName())
            .isAcknowledged(alert.getIsAcknowledged())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .acknowledgedDate(alert.getAcknowledgedDate())
            .providerResponse(alert.getProviderResponse())
            .escalationLevel(alert.getEscalationLevel())
            .escalatedTo(alert.getEscalatedTo())
            .escalationDate(alert.getEscalationDate())
            .escalationReason(alert.getEscalationReason())
            .notificationSentDate(alert.getNotificationSentDate())
            .createdAt(alert.getCreatedAt())
            .build();
    }
    
    private CriticalValueDocumentationResponse.DocumentationEntry mapHistoryToDocumentation(
            LabResultHistory history) {
        String description = history.getChangeReason();
        if (history.getNotes() != null && !history.getNotes().isEmpty()) {
            description += ". " + history.getNotes();
        }
        return CriticalValueDocumentationResponse.DocumentationEntry.builder()
            .timestamp(history.getChangedDate())
            .action(history.getChangeType().toString())
            .performedBy(history.getChangedBy())
            .description(description)
            .build();
    }
}
