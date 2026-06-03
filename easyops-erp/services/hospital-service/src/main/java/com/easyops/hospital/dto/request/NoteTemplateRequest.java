package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.NoteTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteTemplateRequest {
    
    @NotBlank(message = "Template name is required")
    private String templateName;
    
    @NotNull(message = "Template type is required")
    private NoteTemplate.TemplateType templateType;
    
    private String specialty;
    
    private UUID departmentId;
    
    private String templateContent; // JSON string
    
    private String description;
    
    @Builder.Default
    private Boolean isPublic = false;
}
