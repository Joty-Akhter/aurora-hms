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
@Table(name = "payroll_details", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PayrollDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "payroll_detail_id")
    private UUID payrollDetailId;
    
    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;
    
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;
    
    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    
    @Column(name = "total_reimbursements", precision = 12, scale = 2)
    private BigDecimal totalReimbursements = BigDecimal.ZERO;
    
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;
    
    @Column(name = "working_days")
    private Integer workingDays;
    
    @Column(name = "present_days", precision = 5, scale = 2)
    private BigDecimal presentDays;
    
    @Column(name = "leave_days", precision = 4, scale = 1)
    private BigDecimal leaveDays;
    
    @Column(name = "overtime_hours", precision = 6, scale = 2)
    private BigDecimal overtimeHours;
    
    @Column(name = "overtime_amount", precision = 10, scale = 2)
    private BigDecimal overtimeAmount;

    /** Unpaid absence days (from attendance status); drives {@link #lopAmount}. */
    @Column(name = "lop_days", precision = 4, scale = 1)
    private BigDecimal lopDays;

    /** Loss-of-pay deduction applied in this run (pro-rated from basic salary). */
    @Column(name = "lop_amount", precision = 12, scale = 2)
    private BigDecimal lopAmount;

    /** HR-PY-03: Total approved bonus amount included in this payroll detail row. */
    @Column(name = "bonus_amount", precision = 12, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "status", length = 50)
    private String status = "pending";
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "bank_transfer";
    
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
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

