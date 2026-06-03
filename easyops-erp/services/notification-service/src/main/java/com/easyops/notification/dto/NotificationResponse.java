package com.easyops.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private String type;
    private String title;
    private String message;
    private String actionUrl;
    private String actionLabel;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String priority;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
}

