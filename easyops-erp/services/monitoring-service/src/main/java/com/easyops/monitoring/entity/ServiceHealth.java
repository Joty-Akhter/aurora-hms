package com.easyops.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "service_health", schema = "system")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status; // UP, DOWN, DEGRADED, UNKNOWN
    
    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt = LocalDateTime.now();
    
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastCheckAt = LocalDateTime.now();
    }
}

