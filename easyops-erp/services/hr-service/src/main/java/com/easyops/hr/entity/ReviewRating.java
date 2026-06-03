package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_ratings", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ReviewRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rating_id")
    private UUID ratingId;
    
    @Column(name = "review_id", nullable = false)
    private UUID reviewId;
    
    @Column(name = "competency_id")
    private UUID competencyId;
    
    @Column(name = "rating_area", nullable = false, length = 200)
    private String ratingArea;
    
    @Column(name = "rating_value", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingValue;
    
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ZERO;
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

