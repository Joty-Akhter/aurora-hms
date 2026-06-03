package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_base", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "article_id")
    private UUID articleId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "article_number", nullable = false, length = 50)
    private String articleNumber;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "subcategory", length = 100)
    private String subcategory;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT";
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "is_featured")
    private Boolean isFeatured = false;
    
    @Column(name = "keywords", columnDefinition = "TEXT[]")
    private String[] keywords;
    
    @Column(name = "slug", length = 255)
    private String slug;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column(name = "helpful_count")
    private Integer helpfulCount = 0;
    
    @Column(name = "not_helpful_count")
    private Integer notHelpfulCount = 0;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;
    
    @Column(name = "last_reviewed_by")
    private UUID lastReviewedBy;
    
    @Column(name = "related_articles", columnDefinition = "UUID[]")
    private UUID[] relatedArticles;
    
    @Column(name = "author_id")
    private UUID authorId;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
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

