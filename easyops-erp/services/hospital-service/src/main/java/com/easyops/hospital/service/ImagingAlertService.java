package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.ImagingAlertRequest;
import com.easyops.hospital.dto.response.ImagingAlertResponse;
import com.easyops.hospital.entity.ImagingCriticalFindingAlert;
import com.easyops.hospital.entity.ImagingStudy;
import com.easyops.hospital.repository.ImagingCriticalFindingAlertRepository;
import com.easyops.hospital.repository.ImagingStudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for imaging alerts and notifications.
 * Handles abnormal finding alerts, significant changes, critical findings, and report availability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingAlertService {
    
    private final ImagingCriticalFindingAlertRepository alertRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingNotificationService notificationService;
    
    /**
     * Create critical finding alert
     */
    @Transactional
    public ImagingAlertResponse createCriticalFindingAlert(ImagingAlertRequest request, UUID userId) {
        log.info("Creating critical finding alert for study: {}", request.getStudyId());
        
        ImagingStudy study = imagingStudyRepository.findById(request.getStudyId())
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + request.getStudyId()));
        
        ImagingCriticalFindingAlert alert = ImagingCriticalFindingAlert.builder()
            .study(study)
            .patient(study.getPatient())
            .orderId(study.getOrder().getOrderId())
            .alertStatus(ImagingCriticalFindingAlert.AlertStatus.PENDING)
            .alertPriority(request.getAlertPriority() != null 
                ? request.getAlertPriority() 
                : ImagingCriticalFindingAlert.AlertPriority.HIGH)
            .alertMessage(request.getAlertMessage())
            .findingKeywords(request.getFindingKeywords())
            .build();
        
        ImagingCriticalFindingAlert savedAlert = alertRepository.save(alert);
        
        // Send notification
        notificationService.sendCriticalFindingNotification(savedAlert);
        
        log.info("Created critical finding alert: {}", savedAlert.getAlertId());
        return mapToResponse(savedAlert);
    }
    
    /**
     * Create abnormal finding alert
     */
    @Transactional
    public ImagingAlertResponse createAbnormalFindingAlert(ImagingAlertRequest request, UUID userId) {
        log.info("Creating abnormal finding alert for study: {}", request.getStudyId());
        
        ImagingStudy study = imagingStudyRepository.findById(request.getStudyId())
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + request.getStudyId()));
        
        ImagingCriticalFindingAlert alert = ImagingCriticalFindingAlert.builder()
            .study(study)
            .patient(study.getPatient())
            .orderId(study.getOrder().getOrderId())
            .alertStatus(ImagingCriticalFindingAlert.AlertStatus.PENDING)
            .alertPriority(ImagingCriticalFindingAlert.AlertPriority.MEDIUM)
            .alertMessage("Abnormal finding detected: " + request.getAlertMessage())
            .findingKeywords(request.getFindingKeywords())
            .build();
        
        ImagingCriticalFindingAlert savedAlert = alertRepository.save(alert);
        
        // Send notification
        notificationService.sendAbnormalFindingNotification(savedAlert);
        
        log.info("Created abnormal finding alert: {}", savedAlert.getAlertId());
        return mapToResponse(savedAlert);
    }
    
    /**
     * Create significant change alert (compared to prior study)
     */
    @Transactional
    public ImagingAlertResponse createSignificantChangeAlert(UUID studyId, UUID priorStudyId, 
                                                             String changeDescription, UUID userId) {
        log.info("Creating significant change alert for study: {} compared to prior: {}", studyId, priorStudyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        ImagingStudy priorStudy = imagingStudyRepository.findById(priorStudyId)
            .orElseThrow(() -> new RuntimeException("Prior imaging study not found: " + priorStudyId));
        
        String alertMessage = String.format(
            "Significant change detected compared to prior study (%s): %s",
            priorStudy.getStudyDate().toLocalDate(),
            changeDescription
        );
        
        ImagingCriticalFindingAlert alert = ImagingCriticalFindingAlert.builder()
            .study(study)
            .patient(study.getPatient())
            .orderId(study.getOrder().getOrderId())
            .alertStatus(ImagingCriticalFindingAlert.AlertStatus.PENDING)
            .alertPriority(ImagingCriticalFindingAlert.AlertPriority.MEDIUM)
            .alertMessage(alertMessage)
            .findingKeywords("SIGNIFICANT_CHANGE")
            .build();
        
        ImagingCriticalFindingAlert savedAlert = alertRepository.save(alert);
        
        // Send notification
        notificationService.sendSignificantChangeNotification(savedAlert, priorStudy);
        
        log.info("Created significant change alert: {}", savedAlert.getAlertId());
        return mapToResponse(savedAlert);
    }
    
    /**
     * Create report availability notification
     */
    @Transactional
    public ImagingAlertResponse createReportAvailabilityNotification(UUID studyId, UUID userId) {
        log.info("Creating report availability notification for study: {}", studyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        ImagingCriticalFindingAlert alert = ImagingCriticalFindingAlert.builder()
            .study(study)
            .patient(study.getPatient())
            .orderId(study.getOrder().getOrderId())
            .alertStatus(ImagingCriticalFindingAlert.AlertStatus.NOTIFIED)
            .alertPriority(ImagingCriticalFindingAlert.AlertPriority.LOW)
            .alertMessage("Imaging study report is now available: " + study.getStudyName())
            .notificationSentDate(LocalDateTime.now())
            .notificationDelivered(true)
            .build();
        
        ImagingCriticalFindingAlert savedAlert = alertRepository.save(alert);
        
        // Send notification
        notificationService.sendReportAvailabilityNotification(savedAlert);
        
        log.info("Created report availability notification: {}", savedAlert.getAlertId());
        return mapToResponse(savedAlert);
    }
    
    /**
     * Get all alerts for a patient
     */
    public List<ImagingAlertResponse> getAlertsByPatient(UUID patientId) {
        List<ImagingCriticalFindingAlert> alerts = alertRepository.findByPatientPatientId(patientId);
        return alerts.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unacknowledged alerts for a patient
     */
    public List<ImagingAlertResponse> getUnacknowledgedAlertsByPatient(UUID patientId) {
        List<ImagingCriticalFindingAlert> alerts = alertRepository.findUnacknowledgedAlertsByPatient(patientId);
        return alerts.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all unacknowledged alerts
     */
    public List<ImagingAlertResponse> getAllUnacknowledgedAlerts() {
        List<ImagingCriticalFindingAlert> alerts = alertRepository.findAllUnacknowledgedAlerts();
        return alerts.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Acknowledge alert
     */
    @Transactional
    public ImagingAlertResponse acknowledgeAlert(UUID alertId, String acknowledgmentNotes, UUID userId) {
        log.info("Acknowledging alert: {}", alertId);
        
        ImagingCriticalFindingAlert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedDate(LocalDateTime.now());
        alert.setAcknowledgmentNotes(acknowledgmentNotes);
        alert.setAlertStatus(ImagingCriticalFindingAlert.AlertStatus.ACKNOWLEDGED);
        
        ImagingCriticalFindingAlert savedAlert = alertRepository.save(alert);
        
        log.info("Acknowledged alert: {}", alertId);
        return mapToResponse(savedAlert);
    }
    
    /**
     * Map to response DTO
     */
    private ImagingAlertResponse mapToResponse(ImagingCriticalFindingAlert alert) {
        return ImagingAlertResponse.builder()
            .alertId(alert.getAlertId())
            .studyId(alert.getStudy().getStudyId())
            .patientId(alert.getPatient().getPatientId())
            .orderId(alert.getOrderId())
            .alertStatus(alert.getAlertStatus())
            .alertPriority(alert.getAlertPriority())
            .alertMessage(alert.getAlertMessage())
            .findingKeywords(alert.getFindingKeywords())
            .isAcknowledged(alert.getIsAcknowledged())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .acknowledgedDate(alert.getAcknowledgedDate())
            .acknowledgmentNotes(alert.getAcknowledgmentNotes())
            .notificationSentDate(alert.getNotificationSentDate())
            .notificationDelivered(alert.getNotificationDelivered())
            .createdAt(alert.getCreatedAt())
            .build();
    }
}
