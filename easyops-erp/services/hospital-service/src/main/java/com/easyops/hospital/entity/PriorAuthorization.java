package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prior_authorizations", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PriorAuthorization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "prior_auth_id")
    private UUID priorAuthId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formulary_check_id")
    private FormularyCheck formularyCheck;
    
    @Column(name = "insurance_id")
    private UUID insuranceId;
    
    @Column(name = "insurance_company_name", length = 200)
    private String insuranceCompanyName;
    
    @Column(name = "policy_number", length = 100)
    private String policyNumber;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_name", length = 500)
    private String medicationName;
    
    @Column(name = "prior_auth_number", length = 100)
    private String priorAuthNumber; // Authorization number from insurance
    
    @Column(name = "request_date")
    @Builder.Default
    private LocalDate requestDate = LocalDate.now();
    
    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PriorAuthStatus status = PriorAuthStatus.PENDING;
    
    @Column(name = "submitted_date")
    private LocalDate submittedDate;
    
    @Column(name = "approved_date")
    private LocalDate approvedDate;
    
    @Column(name = "denied_date")
    private LocalDate deniedDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;
    
    @Column(name = "clinical_justification", columnDefinition = "TEXT")
    private String clinicalJustification;
    
    @Column(name = "supporting_documentation", columnDefinition = "TEXT")
    private String supportingDocumentation; // References to documents
    
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    @Column(name = "pbm_name", length = 200)
    private String pbmName;
    
    @Column(name = "pbm_request_id", length = 100)
    private String pbmRequestId;
    
    @Column(name = "pbm_response_id", length = 100)
    private String pbmResponseId;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData; // JSON response from PBM
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum PriorAuthStatus {
        PENDING, SUBMITTED, APPROVED, DENIED, EXPIRED, CANCELLED
    }
}
