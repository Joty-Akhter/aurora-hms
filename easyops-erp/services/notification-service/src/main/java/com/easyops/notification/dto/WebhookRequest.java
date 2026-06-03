package com.easyops.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WebhookRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Webhook name is required")
    private String name;
    
    @NotBlank(message = "Webhook URL is required")
    private String url;
    
    private String secret;
    private String[] events;
    private Boolean isActive = true;
    private Integer retryCount = 3;
    private Integer timeoutSeconds = 30;
}

