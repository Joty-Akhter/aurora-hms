package com.easyops.monitoring.service;

import com.easyops.monitoring.dto.SystemMetrics;
import com.easyops.monitoring.entity.ServiceHealth;
import com.easyops.monitoring.repository.AlertRepository;
import com.easyops.monitoring.repository.MetricRepository;
import com.easyops.monitoring.repository.ServiceHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {
    
    private final ServiceHealthRepository healthRepository;
    private final AlertRepository alertRepository;
    private final MetricRepository metricRepository;
    
    public SystemMetrics getSystemMetrics() {
        Long totalServices = healthRepository.count();
        Long servicesUp = healthRepository.countServicesUp();
        Long servicesDown = healthRepository.countServicesDown();
        Long activeAlerts = (long) alertRepository.findActiveAlerts().size();
        Long criticalAlerts = alertRepository.countCriticalAlerts();
        
        // Calculate average response time
        List<ServiceHealth> allServices = healthRepository.findAll();
        Double avgResponseTime = allServices.stream()
            .filter(s -> s.getResponseTimeMs() != null)
            .mapToInt(ServiceHealth::getResponseTimeMs)
            .average()
            .orElse(0.0);
        
        return new SystemMetrics(
            totalServices,
            servicesUp,
            servicesDown,
            activeAlerts,
            criticalAlerts,
            avgResponseTime
        );
    }
}

