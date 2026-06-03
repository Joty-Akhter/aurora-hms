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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "performance_reviews", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PerformanceReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "review_id")
    private UUID reviewId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "cycle_id")
    private UUID cycleId;
    
    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;
    
    @Column(name = "review_type", nullable = false, length = 50)
    private String reviewType;
    
    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;
    
    @Column(name = "review_period_start", nullable = false)
    private LocalDate reviewPeriodStart;
    
    @Column(name = "review_period_end", nullable = false)
    private LocalDate reviewPeriodEnd;
    
    @Column(name = "overall_rating", precision = 3, scale = 2)
    private BigDecimal overallRating;
    
    @Column(name = "status", length = 50)
    private String status = "draft";
    
    @Column(name = "self_review_completed")
    private Boolean selfReviewCompleted = false;
    
    @Column(name = "manager_review_completed")
    private Boolean managerReviewCompleted = false;
    
    @Column(name = "hr_review_completed")
    private Boolean hrReviewCompleted = false;
    
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;
    
    @Column(name = "areas_for_improvement", columnDefinition = "TEXT")
    private String areasForImprovement;
    
    @Column(name = "achievements", columnDefinition = "TEXT")
    private String achievements;
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "recommended_action", length = 100)
    private String recommendedAction;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
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

