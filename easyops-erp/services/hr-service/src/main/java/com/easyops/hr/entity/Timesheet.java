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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "timesheets", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "week_start_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Timesheet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "timesheet_id")
    private UUID timesheetId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;
    
    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;
    
    @Column(name = "total_hours", precision = 6, scale = 2)
    private BigDecimal totalHours;
    
    @Column(name = "regular_hours", precision = 6, scale = 2)
    private BigDecimal regularHours;
    
    @Column(name = "overtime_hours", precision = 6, scale = 2)
    private BigDecimal overtimeHours;
    
    @Column(name = "status", length = 50)
    private String status = "draft";
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @OneToMany(mappedBy = "timesheetId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetLine> lines = new ArrayList<>();
    
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

