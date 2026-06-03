package com.easyops.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String url;
    private String[] events;
    private Boolean isActive;
    private Integer retryCount;
    private Integer timeoutSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

