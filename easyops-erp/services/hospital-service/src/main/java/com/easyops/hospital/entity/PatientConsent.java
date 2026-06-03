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
@Table(name = "patient_consents", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientConsent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "consent_id")
    private UUID consentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "consent_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConsentType consentType;
    
    @Column(name = "consent_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConsentStatus consentStatus;
    
    @Column(name = "consent_date", nullable = false)
    private LocalDate consentDate;
    
    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;
    
    @Column(name = "expires_date")
    private LocalDate expiresDate;
    
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
    
    public enum ConsentType {
        HIPAA, FINANCIAL, MARKETING, TREATMENT
    }
    
    public enum ConsentStatus {
        GRANTED, DENIED, REVOKED
    }
}
