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
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "imaging_orders", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_id")
    private UUID orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Order Information
    @Column(name = "order_number", unique = true, nullable = false, length = 100)
    private String orderNumber;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "ordering_provider_id", nullable = false)
    private UUID orderingProviderId;
    
    @Column(name = "ordering_provider_name", length = 200)
    private String orderingProviderName;
    
    @Column(name = "ordering_facility_id")
    private UUID orderingFacilityId;
    
    @Column(name = "ordering_facility_name", length = 200)
    private String orderingFacilityName;
    
    // Study Information
    @Column(name = "study_type", nullable = false, length = 100)
    private String studyType;
    
    @Column(name = "study_modality", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StudyModality studyModality;
    
    @Column(name = "study_description", nullable = false, length = 500)
    private String studyDescription;
    
    @Column(name = "cpt_code", nullable = false, length = 20)
    private String cptCode;
    
    @Column(name = "body_part", nullable = false, length = 200)
    private String bodyPart;
    
    @Column(name = "laterality", length = 50)
    private String laterality;
    
    @Column(name = "specific_anatomical_site", length = 200)
    private String specificAnatomicalSite;
    
    @Column(name = "view_projection", length = 100)
    private String viewProjection;
    
    // Clinical Information
    @Column(name = "clinical_indication", nullable = false, columnDefinition = "TEXT")
    private String clinicalIndication;
    
    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderPriority priority = OrderPriority.ROUTINE;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
    @Column(name = "contrast_required")
    private Boolean contrastRequired = false;
    
    @Column(name = "contrast_type", length = 100)
    private String contrastType;
    
    @Column(name = "patient_preparation_required")
    private Boolean patientPreparationRequired = false;
    
    @Column(name = "patient_preparation_instructions", columnDefinition = "TEXT")
    private String patientPreparationInstructions;
    
    @Column(name = "sedation_required")
    private Boolean sedationRequired = false;
    
    // Order Status
    @Column(name = "order_status", length = 50)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    @Column(name = "sent_date")
    private LocalDateTime sentDate;
    
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;
    
    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;
    
    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "no_show")
    private Boolean noShow = false;
    
    // Transmission Information
    @Column(name = "transmission_method", length = 50)
    private String transmissionMethod;
    
    @Column(name = "transmission_status", length = 50)
    private String transmissionStatus;
    
    @Column(name = "transmission_date")
    private LocalDateTime transmissionDate;
    
    @Column(name = "radiology_facility_id")
    private UUID radiologyFacilityId;
    
    @Column(name = "radiology_facility_name", length = 200)
    private String radiologyFacilityName;
    
    @Column(name = "order_confirmation_received")
    private Boolean orderConfirmationReceived = false;
    
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
    
    public enum StudyModality {
        XRAY, CT, MRI, ULTRASOUND, MAMMOGRAPHY, NUCLEAR_MEDICINE, PET, DEXA, FLUOROSCOPY, OTHER
    }
    
    public enum OrderPriority {
        ROUTINE, STAT, URGENT
    }
    
    public enum OrderStatus {
        PENDING, SENT, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    }
}
