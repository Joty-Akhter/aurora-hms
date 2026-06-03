package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback_360", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Feedback360 {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "feedback_id")
    private UUID feedbackId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "cycle_id")
    private UUID cycleId;
    
    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;
    
    @Column(name = "reviewer_relationship", nullable = false, length = 50)
    private String reviewerRelationship;
    
    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;
    
    @Column(name = "overall_rating", precision = 3, scale = 2)
    private BigDecimal overallRating;
    
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;
    
    @Column(name = "areas_for_improvement", columnDefinition = "TEXT")
    private String areasForImprovement;
    
    @Column(name = "specific_feedback", columnDefinition = "TEXT")
    private String specificFeedback;
    
    @Column(name = "would_recommend_collaboration")
    private Boolean wouldRecommendCollaboration;
    
    @Column(name = "status", length = 50)
    private String status = "draft";
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

