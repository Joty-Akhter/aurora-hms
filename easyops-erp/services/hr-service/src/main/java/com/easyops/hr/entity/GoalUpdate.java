package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_updates", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GoalUpdate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "update_id")
    private UUID updateId;
    
    @Column(name = "goal_id", nullable = false)
    private UUID goalId;
    
    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;
    
    @Column(name = "current_value", precision = 10, scale = 2)
    private BigDecimal currentValue;
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage;
    
    @Column(name = "update_notes", columnDefinition = "TEXT")
    private String updateNotes;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

