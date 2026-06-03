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
 * Entity for linking medications to clinical notes
 */
@Entity
@Table(name = "clinical_note_medications", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ClinicalNoteMedication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "link_id")
    private UUID linkId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private ClinicalNote note;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Link Information
    @Column(name = "link_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LinkType linkType = LinkType.DOCUMENTED;
    
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
        DOCUMENTED, PRESCRIBED, DISCONTINUED, MODIFIED, MONITORED, OTHER
    }
    
    public enum LinkStrength {
        WEAK, MODERATE, STRONG
    }
}
