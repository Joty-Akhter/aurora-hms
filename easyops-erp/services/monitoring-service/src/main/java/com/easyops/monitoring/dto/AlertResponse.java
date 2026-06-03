package com.easyops.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private UUID id;
    private String alertName;
    private String alertType;
    private String serviceName;
    private String message;
    private Map<String, Object> details;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
}

