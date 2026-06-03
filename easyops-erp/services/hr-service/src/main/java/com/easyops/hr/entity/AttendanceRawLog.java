package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * HR-AT-03: Raw punch record from a biometric / access-control device.
 * Punches are ingested here first, then processed into {@link AttendanceRecord} rows.
 */
@Entity
@Table(name = "attendance_raw_logs", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AttendanceRawLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "raw_log_id")
    private UUID rawLogId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** Resolved employee UUID. May be null if device sent only rawEmployeeCode and mapping failed. */
    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "punch_time", nullable = false)
    private LocalDateTime punchTime;

    /** IN | OUT | UNKNOWN */
    @Column(name = "punch_type", length = 10)
    private String punchType = "UNKNOWN";

    /** DEVICE | API | IMPORT */
    @Column(name = "source", length = 50)
    private String source = "DEVICE";

    /** Device-side employee code used when device does not send UUID. */
    @Column(name = "raw_employee_code", length = 100)
    private String rawEmployeeCode;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** FK to the attendance_record created/updated during processing. */
    @Column(name = "attendance_record_id")
    private UUID attendanceRecordId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
