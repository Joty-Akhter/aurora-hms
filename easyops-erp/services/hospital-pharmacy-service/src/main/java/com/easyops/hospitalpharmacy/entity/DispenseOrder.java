package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispense_orders", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DispenseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "patient_id")
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_location_id", nullable = false)
    private PharmacyLocation pharmacyLocation;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "context_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ContextType contextType;

    @Column(name = "department_id")
    private UUID departmentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** Set when charges were posted to hospital-billing (Phase P2). */
    @Column(name = "billing_posted_at")
    private OffsetDateTime billingPostedAt;

    /** Phase P3 WS-E — paper Rx reference when no EHR prescription id is linked. */
    @Column(name = "paper_prescription_ref", length = 500)
    private String paperPrescriptionRef;

    @Column(name = "prescription_image_attachment_id")
    private UUID prescriptionImageAttachmentId;

    /** NOT_REQUIRED | PENDING | VERIFIED | FAILED_SOFT */
    @Column(name = "external_validation_status", length = 32)
    @Enumerated(EnumType.STRING)
    private ExternalValidationStatus externalValidationStatus;

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public enum ContextType {
        PATIENT_PRESCRIPTION,
        WALK_IN,
        DEPARTMENT_ISSUE
    }
}

