package com.easyops.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class NotificationRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID organizationId;
    
    private String type = "INFO"; // INFO, WARNING, ERROR, SUCCESS
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String actionUrl;
    private String actionLabel;
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL
    private LocalDateTime expiresAt;
    private Map<String, Object> metadata;
}

