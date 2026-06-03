package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "note_templates", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class NoteTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "template_id")
    private UUID templateId;
    
    // Template Identification
    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;
    
    @Column(name = "template_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TemplateType templateType;
    
    @Column(name = "specialty", length = 100)
    private String specialty;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    // Template Content (stored as JSONB — must use SqlTypes.JSON or PostgreSQL rejects varchar binds)
    @Column(name = "template_content", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String templateContent;
    
    // Template Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_system_template")
    @Builder.Default
    private Boolean isSystemTemplate = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    // Ownership
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_date")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
    
    // Usage Tracking
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;
    
    @Column(name = "last_used_date")
    private LocalDateTime lastUsedDate;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum TemplateType {
        SOAP, PROGRESS, CONSULTATION, DISCHARGE, PROCEDURE, ADMISSION, OPERATIVE, OTHER
    }
}
