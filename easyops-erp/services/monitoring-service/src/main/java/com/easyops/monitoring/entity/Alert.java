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
@Table(name = "alerts", schema = "system")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "alert_name", nullable = false)
    private String alertName;
    
    @Column(name = "alert_type", length = 50)
    private String alertType; // CRITICAL, WARNING, INFO
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;
    
    @Column(name = "status", length = 50)
    private String status = "ACTIVE"; // ACTIVE, ACKNOWLEDGED, RESOLVED
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

