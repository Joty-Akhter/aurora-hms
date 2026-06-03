package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteTemplateResponse {

    /** API-only enum (do not reference JPA entities here — avoids coupling and circular type graphs). */
    public enum TemplateType {
        SOAP, PROGRESS, CONSULTATION, DISCHARGE, PROCEDURE, ADMISSION, OPERATIVE, OTHER
    }
    
    private UUID templateId;
    
    // Template Identification
    private String templateName;
    private TemplateType templateType;
    private String specialty;
    private UUID departmentId;
    
    // Template Content
    private String templateContent; // JSON string
    
    // Template Metadata
    private String description;
    private Boolean isSystemTemplate;
    private Boolean isActive;
    private Boolean isPublic;
    
    // Ownership
    private UUID createdBy;
    private LocalDateTime createdDate;
    
    // Usage Tracking
    private Integer usageCount;
    private LocalDateTime lastUsedDate;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
}
