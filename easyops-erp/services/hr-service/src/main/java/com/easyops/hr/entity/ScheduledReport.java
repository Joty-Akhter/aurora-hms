package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_reports", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScheduledReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scheduled_report_id")
    private UUID scheduledReportId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "report_name", nullable = false, length = 200)
    private String reportName;
    
    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType;
    
    @Column(name = "report_config", columnDefinition = "JSONB")
    private String reportConfig; // JSON structure for report configuration
    
    @Column(name = "schedule_frequency", nullable = false, length = 50)
    private String scheduleFrequency; // daily, weekly, monthly, quarterly, yearly
    
    @Column(name = "schedule_day")
    private Integer scheduleDay; // Day of week/month for scheduling
    
    @Column(name = "schedule_time", length = 10)
    private String scheduleTime; // Time in HH:mm format
    
    @Column(name = "recipients", columnDefinition = "JSONB")
    private String recipients; // JSON array of email addresses
    
    @Column(name = "format", length = 50)
    @Builder.Default
    private String format = "json"; // json, pdf, excel, csv
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "last_run_date")
    private LocalDateTime lastRunDate;
    
    @Column(name = "next_run_date")
    private LocalDate nextRunDate;
    
    @Column(name = "run_count")
    @Builder.Default
    private Integer runCount = 0;
    
    @Column(name = "last_run_status", length = 50)
    private String lastRunStatus; // success, failed, skipped
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

