package com.easyops.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthResponse {
    private String serviceName;
    private String status;
    private LocalDateTime lastCheckAt;
    private Integer responseTimeMs;
    private String errorMessage;
    private Map<String, Object> metadata;
}

