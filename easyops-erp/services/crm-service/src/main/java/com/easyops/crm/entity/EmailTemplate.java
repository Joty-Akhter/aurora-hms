package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_templates", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "template_id")
    private UUID templateId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;
    
    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;
    
    @Column(name = "subject", nullable = false, length = 500)
    private String subject;
    
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;
    
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;
    
    @Column(name = "template_type", length = 50)
    private String templateType;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "placeholders", columnDefinition = "TEXT[]")
    private String[] placeholders;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

