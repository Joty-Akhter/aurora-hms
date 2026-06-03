package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "performance_cycles", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "cycle_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PerformanceCycle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cycle_id")
    private UUID cycleId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "cycle_name", nullable = false, length = 200)
    private String cycleName;
    
    @Column(name = "cycle_type", nullable = false, length = 50)
    private String cycleType;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "review_start_date")
    private LocalDate reviewStartDate;
    
    @Column(name = "review_end_date")
    private LocalDate reviewEndDate;
    
    @Column(name = "status", length = 50)
    private String status = "planned";
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
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

