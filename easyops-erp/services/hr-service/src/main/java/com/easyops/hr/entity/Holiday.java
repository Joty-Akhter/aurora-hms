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
@Table(name = "holidays", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Holiday {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "holiday_id")
    private UUID holidayId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "holiday_name", nullable = false, length = 200)
    private String holidayName;
    
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
    
    @Column(name = "holiday_type", length = 50)
    private String holidayType = "public";
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** Optional payroll scope: department-wide holiday when set (employee_id null). */
    @Column(name = "department_id")
    private UUID departmentId;

    /** Optional payroll scope: employee-specific holiday when set. */
    @Column(name = "employee_id")
    private UUID employeeId;

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
