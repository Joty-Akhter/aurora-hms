package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_insurance", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientInsurance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "insurance_id")
    private UUID insuranceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "insurance_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InsuranceType insuranceType;
    
    @Column(name = "insurance_company_name", length = 200)
    private String insuranceCompanyName;
    
    @Column(name = "policy_number", length = 100)
    private String policyNumber;
    
    @Column(name = "group_number", length = 100)
    private String groupNumber;
    
    @Column(name = "subscriber_name", length = 200)
    private String subscriberName;
    
    @Column(name = "subscriber_dob")
    private LocalDate subscriberDob;
    
    @Column(name = "subscriber_relationship", length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriberRelationship subscriberRelationship;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;
    
    @Column(name = "verification_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.Not_Verified;
    
    @Column(name = "verified_date")
    private LocalDate verifiedDate;
    
    @Column(name = "insurance_phone", length = 50)
    private String insurancePhone;
    
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
    
    public enum InsuranceType {
        PRIMARY, SECONDARY, TERTIARY
    }
    
    public enum SubscriberRelationship {
        Self, Spouse, Child, Other
    }
    
    public enum VerificationStatus {
        Verified, Pending, Not_Verified, Not_Applicable
    }
}
