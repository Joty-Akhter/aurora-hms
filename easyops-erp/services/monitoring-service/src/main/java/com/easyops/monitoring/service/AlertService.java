package com.easyops.monitoring.service;

import com.easyops.monitoring.dto.AlertResponse;
import com.easyops.monitoring.entity.Alert;
import com.easyops.monitoring.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {
    
    private final AlertRepository alertRepository;
    
    public List<AlertResponse> getActiveAlerts() {
        return alertRepository.findActiveAlerts()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public Page<AlertResponse> getAlertsByService(String serviceName, Pageable pageable) {
        return alertRepository.findByServiceNameOrderByCreatedAtDesc(serviceName, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional
    public void acknowledgeAlert(UUID alertId, UUID acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());
        
        alertRepository.save(alert);
        log.info("Alert acknowledged: {} by user: {}", alertId, acknowledgedBy);
    }
    
    @Transactional
    public void resolveAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        
        alertRepository.save(alert);
        log.info("Alert resolved: {}", alertId);
    }
    
    private AlertResponse mapToResponse(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getAlertName(),
            alert.getAlertType(),
            alert.getServiceName(),
            alert.getMessage(),
            alert.getDetails(),
            alert.getStatus(),
            alert.getCreatedAt(),
            alert.getAcknowledgedAt(),
            alert.getResolvedAt()
        );
    }
}

