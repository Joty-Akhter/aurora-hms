package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "snomed_codes", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnomedCode {
    
    @Id
    @Column(name = "code", length = 50)
    private String code;
    
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(name = "concept_id", length = 50)
    private String conceptId;
    
    @Column(name = "fully_specified_name", columnDefinition = "TEXT")
    private String fullySpecifiedName;
    
    @Column(name = "semantic_tag", length = 100)
    private String semanticTag;
    
    @Column(name = "is_valid")
    @Builder.Default
    private Boolean isValid = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
