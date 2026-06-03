package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imaging_critical_finding_alerts", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingCriticalFindingAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_id")
    private UUID alertId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private ImagingStudy study;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    // Alert Information
    @Column(name = "alert_status", length = 50)
    @Enumerated(EnumType.STRING)
    private AlertStatus alertStatus = AlertStatus.PENDING;
    
    @Column(name = "alert_priority", length = 50)
    @Enumerated(EnumType.STRING)
    private AlertPriority alertPriority = AlertPriority.HIGH;
    
    @Column(name = "alert_message", nullable = false, columnDefinition = "TEXT")
    private String alertMessage;
    
    @Column(name = "finding_keywords", columnDefinition = "TEXT")
    private String findingKeywords;
    
    // Notification Information
    @Column(name = "notified_provider_id")
    private UUID notifiedProviderId;
    
    @Column(name = "notified_provider_name", length = 200)
    private String notifiedProviderName;
    
    @Column(name = "notification_method", length = 50)
    private String notificationMethod;
    
    @Column(name = "notification_sent_date")
    private LocalDateTime notificationSentDate;
    
    @Column(name = "notification_delivered")
    private Boolean notificationDelivered = false;
    
    // Acknowledgment Information
    @Column(name = "is_acknowledged")
    private Boolean isAcknowledged = false;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;
    
    @Column(name = "acknowledgment_notes", columnDefinition = "TEXT")
    private String acknowledgmentNotes;
    
    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;
    
    // Escalation Information
    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;
    
    @Column(name = "escalated_to")
    private UUID escalatedTo;
    
    @Column(name = "escalation_date")
    private LocalDateTime escalationDate;
    
    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum AlertStatus {
        PENDING, NOTIFIED, ACKNOWLEDGED, ESCALATED, RESOLVED
    }
    
    public enum AlertPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
