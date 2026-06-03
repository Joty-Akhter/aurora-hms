package com.easyops.hr.entity;

import jakarta.persistence.*;
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
@Table(name = "payroll_runs", schema = "hr")
@EntityListeners(AuditingEntityListener.class)
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "payroll_run_id")
    private UUID payrollRunId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "run_name", nullable = false, length = 200)
    private String runName;

    @Column(name = "pay_period_start", nullable = false)
    private LocalDate payPeriodStart;

    @Column(name = "pay_period_end", nullable = false)
    private LocalDate payPeriodEnd;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "status", length = 50)
    private String status = "draft";

    @Column(name = "total_gross_pay", precision = 15, scale = 2)
    private BigDecimal totalGrossPay;

    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "total_net_pay", precision = 15, scale = 2)
    private BigDecimal totalNetPay;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "payrollRunId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollDetail> payrollDetails = new ArrayList<>();

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

    public PayrollRun() {
        // JPA requirement
    }

    public UUID getPayrollRunId() {
        return payrollRunId;
    }

    public void setPayrollRunId(UUID payrollRunId) {
        this.payrollRunId = payrollRunId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public LocalDate getPayPeriodStart() {
        return payPeriodStart;
    }

    public void setPayPeriodStart(LocalDate payPeriodStart) {
        this.payPeriodStart = payPeriodStart;
    }

    public LocalDate getPayPeriodEnd() {
        return payPeriodEnd;
    }

    public void setPayPeriodEnd(LocalDate payPeriodEnd) {
        this.payPeriodEnd = payPeriodEnd;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalGrossPay() {
        return totalGrossPay;
    }

    public void setTotalGrossPay(BigDecimal totalGrossPay) {
        this.totalGrossPay = totalGrossPay;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getTotalNetPay() {
        return totalNetPay;
    }

    public void setTotalNetPay(BigDecimal totalNetPay) {
        this.totalNetPay = totalNetPay;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public UUID getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(UUID processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PayrollDetail> getPayrollDetails() {
        return payrollDetails;
    }

    public void setPayrollDetails(List<PayrollDetail> payrollDetails) {
        this.payrollDetails = payrollDetails;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

