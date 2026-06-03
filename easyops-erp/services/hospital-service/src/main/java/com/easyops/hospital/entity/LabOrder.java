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
@Table(name = "lab_orders", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LabOrder {
    
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
    
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;
    
    @Column(name = "ordering_provider_id", nullable = false)
    private UUID orderingProviderId;
    
    @Column(name = "ordering_provider_name", length = 200)
    private String orderingProviderName;
    
    @Column(name = "ordering_facility_id")
    private UUID orderingFacilityId;
    
    @Column(name = "ordering_facility_name", length = 200)
    private String orderingFacilityName;
    
    // Test Information
    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;
    
    @Column(name = "loinc_code", length = 20)
    private String loincCode;
    
    @Column(name = "test_category", length = 100)
    private String testCategory;
    
    @Column(name = "test_type", length = 100)
    private String testType;
    
    @Column(name = "is_test_panel")
    private Boolean isTestPanel = false;
    
    @Column(name = "panel_name", length = 200)
    private String panelName;
    
    // Clinical Information
    @Column(name = "clinical_indication", columnDefinition = "TEXT")
    private String clinicalIndication;
    
    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderPriority priority = OrderPriority.ROUTINE;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
    @Column(name = "fasting_required")
    private Boolean fastingRequired = false;
    
    @Column(name = "patient_preparation_instructions", columnDefinition = "TEXT")
    private String patientPreparationInstructions;
    
    // Order Status
    @Column(name = "order_status", length = 50)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    @Column(name = "sent_date")
    private LocalDateTime sentDate;
    
    @Column(name = "collected_date")
    private LocalDateTime collectedDate;
    
    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    // Transmission Information
    @Column(name = "transmission_method", length = 50)
    private String transmissionMethod;
    
    @Column(name = "transmission_status", length = 50)
    private String transmissionStatus;
    
    @Column(name = "transmission_date")
    private LocalDateTime transmissionDate;
    
    @Column(name = "laboratory_id")
    private UUID laboratoryId;
    
    @Column(name = "laboratory_name", length = 200)
    private String laboratoryName;
    
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
    
    public enum OrderPriority {
        ROUTINE, STAT, ASAP, TIMED
    }
    
    public enum OrderStatus {
        PENDING, SENT, COLLECTED, IN_PROCESS, COMPLETED, CANCELLED
    }
}
