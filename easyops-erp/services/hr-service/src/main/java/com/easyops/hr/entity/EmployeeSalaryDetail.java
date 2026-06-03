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
@Table(name = "employee_salary_details", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmployeeSalaryDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "salary_detail_id")
    private UUID salaryDetailId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "salary_structure_id")
    private UUID salaryStructureId;
    
    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    /** ES-07: AMOUNT = fixed amount; PERCENTAGE = override %; USE_MASTER_DEFAULT = use master default/formula. */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 30)
    private ComponentValueType valueType = ComponentValueType.AMOUNT;
    
    /** When valueType = AMOUNT, required; otherwise optional. */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;
    
    /** When valueType = PERCENTAGE, override percentage; base from component master. */
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;
    
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** ES-17: Reason for this revision (e.g. "Bulk 5% to Basic by grade"). */
    @Column(name = "revision_reason", length = 500)
    private String revisionReason;

    /** ES-17: Type of revision (e.g. ANNUAL_INCREMENT, BULK_PERCENTAGE). */
    @Column(name = "revision_type", length = 50)
    private String revisionType;
    
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

