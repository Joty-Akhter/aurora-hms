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
@Table(name = "attendance_records", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "attendance_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AttendanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "attendance_id")
    private UUID attendanceId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;
    
    @Column(name = "clock_in_time")
    private LocalDateTime clockInTime;
    
    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;
    
    @Column(name = "break_start_time")
    private LocalDateTime breakStartTime;
    
    @Column(name = "break_end_time")
    private LocalDateTime breakEndTime;
    
    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;
    
    @Column(name = "regular_hours", precision = 5, scale = 2)
    private BigDecimal regularHours;
    
    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours;
    
    @Column(name = "status", length = 50)
    private String status = "present";
    
    @Column(name = "work_location", length = 100)
    private String workLocation;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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

