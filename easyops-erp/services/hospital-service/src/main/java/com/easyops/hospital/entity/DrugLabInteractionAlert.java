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
@Table(name = "drug_lab_interaction_alerts", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DrugLabInteractionAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_id")
    private UUID alertId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private LabResult result;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Interaction Information
    @Column(name = "interaction_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;
    
    @Column(name = "interaction_severity", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InteractionSeverity interactionSeverity = InteractionSeverity.MODERATE;
    
    @Column(name = "interaction_description", nullable = false, columnDefinition = "TEXT")
    private String interactionDescription;
    
    @Column(name = "affected_test", length = 500)
    private String affectedTest;
    
    @Column(name = "affected_medication", length = 500)
    private String affectedMedication;
    
    // Clinical Impact
    @Column(name = "clinical_impact", columnDefinition = "TEXT")
    private String clinicalImpact;
    
    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;
    
    @Column(name = "monitoring_required")
    @Builder.Default
    private Boolean monitoringRequired = false;
    
    @Column(name = "monitoring_frequency", length = 100)
    private String monitoringFrequency;
    
    // Alert Status
    @Column(name = "alert_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus alertStatus = AlertStatus.ACTIVE;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;
    
    @Column(name = "acknowledgment_notes", columnDefinition = "TEXT")
    private String acknowledgmentNotes;
    
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    // Notification
    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;
    
    @Column(name = "notification_sent_date")
    private LocalDateTime notificationSentDate;
    
    @Column(name = "notified_provider_id")
    private UUID notifiedProviderId;
    
    @Column(name = "notified_provider_name", length = 200)
    private String notifiedProviderName;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum InteractionType {
        FALSE_POSITIVE, FALSE_NEGATIVE, ALTERED_RESULT, INTERFERENCE, OTHER
    }
    
    public enum InteractionSeverity {
        LOW, MODERATE, HIGH, CRITICAL
    }
    
    public enum AlertStatus {
        ACTIVE, ACKNOWLEDGED, RESOLVED, DISMISSED
    }
}
