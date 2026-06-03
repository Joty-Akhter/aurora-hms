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

/**
 * Entity for linking imaging studies to medications (e.g., contrast agents)
 */
@Entity
@Table(name = "imaging_study_medications", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingStudyMedication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "link_id")
    private UUID linkId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private ImagingStudy study;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Link Information
    @Column(name = "link_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LinkType linkType = LinkType.CONTRAST_AGENT;
    
    @Column(name = "link_strength", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LinkStrength linkStrength = LinkStrength.MODERATE;
    
    @Column(name = "clinical_relevance", columnDefinition = "TEXT")
    private String clinicalRelevance;
    
    @Column(name = "linked_by", nullable = false)
    private UUID linkedBy;
    
    @Column(name = "linked_date")
    @Builder.Default
    private LocalDateTime linkedDate = LocalDateTime.now();
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum LinkType {
        CONTRAST_AGENT, PRE_MEDICATION, POST_MEDICATION, RELATED, OTHER
    }
    
    public enum LinkStrength {
        WEAK, MODERATE, STRONG
    }
}
