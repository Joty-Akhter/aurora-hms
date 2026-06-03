package com.easyops.monitoring.service;

import com.easyops.monitoring.dto.ServiceHealthResponse;
import com.easyops.monitoring.entity.Alert;
import com.easyops.monitoring.entity.ServiceHealth;
import com.easyops.monitoring.repository.AlertRepository;
import com.easyops.monitoring.repository.ServiceHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthCheckService {
    
    private final ServiceHealthRepository healthRepository;
    private final AlertRepository alertRepository;
    private final WebClient.Builder webClientBuilder;
    
    private static final Map<String, String> SERVICES = Map.of(
        "eureka", "http://eureka:8761/actuator/health",
        "api-gateway", "http://api-gateway:8081/actuator/health",
        "user-management", "http://user-management:8082/actuator/health",
        "auth-service", "http://auth-service:8083/actuator/health",
        "rbac-service", "http://rbac-service:8084/actuator/health",
        "organization-service", "http://organization-service:8085/actuator/health",
        "notification-service", "http://notification-service:8086/actuator/health"
    );
    
    @Scheduled(fixedDelayString = "${monitoring.health-check.interval-seconds:30}000")
    @Transactional
    public void performHealthChecks() {
        log.info("Starting health checks for all services");
        
        for (Map.Entry<String, String> serviceEntry : SERVICES.entrySet()) {
            checkServiceHealth(serviceEntry.getKey(), serviceEntry.getValue());
        }
    }
    
    private void checkServiceHealth(String serviceName, String healthUrl) {
        long startTime = System.currentTimeMillis();
        
        try {
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            updateServiceHealth(serviceName, "UP", (int) responseTime, null, null);
            resolveServiceDownAlert(serviceName);
            
            log.debug("Service {} is UP ({}ms)", serviceName, responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            updateServiceHealth(serviceName, "DOWN", (int) responseTime, e.getMessage(), null);
            createServiceDownAlert(serviceName, e.getMessage());
            
            log.error("Service {} is DOWN: {}", serviceName, e.getMessage());
        }
    }
    
    private void updateServiceHealth(String serviceName, String status, Integer responseTime, String error, Map<String, Object> metadata) {
        ServiceHealth health = healthRepository.findByServiceName(serviceName)
            .orElse(new ServiceHealth());
        
        health.setServiceName(serviceName);
        health.setStatus(status);
        health.setResponseTimeMs(responseTime);
        health.setErrorMessage(error);
        health.setMetadata(metadata);
        health.setLastCheckAt(LocalDateTime.now());
        
        healthRepository.save(health);
    }
    
    private void createServiceDownAlert(String serviceName, String errorMessage) {
        // Check if alert already exists
        List<Alert> existingAlerts = alertRepository.findByStatus("ACTIVE")
            .stream()
            .filter(a -> a.getServiceName().equals(serviceName) && a.getAlertName().equals("Service Down"))
            .toList();
        
        if (existingAlerts.isEmpty()) {
            Alert alert = new Alert();
            alert.setAlertName("Service Down");
            alert.setAlertType("CRITICAL");
            alert.setServiceName(serviceName);
            alert.setMessage("Service " + serviceName + " is down");
            alert.setDetails(Map.of("error", errorMessage));
            alert.setStatus("ACTIVE");
            
            alertRepository.save(alert);
            log.warn("Created critical alert for service down: {}", serviceName);
        }
    }
    
    private void resolveServiceDownAlert(String serviceName) {
        List<Alert> activeAlerts = alertRepository.findByStatus("ACTIVE")
            .stream()
            .filter(a -> a.getServiceName().equals(serviceName) && a.getAlertName().equals("Service Down"))
            .toList();
        
        for (Alert alert : activeAlerts) {
            alert.setStatus("RESOLVED");
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info("Resolved service down alert for: {}", serviceName);
        }
    }
    
    public List<ServiceHealthResponse> getAllServicesHealth() {
        return healthRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public ServiceHealthResponse getServiceHealth(String serviceName) {
        ServiceHealth health = healthRepository.findByServiceName(serviceName)
            .orElseThrow(() -> new RuntimeException("Service health not found: " + serviceName));
        return mapToResponse(health);
    }
    
    private ServiceHealthResponse mapToResponse(ServiceHealth health) {
        return new ServiceHealthResponse(
            health.getServiceName(),
            health.getStatus(),
            health.getLastCheckAt(),
            health.getResponseTimeMs(),
            health.getErrorMessage(),
            health.getMetadata()
        );
    }
}

