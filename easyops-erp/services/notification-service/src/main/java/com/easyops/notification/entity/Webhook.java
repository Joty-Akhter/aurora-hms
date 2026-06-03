package com.easyops.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhooks", schema = "integration")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "url", nullable = false, length = 500)
    private String url;
    
    @Column(name = "secret")
    private String secret; // For HMAC signature
    
    @Column(name = "events", columnDefinition = "TEXT[]")
    private String[] events;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "retry_count")
    private Integer retryCount = 3;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

