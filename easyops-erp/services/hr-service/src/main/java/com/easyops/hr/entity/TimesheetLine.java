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
@Table(name = "timesheet_lines", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TimesheetLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "line_id")
    private UUID lineId;
    
    @Column(name = "timesheet_id", nullable = false)
    private UUID timesheetId;
    
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;
    
    @Column(name = "hours_worked", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursWorked;
    
    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    
    @Column(name = "billable")
    private Boolean billable = false;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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

