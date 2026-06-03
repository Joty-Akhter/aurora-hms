package com.easyops.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_templates", schema = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;
    
    @Column(name = "subject", nullable = false)
    private String subject;
    
    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;
    
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;
    
    @Column(name = "variables", columnDefinition = "TEXT[]")
    private String[] variables;
    
    @Column(name = "locale", length = 10)
    private String locale = "en-US";
    
    @Column(name = "organization_id")
    private UUID organizationId; // NULL for global templates
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
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

