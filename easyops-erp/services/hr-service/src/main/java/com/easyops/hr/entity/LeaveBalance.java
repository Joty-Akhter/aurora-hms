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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_balances", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LeaveBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "balance_id")
    private UUID balanceId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "allocated_days", precision = 5, scale = 1)
    private BigDecimal allocatedDays = BigDecimal.ZERO;
    
    @Column(name = "used_days", precision = 5, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;
    
    @Column(name = "carried_forward_days", precision = 5, scale = 1)
    private BigDecimal carriedForwardDays = BigDecimal.ZERO;
    
    // balance_days is a computed column in the database, so we don't map it here
    // but we can add a transient field for convenience
    @Transient
    private BigDecimal balanceDays;
    
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
    
    // Utility method to calculate balance
    public BigDecimal getBalanceDays() {
        return allocatedDays.add(carriedForwardDays).subtract(usedDays);
    }
}

