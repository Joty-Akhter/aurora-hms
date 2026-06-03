package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_types", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "type_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LeaveType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "leave_type_id")
    private UUID leaveTypeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_paid")
    private Boolean isPaid = true;
    
    @Column(name = "max_days_per_year")
    private Integer maxDaysPerYear;
    
    @Column(name = "requires_approval")
    private Boolean requiresApproval = true;
    
    @Column(name = "carry_forward")
    private Boolean carryForward = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
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

