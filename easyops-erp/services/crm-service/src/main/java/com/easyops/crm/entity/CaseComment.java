package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_comments", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "comment_id")
    private UUID commentId;
    
    @Column(name = "case_id", nullable = false)
    private UUID caseId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;
    
    @Column(name = "comment_type", length = 50)
    private String commentType;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "has_attachments")
    private Boolean hasAttachments = false;
    
    @Column(name = "attachment_count")
    private Integer attachmentCount = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

