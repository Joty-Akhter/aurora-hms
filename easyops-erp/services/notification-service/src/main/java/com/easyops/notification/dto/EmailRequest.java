package com.easyops.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class EmailRequest {
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email address")
    private String toEmail;
    
    private String fromEmail;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String body; // For direct email (not template-based)
    
    private String templateName; // For template-based email
    private Map<String, Object> templateVariables;
    
    private UUID organizationId;
    private UUID userId;
}

