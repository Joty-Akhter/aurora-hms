package com.easyops.notification.entity;

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
@Table(name = "email_logs", schema = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "to_email", nullable = false)
    private String toEmail;
    
    @Column(name = "from_email")
    private String fromEmail;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "template_name", length = 100)
    private String templateName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_variables", columnDefinition = "jsonb")
    private Map<String, Object> templateVariables;
    
    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING, SENT, FAILED, BOUNCED
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

