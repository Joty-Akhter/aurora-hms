package com.easyops.hospitalscheduling.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health endpoint for API Gateway and load balancers.
 * Full health details (including DB) are available at /actuator/health when the service is run directly.
 */
@RestController
@RequestMapping("/api/hospital-scheduling")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "hospital-scheduling-service"));
    }
}
