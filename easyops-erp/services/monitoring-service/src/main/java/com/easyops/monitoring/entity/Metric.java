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
@Table(name = "metrics", schema = "system")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;
    
    @Column(name = "metric_value")
    private Double metricValue;
    
    @Column(name = "metric_type", length = 50)
    private String metricType; // COUNTER, GAUGE, HISTOGRAM, TIMER
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags;
    
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}

