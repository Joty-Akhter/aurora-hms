package com.easyops.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {
    private Long totalServices;
    private Long servicesUp;
    private Long servicesDown;
    private Long activeAlerts;
    private Long criticalAlerts;
    private Double avgResponseTime;
}

