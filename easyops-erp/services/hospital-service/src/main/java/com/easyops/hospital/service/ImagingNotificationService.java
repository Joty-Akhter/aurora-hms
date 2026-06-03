package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingCriticalFindingAlert;
import com.easyops.hospital.entity.ImagingStudy;
import com.easyops.hospital.integration.communication.CommunicationSmsClient;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for sending imaging-related notifications.
 * Integrates with notification service for email, in-app, and other notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingNotificationService {

    private final CommunicationSmsClient communicationSmsClient;

    @Value("${imaging.notifications.enabled:true}")
    private boolean notificationsEnabled;
    
    /**
     * Send critical finding notification
     */
    public void sendCriticalFindingNotification(ImagingCriticalFindingAlert alert) {
        if (!notificationsEnabled) {
            log.debug("Notifications are disabled");
            return;
        }
        
        log.info("Sending critical finding notification for alert: {}", alert.getAlertId());
        
        ImagingStudy study = alert.getStudy();
        
        // In a real implementation, this would call the notification service
        // For now, we'll just log and update the alert
        String notificationMessage = String.format(
            "CRITICAL FINDING ALERT: %s - %s\nPatient: %s\nStudy: %s\nAccession: %s",
            alert.getAlertPriority(),
            alert.getAlertMessage(),
            PatientDisplayName.of(study.getPatient()),
            study.getStudyName(),
            study.getAccessionNumber()
        );
        
        log.info("Notification message: {}", notificationMessage);
        
        CommunicationSmsClient.SmsSendResult smsResult =
                communicationSmsClient.sendPatientNotification(study.getPatient(), notificationMessage);

        // Update alert with notification info
        alert.setNotificationSentDate(LocalDateTime.now());
        alert.setNotificationMethod(smsResult.getNotificationMethod());
        alert.setNotificationDelivered(smsResult.isDelivered());
        if (smsResult.isDelivered()) {
            alert.setAlertStatus(ImagingCriticalFindingAlert.AlertStatus.NOTIFIED);
        }
        log.info("Critical finding SMS result for alert {}: delivered={}, skipped={}, reason={}",
            alert.getAlertId(), smsResult.isDelivered(), smsResult.isSkipped(), smsResult.getReason());
    }
    
    /**
     * Send abnormal finding notification
     */
    public void sendAbnormalFindingNotification(ImagingCriticalFindingAlert alert) {
        if (!notificationsEnabled) {
            log.debug("Notifications are disabled");
            return;
        }
        
        log.info("Sending abnormal finding notification for alert: {}", alert.getAlertId());
        
        ImagingStudy study = alert.getStudy();
        
        String notificationMessage = String.format(
            "ABNORMAL FINDING: %s\nPatient: %s\nStudy: %s",
            alert.getAlertMessage(),
            PatientDisplayName.of(study.getPatient()),
            study.getStudyName()
        );
        
        log.info("Notification message: {}", notificationMessage);
        
        CommunicationSmsClient.SmsSendResult smsResult =
                communicationSmsClient.sendPatientNotification(study.getPatient(), notificationMessage);
        alert.setNotificationSentDate(LocalDateTime.now());
        alert.setNotificationMethod(smsResult.getNotificationMethod());
        alert.setNotificationDelivered(smsResult.isDelivered());
        if (smsResult.isDelivered()) {
            alert.setAlertStatus(ImagingCriticalFindingAlert.AlertStatus.NOTIFIED);
        }
        log.info("Abnormal finding SMS result for alert {}: delivered={}, skipped={}, reason={}",
            alert.getAlertId(), smsResult.isDelivered(), smsResult.isSkipped(), smsResult.getReason());
    }
    
    /**
     * Send significant change notification
     */
    public void sendSignificantChangeNotification(ImagingCriticalFindingAlert alert, ImagingStudy priorStudy) {
        if (!notificationsEnabled) {
            log.debug("Notifications are disabled");
            return;
        }
        
        log.info("Sending significant change notification for alert: {}", alert.getAlertId());
        
        ImagingStudy study = alert.getStudy();
        
        String notificationMessage = String.format(
            "SIGNIFICANT CHANGE DETECTED\nPatient: %s\nCurrent Study: %s (%s)\nPrior Study: %s (%s)\nChange: %s",
            PatientDisplayName.of(study.getPatient()),
            study.getStudyName(),
            study.getStudyDate().toLocalDate(),
            priorStudy.getStudyName(),
            priorStudy.getStudyDate().toLocalDate(),
            alert.getAlertMessage()
        );
        
        log.info("Notification message: {}", notificationMessage);
        
        CommunicationSmsClient.SmsSendResult smsResult =
                communicationSmsClient.sendPatientNotification(study.getPatient(), notificationMessage);
        alert.setNotificationSentDate(LocalDateTime.now());
        alert.setNotificationMethod(smsResult.getNotificationMethod());
        alert.setNotificationDelivered(smsResult.isDelivered());
        if (smsResult.isDelivered()) {
            alert.setAlertStatus(ImagingCriticalFindingAlert.AlertStatus.NOTIFIED);
        }
        log.info("Significant change SMS result for alert {}: delivered={}, skipped={}, reason={}",
            alert.getAlertId(), smsResult.isDelivered(), smsResult.isSkipped(), smsResult.getReason());
    }
    
    /**
     * Send report availability notification
     */
    public void sendReportAvailabilityNotification(ImagingCriticalFindingAlert alert) {
        if (!notificationsEnabled) {
            log.debug("Notifications are disabled");
            return;
        }
        
        log.info("Sending report availability notification for alert: {}", alert.getAlertId());
        
        ImagingStudy study = alert.getStudy();
        
        String notificationMessage = String.format(
            "IMAGING REPORT AVAILABLE\nPatient: %s\nStudy: %s\nAccession: %s\nReport Date: %s",
            PatientDisplayName.of(study.getPatient()),
            study.getStudyName(),
            study.getAccessionNumber(),
            study.getReportDate() != null ? study.getReportDate().toLocalDate() : "N/A"
        );
        
        log.info("Notification message: {}", notificationMessage);
        
        CommunicationSmsClient.SmsSendResult smsResult =
                communicationSmsClient.sendPatientNotification(study.getPatient(), notificationMessage);
        alert.setNotificationSentDate(LocalDateTime.now());
        alert.setNotificationMethod(smsResult.getNotificationMethod());
        alert.setNotificationDelivered(smsResult.isDelivered());
        if (smsResult.isDelivered()) {
            alert.setAlertStatus(ImagingCriticalFindingAlert.AlertStatus.NOTIFIED);
        }
        log.info("Report availability SMS result for alert {}: delivered={}, skipped={}, reason={}",
            alert.getAlertId(), smsResult.isDelivered(), smsResult.isSkipped(), smsResult.getReason());
    }
}
